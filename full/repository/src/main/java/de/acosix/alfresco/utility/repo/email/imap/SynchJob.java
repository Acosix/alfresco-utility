/*
 * Copyright 2016 - 2024 Acosix GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.acosix.alfresco.utility.repo.email.imap;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.net.SocketFactory;

import org.alfresco.encryption.AlfrescoKeyStore;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.batch.BatchProcessWorkProvider;
import org.alfresco.repo.batch.BatchProcessor;
import org.alfresco.repo.batch.BatchProcessor.BatchProcessWorker;
import org.alfresco.repo.lock.LockAcquisitionException;
import org.alfresco.repo.security.authentication.AlfrescoSSLSocketFactory;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.email.EmailDelivery;
import org.alfresco.service.cmr.email.EmailMessageException;
import org.alfresco.service.cmr.email.EmailService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.transaction.TransactionListenerAdapter;
import org.alfresco.util.transaction.TransactionSupportUtil;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.surf.util.I18NUtil;

import de.acosix.alfresco.utility.repo.email.server.ImprovedEmailMessage;
import de.acosix.alfresco.utility.repo.job.GenericJob;
import de.acosix.alfresco.utility.repo.job.JobUtilities;
import de.acosix.alfresco.utility.repo.job.JobUtilities.LockReleasedCheck;
import de.acosix.alfresco.utility.repo.job.JobUtilities.RefreshAwareOperationWithJobLock;
import de.acosix.alfresco.utility.repo.subetha3.email.imap.JavaMailClient;
import de.acosix.alfresco.utility.repo.subetha6.email.imap.JakartaMailClient;

/**
 * Instances of this job class handle the synchronisation of emails from an IMAP account to Alfresco.
 *
 * @author Axel Faust
 */
public class SynchJob implements GenericJob, BatchProcessWorkProvider<SynchWork>, BatchProcessWorker<SynchWork>
{

    private static final Logger LOGGER = LoggerFactory.getLogger(SynchJob.class);

    private EmailService emailService;

    private Client imapClient;

    private String configName;

    private Config imapConfig;

    private LockReleasedCheck lockReleasedCheck;

    private final ThreadLocal<Locale> originalLocale = new ThreadLocal<>();

    private Locale importLocale;

    private int previouslyEstimatedWorkSize = -1;

    private Iterator<String> folderIter;

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(final Object jobExecutionContext)
    {
        this.configName = JobUtilities.getJobDataValue(jobExecutionContext, "configName", String.class);
        final String localeStr = JobUtilities.getJobDataValue(jobExecutionContext, "locale", String.class, false);
        if (localeStr != null && !localeStr.trim().isEmpty())
        {
            this.importLocale = DefaultTypeConverter.INSTANCE.convert(Locale.class, localeStr);
        }

        final TransactionService transactionService = JobUtilities.getJobDataValue(jobExecutionContext, "transactionService",
                TransactionService.class);
        this.emailService = JobUtilities.getJobDataValue(jobExecutionContext, "emailService", EmailService.class);
        this.imapConfig = JobUtilities.getJobDataValue(jobExecutionContext, "imapConfig", Config.class);

        final AlfrescoKeyStore sslTruststore = JobUtilities.getJobDataValue(jobExecutionContext, "ssl.truststore", AlfrescoKeyStore.class);
        final String truststorePath = JobUtilities.getJobDataValue(jobExecutionContext, "truststorePath", String.class, false);
        final String truststoreType = JobUtilities.getJobDataValue(jobExecutionContext, "truststoreType", String.class, false);
        final String truststorePassphrase = JobUtilities.getJobDataValue(jobExecutionContext, "truststorePassphrase", String.class, false);

        SocketFactory socketFactory = null;
        if ("imaps".equalsIgnoreCase(this.imapConfig.getProtocol()) || this.imapConfig.isStartTlsEnabled()
                || this.imapConfig.isStartTlsRequired())
        {
            synchronized (AlfrescoSSLSocketFactory.class)
            {
                if (truststorePath != null && !truststorePath.isEmpty() && truststoreType != null && !truststoreType.isEmpty())
                {
                    final KeyStore trustStore = this.initTrustStore(truststorePath, truststoreType, truststorePassphrase);
                    AlfrescoSSLSocketFactory.initTrustedSSLSocketFactory(trustStore);
                }
                else
                {
                    try
                    {
                        final KeyStore trustStore = KeyStore.getInstance("JKS");
                        for (final String alias : sslTruststore.getKeyAliases())
                        {
                            final Key key = sslTruststore.getKey(alias);
                            trustStore.setKeyEntry(alias, key, null, null);
                        }
                        AlfrescoSSLSocketFactory.initTrustedSSLSocketFactory(trustStore);
                    }
                    catch (final KeyStoreException e)
                    {
                        throw new AlfrescoRuntimeException("Failed to initialise truststore", e);
                    }
                }
                socketFactory = AlfrescoSSLSocketFactory.getDefault();
            }
        }

        final String threadCountStr = JobUtilities.getJobDataValue(jobExecutionContext, "threadCount", String.class, false);
        final int threadCount = threadCountStr != null ? Math.max(1, Integer.parseInt(threadCountStr)) : 4;

        try
        {
            Class.forName("javax.mail.Message");
            this.imapClient = JavaMailClient.open(this.imapConfig, threadCount + 1, socketFactory);
        }
        catch (final ClassNotFoundException e)
        {
            this.imapClient = JakartaMailClient.open(this.imapConfig, threadCount + 1, socketFactory);
        }
        try
        {

            final String lockTTLStr = JobUtilities.getJobDataValue(jobExecutionContext, "lockTTL", String.class, false);
            final String retryWaitStr = JobUtilities.getJobDataValue(jobExecutionContext, "lockRetryWait", String.class, false);

            final long lockTTL = lockTTLStr != null ? Long.parseLong(lockTTLStr) : 30000;
            final long retryWait = retryWaitStr != null ? Long.parseLong(retryWaitStr) : 5000;

            final QName lockQName = QName.createQName(SynchJob.class.getName(), this.configName);
            final RefreshAwareOperationWithJobLock op = check -> {
                this.lockReleasedCheck = check;
                final String logIntervalStr = JobUtilities.getJobDataValue(jobExecutionContext, "logInterval", String.class, false);

                final int logInterval = logIntervalStr != null ? Integer.parseInt(logIntervalStr) : 100;

                final BatchProcessor<SynchWork> processor = new BatchProcessor<>(
                        String.format(Locale.ENGLISH, "%s(%s)", SynchJob.class.getSimpleName(), this.configName),
                        transactionService.getRetryingTransactionHelper(), this, threadCount, 1, null, LogFactory.getLog(SynchJob.class),
                        logInterval);
                processor.process(this, true);
            };

            try
            {
                JobUtilities.runWithJobLock(jobExecutionContext, lockQName, lockTTL, retryWait, 3, op);
            }
            catch (final LockAcquisitionException lae)
            {
                LOGGER.info("Job lock unavailable for {} - job potentially running on different server", this.configName);
            }
        }
        finally
        {
            try
            {
                this.imapClient.close();
            }
            catch (final IOException e)
            {
                LOGGER.warn("Error closing IMAP client", e);
            }
        }
    }

    /**
     * Get an estimate of the total number of objects that will be provided by this instance.
     * Instances can provide accurate answers on each call, but only if the answer can be
     * provided quickly and efficiently; usually it is enough to to cache the result after
     * providing an initial estimate.
     *
     * @return a total work size estimate
     */
    public long getTotalEstimatedWorkSizeLong()
    {
        if (this.previouslyEstimatedWorkSize == -1)
        {
            this.estimateTotalWorkSize();
        }
        return this.previouslyEstimatedWorkSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTotalEstimatedWorkSize()
    {
        if (this.previouslyEstimatedWorkSize == -1)
        {
            this.estimateTotalWorkSize();
        }
        return this.previouslyEstimatedWorkSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<SynchWork> getNextWork()
    {
        Collection<SynchWork> nextWork = Collections.emptyList();
        if (this.folderIter == null)
        {
            this.folderIter = this.imapConfig.getFolders().iterator();
        }

        if (this.folderIter.hasNext())
        {
            final String folder = this.folderIter.next();

            final String path = this.imapConfig.getPathByFolder().get(folder);
            final String fromOverride = this.imapConfig.getFromOverrideByFolder().getOrDefault(folder,
                    this.imapConfig.getDefaultFromOverride());
            final String toOverride = this.imapConfig.getToOverrideByFolder().getOrDefault(folder, this.imapConfig.getDefaultToOverride());
            final String moveProcessedPath = this.imapConfig.getMoveProcessedToPathByFolder().get(folder);
            final String moveRejectedPath = this.imapConfig.getMoveRejectedToPathByFolder().get(folder);

            final List<ImapEmailMessage> messages;

            final boolean filter = this.imapConfig.isProcessFilterByFlagEnabled();

            if (filter)
            {
                messages = this.imapClient.listMessages(path, this.imapConfig.getProcessFilterByFlagBits(),
                        this.imapConfig.getProcessFilterByUnsetFlagBits(), this.imapConfig.getProcessFilterByFlagName(),
                        this.imapConfig.getProcessFilterByUnsetFlagName());
            }
            else
            {
                messages = this.imapClient.listMessages(path);
            }

            nextWork = messages.stream().map(SynchWork::new).collect(Collectors.toList());
            nextWork.forEach(w -> {
                w.setFromOverride(fromOverride);
                w.setToOverride(toOverride);
                w.setMoveProcessedPath(moveProcessedPath);
                w.setMoveRejectedPath(moveRejectedPath);
            });

        }
        return nextWork;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIdentifier(final SynchWork entry)
    {
        final ImapEmailMessage imapMessage = entry.getEmailMessage();
        return String.format(Locale.ENGLISH, "%s@%s", imapMessage.getMessageId(), imapMessage.getFolderPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeProcess() throws Throwable
    {
        AuthenticationUtil.pushAuthentication();
        AuthenticationUtil.clearCurrentSecurityContext();
        this.originalLocale.set(I18NUtil.getLocaleOrNull());
        I18NUtil.setLocale(this.importLocale);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final SynchWork entry) throws Throwable
    {
        if (this.lockReleasedCheck.isLockReleased())
        {
            LOGGER.debug("Skipping {} as job lock for {} has already been released", this.getIdentifier(entry), this.configName);
        }
        else
        {
            final String fromOverride = entry.getFromOverride();
            final ImapEmailMessage emailMessage = entry.getEmailMessage();
            final EmailDelivery delivery = new EmailDelivery(entry.getToOverride(),
                    fromOverride != null ? fromOverride : emailMessage.getFrom(), null);
            final ImprovedEmailMessage message = this.imapClient.toImprovedEmailMessage(emailMessage);
            try
            {
                this.emailService.importMessage(delivery, message);

                TransactionSupportUtil.bindListener(new TransactionListenerAdapter()
                {

                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public void afterCommit()
                    {
                        try
                        {
                            if (SynchJob.this.imapConfig.isFlagProcessedEnabled())
                            {
                                SynchJob.this.imapClient.flagMessage(emailMessage, SynchJob.this.imapConfig.getFlagProcessedWithBits(),
                                        SynchJob.this.imapConfig.getFlagProcessedWithUnsetBits(),
                                        SynchJob.this.imapConfig.getFlagProcessedWithName(),
                                        SynchJob.this.imapConfig.getFlagProcessedWithUnsetName());
                            }
                            final String moveProcessedPath = entry.getMoveRejectedPath();
                            if (moveProcessedPath != null)
                            {
                                SynchJob.this.imapClient.moveMessage(emailMessage, moveProcessedPath);
                            }
                        }
                        catch (final EmailMessageException are)
                        {
                            LOGGER.warn("Failed to mark/move processed email", are);
                        }
                    }

                }, 0);
            }
            catch (final EmailMessageException eme)
            {
                RetryingTransactionHelper.getActiveUserTransaction().setRollbackOnly();

                try
                {
                    if (this.imapConfig.isFlagRejectedEnabled())
                    {
                        this.imapClient.flagMessage(emailMessage, this.imapConfig.getFlagRejectedWithBits(),
                                this.imapConfig.getFlagRejectedWithUnsetBits(), this.imapConfig.getFlagRejectedWithName(),
                                this.imapConfig.getFlagRejectedWithUnsetName());
                    }
                    final String moveRejectedPath = entry.getMoveRejectedPath();
                    if (moveRejectedPath != null)
                    {
                        this.imapClient.moveMessage(emailMessage, moveRejectedPath);
                    }
                }
                catch (final EmailMessageException are)
                {
                    LOGGER.warn("Failed to mark/move rejected email", are);
                }

                throw eme;
            }
            catch (final Throwable e)
            {
                if (RetryingTransactionHelper.extractRetryCause(e) == null)
                {
                    RetryingTransactionHelper.getActiveUserTransaction().setRollbackOnly();
                }
                if (e instanceof RuntimeException)
                {
                    throw (RuntimeException) e;
                }
                throw new AlfrescoRuntimeException("Error importing message", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterProcess() throws Throwable
    {
        AuthenticationUtil.popAuthentication();
        I18NUtil.setLocale(this.originalLocale.get());
        this.originalLocale.remove();
    }

    private void estimateTotalWorkSize()
    {
        final boolean filter = this.imapConfig.isProcessFilterByFlagEnabled();
        final Map<String, String> pathByFolder = this.imapConfig.getPathByFolder();
        final Collection<String> folders = this.imapConfig.getFolders();

        int workSize = 0;
        for (final String folder : folders)
        {
            final String path = pathByFolder.get(folder);
            if (filter)
            {
                workSize += this.imapClient.countMessages(path, this.imapConfig.getProcessFilterByFlagBits(),
                        this.imapConfig.getProcessFilterByUnsetFlagBits(), this.imapConfig.getProcessFilterByFlagName(),
                        this.imapConfig.getProcessFilterByUnsetFlagName());
            }
            else
            {
                workSize += this.imapClient.countMessages(path);
            }
        }
        this.previouslyEstimatedWorkSize = workSize;
    }

    private KeyStore initTrustStore(final String path, final String type, final String passphrase)
    {
        KeyStore ks;
        try
        {
            ks = KeyStore.getInstance(type);
        }
        catch (final KeyStoreException kse)
        {
            throw new AlfrescoRuntimeException("No provider supports " + type, kse);
        }
        try
        {
            ks.load(new FileInputStream(path), passphrase != null ? passphrase.toCharArray() : null);
        }
        catch (final FileNotFoundException fnfe)
        {
            throw new AlfrescoRuntimeException("The truststore file is not found.", fnfe);
        }
        catch (final IOException ioe)
        {
            throw new AlfrescoRuntimeException("The truststore file cannot be read.", ioe);
        }
        catch (final NoSuchAlgorithmException nsae)
        {
            throw new AlfrescoRuntimeException("Algorithm used to check the integrity of the truststore cannot be found.", nsae);
        }
        catch (final CertificateException ce)
        {
            throw new AlfrescoRuntimeException("The certificates cannot be loaded from truststore.", ce);
        }
        return ks;
    }
}
