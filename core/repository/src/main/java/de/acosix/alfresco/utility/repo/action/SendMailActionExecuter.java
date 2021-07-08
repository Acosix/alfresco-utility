/*
 * Copyright 2016 - 2021 Acosix GmbH
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
package de.acosix.alfresco.utility.repo.action;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.action.executer.MailActionExecuter;
import org.alfresco.repo.action.executer.MailActionExecuter.URLHelper;
import org.alfresco.repo.admin.SysAdminParams;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.template.DateCompareMethod;
import org.alfresco.repo.template.HasAspectMethod;
import org.alfresco.repo.template.I18NMessageMethod;
import org.alfresco.repo.template.TemplateNode;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.repo.tenant.TenantUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionServiceException;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.preference.PreferenceService;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.TemplateImageResolver;
import org.alfresco.service.cmr.repository.TemplateService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.Pair;
import org.alfresco.util.PropertyCheck;
import org.alfresco.util.UrlUtil;
import org.alfresco.util.transaction.TransactionListenerAdapter;
import org.alfresco.util.transaction.TransactionSupportUtil;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.surf.util.I18NUtil;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * This action implementation is an alternative to the default Alfresco {@link MailActionExecuter mail action}. Its aim is to have
 * <ul>
 * <li>a cleaner, more extensible code structure</li>
 * <li>support for additional parameter fields, specifically setting a reply-to address as well as arbitrary email headers</li>
 * <li>improved distinction between single email with multiple {@link #PARAM_TO TO}/{@link #PARAM_CC CC}/{@link #PARAM_BCC BCC}-addressees
 * and bulk emailing with one email per addressee based on {@link #PARAM_ONE_MAIL_PER_ADDRESSEE a one-mail-per-addresse} flag</li>
 * <li>support inline attachments where the content ID is the UUID of the node</li>
 * <li>support regular attachments</li>
 * </ul>
 *
 * @author Axel Faust
 */
public class SendMailActionExecuter extends ActionExecuterAbstractBase implements InitializingBean
{

    public static final String NAME = "acosix-mail";

    public static final String PARAM_LOCALE = MailActionExecuter.PARAM_LOCALE;

    public static final String PARAM_TO = MailActionExecuter.PARAM_TO;

    public static final String PARAM_CC = MailActionExecuter.PARAM_CC;

    public static final String PARAM_BCC = MailActionExecuter.PARAM_BCC;

    public static final String PARAM_REPLY_TO = "replyTo";

    public static final String PARAM_ONE_MAIL_PER_ADDRESSEE = "oneMailPerAddressee";

    public static final String PARAM_SUBJECT = MailActionExecuter.PARAM_SUBJECT;

    public static final String PARAM_SUBJECT_PARAMS = MailActionExecuter.PARAM_SUBJECT_PARAMS;

    public static final String PARAM_TEXT = MailActionExecuter.PARAM_TEXT;

    public static final String PARAM_HTML = MailActionExecuter.PARAM_HTML;

    public static final String PARAM_FROM = MailActionExecuter.PARAM_FROM;

    public static final String PARAM_FROM_PERSONAL_NAME = MailActionExecuter.PARAM_FROM_PERSONAL_NAME;

    public static final String PARAM_TEMPLATE = MailActionExecuter.PARAM_TEMPLATE;

    public static final String PARAM_TEMPLATE_MODEL = MailActionExecuter.PARAM_TEMPLATE_MODEL;

    public static final String PARAM_INLINE_ATTACHMENTS = "inlineAttachments";

    public static final String PARAM_ATTACHMENTS = "attachments";

    public static final String PARAM_MAIL_HEADERS = "mailHeaders";

    public static final String PARAM_IGNORE_SEND_FAILURE = MailActionExecuter.PARAM_IGNORE_SEND_FAILURE;

    public static final String PARAM_FILTER_INVALID_ADDRESSEES = "filterInvalidAddressees";

    public static final String PARAM_FAIL_IF_NO_VALID_TO_ADDRESSEES = "failIfNoValidToAddressees";

    public static final String PARAM_SEND_AFTER_COMMIT = MailActionExecuter.PARAM_SEND_AFTER_COMMIT;

    private static final Logger LOGGER = LoggerFactory.getLogger(SendMailActionExecuter.class);

    private static final String FROM_DEFAULT_ADDRESS = "alfresco@alfresco.org";

    protected JavaMailSender mailService;

    protected TransactionService transactionService;

    protected NodeService nodeService;

    protected AuthenticationService authenticationService;

    protected AuthorityService authorityService;

    protected PersonService personService;

    protected TemplateService templateService;

    protected SysAdminParams sysAdminParams;

    protected PreferenceService preferenceService;

    protected TenantService tenantService;

    protected TemplateImageResolver imageResolver;

    protected ServiceRegistry serviceRegistry;

    protected Repository repository;

    protected String headerEncoding;

    protected String fromDefaultAddress;

    protected boolean fromEnabled = true;

    protected boolean validateAddresses = true;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "mailService", this.mailService);
        PropertyCheck.mandatory(this, "transactionService", this.transactionService);
        PropertyCheck.mandatory(this, "nodeService", this.nodeService);
        PropertyCheck.mandatory(this, "authenticationService", this.authenticationService);
        PropertyCheck.mandatory(this, "authorityService", this.authorityService);
        PropertyCheck.mandatory(this, "personService", this.personService);
        PropertyCheck.mandatory(this, "templateService", this.templateService);
        PropertyCheck.mandatory(this, "sysAdminParams", this.sysAdminParams);
        PropertyCheck.mandatory(this, "tenantService", this.tenantService);
        PropertyCheck.mandatory(this, "serviceRegistry", this.serviceRegistry);
        PropertyCheck.mandatory(this, "repository", this.repository);

        if (this.fromDefaultAddress == null || this.fromDefaultAddress.trim().isEmpty())
        {
            this.fromDefaultAddress = FROM_DEFAULT_ADDRESS;
        }
    }

    /**
     * @param mailService
     *     the mailService to set
     */
    public void setMailService(final JavaMailSender mailService)
    {
        this.mailService = mailService;
    }

    /**
     * @param transactionService
     *     the transactionService to set
     */
    public void setTransactionService(final TransactionService transactionService)
    {
        this.transactionService = transactionService;
    }

    /**
     * @param nodeService
     *     the nodeService to set
     */
    public void setNodeService(final NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    /**
     * @param authenticationService
     *     the authenticationService to set
     */
    public void setAuthenticationService(final AuthenticationService authenticationService)
    {
        this.authenticationService = authenticationService;
    }

    /**
     * @param authorityService
     *     the authorityService to set
     */
    public void setAuthorityService(final AuthorityService authorityService)
    {
        this.authorityService = authorityService;
    }

    /**
     * @param personService
     *     the personService to set
     */
    public void setPersonService(final PersonService personService)
    {
        this.personService = personService;
    }

    /**
     * @param templateService
     *     the templateService to set
     */
    public void setTemplateService(final TemplateService templateService)
    {
        this.templateService = templateService;
    }

    /**
     * @param sysAdminParams
     *     the sysAdminParams to set
     */
    public void setSysAdminParams(final SysAdminParams sysAdminParams)
    {
        this.sysAdminParams = sysAdminParams;
    }

    /**
     * @param preferenceService
     *     the preferenceService to set
     */
    public void setPreferenceService(final PreferenceService preferenceService)
    {
        this.preferenceService = preferenceService;
    }

    /**
     * @param tenantService
     *     the tenantService to set
     */
    public void setTenantService(final TenantService tenantService)
    {
        this.tenantService = tenantService;
    }

    /**
     * @param imageResolver
     *     the imageResolver to set
     */
    public void setImageResolver(final TemplateImageResolver imageResolver)
    {
        this.imageResolver = imageResolver;
    }

    /**
     * @param serviceRegistry
     *     the serviceRegistry to set
     */
    public void setServiceRegistry(final ServiceRegistry serviceRegistry)
    {
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * @param repository
     *     the repository to set
     */
    public void setRepository(final Repository repository)
    {
        this.repository = repository;
    }

    /**
     * @param headerEncoding
     *     the headerEncoding to set
     */
    public void setHeaderEncoding(final String headerEncoding)
    {
        this.headerEncoding = headerEncoding;
    }

    /**
     * @param fromDefaultAddress
     *     the fromDefaultAddress to set
     */
    public void setFromDefaultAddress(final String fromDefaultAddress)
    {
        this.fromDefaultAddress = fromDefaultAddress;
    }

    /**
     * @param fromEnabled
     *     the fromEnabled to set
     */
    public void setFromEnabled(final boolean fromEnabled)
    {
        this.fromEnabled = fromEnabled;
    }

    /**
     * @param validateAddresses
     *     the validateAddresses to set
     */
    public void setValidateAddresses(final boolean validateAddresses)
    {
        this.validateAddresses = validateAddresses;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void addParameterDefinitions(final List<ParameterDefinition> paramList)
    {
        paramList.add(new ParameterDefinitionImpl(PARAM_TO, DataTypeDefinition.TEXT, false, this.getParamDisplayLabel(PARAM_TO), true));
        paramList.add(new ParameterDefinitionImpl(PARAM_CC, DataTypeDefinition.TEXT, false, this.getParamDisplayLabel(PARAM_CC), true));
        paramList.add(new ParameterDefinitionImpl(PARAM_BCC, DataTypeDefinition.TEXT, false, this.getParamDisplayLabel(PARAM_BCC), true));
        paramList.add(
                new ParameterDefinitionImpl(PARAM_REPLY_TO, DataTypeDefinition.TEXT, false, this.getParamDisplayLabel(PARAM_REPLY_TO)));
        paramList.add(new ParameterDefinitionImpl(PARAM_FROM, DataTypeDefinition.TEXT, false, this.getParamDisplayLabel(PARAM_FROM)));
        paramList.add(new ParameterDefinitionImpl(PARAM_ONE_MAIL_PER_ADDRESSEE, DataTypeDefinition.BOOLEAN, false,
                this.getParamDisplayLabel(PARAM_ONE_MAIL_PER_ADDRESSEE)));
        paramList.add(new ParameterDefinitionImpl(PARAM_SUBJECT, DataTypeDefinition.TEXT, true, this.getParamDisplayLabel(PARAM_SUBJECT)));
        paramList.add(new ParameterDefinitionImpl(PARAM_TEXT, DataTypeDefinition.TEXT, false, this.getParamDisplayLabel(PARAM_TEXT)));
        paramList.add(new ParameterDefinitionImpl(PARAM_TEMPLATE, DataTypeDefinition.NODE_REF, false,
                this.getParamDisplayLabel(PARAM_TEMPLATE), false, "ac-email-templates"));
        paramList.add(new ParameterDefinitionImpl(PARAM_TEMPLATE_MODEL, DataTypeDefinition.ANY, false,
                this.getParamDisplayLabel(PARAM_TEMPLATE_MODEL), true));
        paramList.add(new ParameterDefinitionImpl(PARAM_MAIL_HEADERS, DataTypeDefinition.ANY, false,
                this.getParamDisplayLabel(PARAM_MAIL_HEADERS), true));
        paramList.add(new ParameterDefinitionImpl(PARAM_ATTACHMENTS, DataTypeDefinition.NODE_REF, false,
                this.getParamDisplayLabel(PARAM_ATTACHMENTS), true));
        paramList.add(new ParameterDefinitionImpl(PARAM_FILTER_INVALID_ADDRESSEES, DataTypeDefinition.BOOLEAN, false,
                this.getParamDisplayLabel(PARAM_FILTER_INVALID_ADDRESSEES)));
        paramList.add(new ParameterDefinitionImpl(PARAM_IGNORE_SEND_FAILURE, DataTypeDefinition.BOOLEAN, false,
                this.getParamDisplayLabel(PARAM_IGNORE_SEND_FAILURE)));
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void executeImpl(final Action action, final NodeRef actionedUponNodeRef)
    {
        if (actionedUponNodeRef != null && !this.nodeService.exists(actionedUponNodeRef))
        {
            throw new AlfrescoRuntimeException("Action executed against non-existing node " + actionedUponNodeRef);
        }

        final boolean filterInvalidAddressees = Boolean.TRUE
                .equals(DefaultTypeConverter.INSTANCE.convert(Boolean.class, action.getParameterValue(PARAM_FILTER_INVALID_ADDRESSEES)));
        final boolean failIfNoValidToAddressees = Boolean.TRUE.equals(
                DefaultTypeConverter.INSTANCE.convert(Boolean.class, action.getParameterValue(PARAM_FAIL_IF_NO_VALID_TO_ADDRESSEES)));

        final Pair<InternetAddress, Locale> fromAndMailDefaultLocale = this.resolveFromAndMailDefaultLocale(action);
        final List<Pair<InternetAddress, Locale>> addresseesAndLocale = this.resolveToAddresseesAndLocale(action, filterInvalidAddressees);

        if (!addresseesAndLocale.isEmpty())
        {
            final List<InternetAddress> ccAddressees = this.resolveAddressees(action, PARAM_CC, filterInvalidAddressees);
            final List<InternetAddress> bccAddressees = this.resolveAddressees(action, PARAM_BCC, filterInvalidAddressees);
            final String replyTo = DefaultTypeConverter.INSTANCE.convert(String.class, action.getParameterValue(PARAM_REPLY_TO));

            final boolean split = Boolean.TRUE
                    .equals(DefaultTypeConverter.INSTANCE.convert(Boolean.class, action.getParameterValue(PARAM_ONE_MAIL_PER_ADDRESSEE)));

            final List<MimeMessage> messages;
            try
            {
                if (addresseesAndLocale.size() == 1 || !split)
                {
                    LOGGER.debug("Preparing single/consolidated mail message");
                    final MimeMessage message = this.prepareSingleMessage(action, actionedUponNodeRef, fromAndMailDefaultLocale,
                            addresseesAndLocale, ccAddressees, bccAddressees, replyTo);
                    messages = Collections.singletonList(message);
                }
                else
                {
                    LOGGER.debug("Preparing individual mail messages");
                    messages = new ArrayList<>(addresseesAndLocale.size());

                    for (final Pair<InternetAddress, Locale> addresseeAndLocale : addresseesAndLocale)
                    {
                        final MimeMessage message = this.prepareSingleMessage(action, actionedUponNodeRef, fromAndMailDefaultLocale,
                                Collections.singletonList(addresseeAndLocale), ccAddressees, bccAddressees, replyTo);
                        messages.add(message);
                    }
                }
            }
            catch (MailException | MessagingException mex)
            {
                throw new ActionServiceException("Failed to create / prepare the mail message", mex);
            }

            this.sendMails(action, messages);
        }
        else if (failIfNoValidToAddressees)
        {
            throw new ActionServiceException("No valid TO addressees have been specified via action parameters");
        }
    }

    protected MimeMessage prepareSingleMessage(final Action action, final NodeRef actionedUponNodeRef,
            final Pair<InternetAddress, Locale> fromAndMailDefaultLocale, final List<Pair<InternetAddress, Locale>> addresseesAndLocale,
            final List<InternetAddress> ccAddressees, final List<InternetAddress> bccAddressees, final String replyTo)
            throws MessagingException
    {
        final Locale explicitLocale = DefaultTypeConverter.INSTANCE.convert(Locale.class, action.getParameterValue(PARAM_LOCALE));

        Locale effectiveLocale;
        if (explicitLocale != null)
        {
            effectiveLocale = explicitLocale;
        }
        else
        {
            final Pair<InternetAddress, Locale> primaryAddressee = addresseesAndLocale.get(0);
            if (primaryAddressee.getSecond() != null)
            {
                effectiveLocale = primaryAddressee.getSecond();
            }
            else
            {
                effectiveLocale = fromAndMailDefaultLocale.getSecond();
            }
        }

        final MimeMessage mimeMessage = this.prepareMessage(action, actionedUponNodeRef, effectiveLocale, mmh -> {
            mmh.setFrom(fromAndMailDefaultLocale.getFirst());
            mmh.setTo(addresseesAndLocale.stream().map(Pair::getFirst).collect(Collectors.toList()).toArray(new InternetAddress[0]));
            if (!ccAddressees.isEmpty())
            {
                mmh.setCc(ccAddressees.toArray(new InternetAddress[0]));
            }
            if (!bccAddressees.isEmpty())
            {
                mmh.setBcc(bccAddressees.toArray(new InternetAddress[0]));
            }
            if (replyTo != null && !replyTo.trim().isEmpty())
            {
                mmh.setReplyTo(replyTo);
            }
        });

        this.handleAttachments(true, action, mimeMessage);
        this.handleAttachments(false, action, mimeMessage);

        return mimeMessage;
    }

    protected List<Pair<InternetAddress, Locale>> resolveToAddresseesAndLocale(final Action action, final boolean filterInvalidAddresses)
    {
        final Serializable toParam = action.getParameterValue(PARAM_TO);
        final List<String> effectiveTo = this.resolveEffectiveAddressees(toParam);

        final List<Pair<InternetAddress, Locale>> addressesAndLocales = effectiveTo.stream().map(t -> {
            try
            {
                InternetAddress address;
                Locale locale = null;
                if (t.matches("^[^<]+<[^>]+@[^>]+>$"))
                {
                    // looks like an address already in the form
                    // "Doe, John <john.doe@acme.com>"
                    // potentially mapped from recorded properties such as cm:originator / imap:messageFrom
                    final int addressStartIdx = t.indexOf('<');
                    final String personalName = t.substring(0, addressStartIdx).trim();
                    final String email = t.substring(addressStartIdx + 1, t.length() - 1).trim();
                    try
                    {
                        address = new InternetAddress(email, personalName);
                    }
                    catch (final UnsupportedEncodingException e)
                    {
                        address = new InternetAddress(email);
                    }
                }
                else if (this.personExists(t))
                {
                    locale = this.getLocaleForUser(t);
                    final String email = this.getPersonEmail(t);
                    if (email != null && !email.trim().isEmpty() && this.isValidAddress(email))
                    {
                        address = new InternetAddress(email);
                    }
                    else if (this.isValidAddress(t))
                    {
                        address = new InternetAddress(t);
                    }
                    else if (!filterInvalidAddresses)
                    {
                        throw new ActionServiceException("Invalid email address " + email + " for user " + t);
                    }
                    else
                    {
                        address = null;
                    }
                }
                else if (this.isValidAddress(t))
                {
                    address = new InternetAddress(t);
                }
                else if (!filterInvalidAddresses)
                {
                    throw new ActionServiceException("Invalid email address " + t);
                }
                else
                {
                    address = null;
                }
                return new Pair<>(address, locale);
            }
            catch (final AddressException e)
            {
                throw new ActionServiceException("Failed to handle addressee " + t, e);
            }
        }).filter(p -> p.getFirst() != null).collect(Collectors.toList());

        LOGGER.debug("Resolved addresses and locales {} from parameter to", addressesAndLocales);

        return addressesAndLocales;
    }

    protected List<InternetAddress> resolveAddressees(final Action action, final String paramName, final boolean filterInvalidAddresses)
    {
        final Serializable addresseeParam = action.getParameterValue(paramName);
        final List<String> effectiveTo = this.resolveEffectiveAddressees(addresseeParam);

        final List<InternetAddress> addresses = effectiveTo.stream().map(t -> {
            try
            {
                InternetAddress address;
                if (t.matches("^[^<]+<[^>]+@[^>]+>$"))
                {
                    // looks like an address already in the form
                    // "Doe, John <john.doe@acme.com>"
                    // potentially mapped from recorded properties such as cm:originator / imap:messageFrom
                    final int addressStartIdx = t.indexOf('<');
                    final String personalName = t.substring(0, addressStartIdx).trim();
                    final String email = t.substring(addressStartIdx + 1, t.length() - 1).trim();
                    try
                    {
                        address = new InternetAddress(email, personalName);
                    }
                    catch (final UnsupportedEncodingException e)
                    {
                        address = new InternetAddress(email);
                    }
                }
                else if (this.personExists(t))
                {
                    final String email = this.getPersonEmail(t);
                    if (email != null && !email.trim().isEmpty() && this.isValidAddress(email))
                    {
                        address = new InternetAddress(email);
                    }
                    else if (this.isValidAddress(t))
                    {
                        address = new InternetAddress(t);
                    }
                    else if (!filterInvalidAddresses)
                    {
                        throw new ActionServiceException("Invalid email address " + email + " for user " + t);
                    }
                    else
                    {
                        address = null;
                    }
                }
                else if (this.isValidAddress(t))
                {
                    address = new InternetAddress(t);
                }
                else if (!filterInvalidAddresses)
                {
                    throw new ActionServiceException("Invalid email address " + t);
                }
                else
                {
                    address = null;
                }
                return address;
            }
            catch (final AddressException e)
            {
                throw new ActionServiceException("Failed to handle addressee " + t, e);
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());

        LOGGER.debug("Resolved addresses {} from parameter {}", addresses, paramName);

        return addresses;
    }

    protected Pair<InternetAddress, Locale> resolveFromAndMailDefaultLocale(final Action action)
    {
        try
        {
            InternetAddress address = null;
            Locale locale = I18NUtil.getLocale();

            final String paramFrom = DefaultTypeConverter.INSTANCE.convert(String.class, action.getParameterValue(PARAM_FROM));
            if (this.fromEnabled && paramFrom != null && !paramFrom.trim().isEmpty())
            {
                LOGGER.debug("From specified via action parameter as {}", paramFrom);

                final String personalName = DefaultTypeConverter.INSTANCE.convert(String.class,
                        action.getParameterValue(PARAM_FROM_PERSONAL_NAME));

                if (paramFrom.matches("^[^<]+<[^>]+@[^>]+>$"))
                {
                    // looks like an address already in the form
                    // "Doe, John <john.doe@acme.com>"
                    // potentially mapped from recorded properties such as cm:originator / imap:messageFrom
                    // use the included personal name only of no explicit personal name was specified
                    final int addressStartIdx = paramFrom.indexOf('<');
                    final String includedPersonalName = paramFrom.substring(0, addressStartIdx).trim();
                    final String email = paramFrom.substring(addressStartIdx + 1, paramFrom.length() - 1).trim();
                    try
                    {
                        address = new InternetAddress(email,
                                personalName != null && !personalName.trim().isEmpty() ? personalName : includedPersonalName);
                    }
                    catch (final UnsupportedEncodingException e)
                    {
                        address = new InternetAddress(email);
                    }
                }
                else if (personalName != null && !personalName.trim().isEmpty())
                {
                    try
                    {
                        address = new InternetAddress(paramFrom, personalName);
                    }
                    catch (final UnsupportedEncodingException e)
                    {
                        address = new InternetAddress(paramFrom);
                    }
                }
                else
                {
                    address = new InternetAddress(paramFrom);
                }

                if (this.personExists(paramFrom))
                {
                    final Locale userLocale = this.getLocaleForUser(paramFrom);
                    locale = userLocale != null ? userLocale : locale;
                }
            }

            if (address == null && !this.authenticationService.isCurrentUserTheSystemUser())
            {
                final String currentUser = this.authenticationService.getCurrentUserName();
                if (currentUser != null && !this.authorityService.isGuestAuthority(currentUser) && this.personExists(currentUser))
                {
                    final String email = this.getPersonEmail(currentUser);
                    if (email != null && !email.trim().isEmpty())
                    {
                        address = new InternetAddress(email);
                    }
                    else
                    {
                        LOGGER.info(
                                "User {} does not have a personal email address set - falling back to the default from-address for mail to {}",
                                currentUser, action.getParameterValue(PARAM_TO));
                    }

                    final Locale userLocale = this.getLocaleForUser(currentUser);
                    locale = userLocale != null ? userLocale : locale;
                }
            }

            if (address == null)
            {
                address = new InternetAddress(this.fromDefaultAddress);
            }
            return new Pair<>(address, locale);
        }
        catch (final AddressException e)
        {
            throw new ActionServiceException("Failed to resolve sender mail address", e);
        }
    }

    protected List<String> resolveEffectiveAddressees(final Serializable addresseeParamValue)
    {
        Collection<String> recipients;
        if (addresseeParamValue instanceof Collection<?>)
        {
            recipients = DefaultTypeConverter.INSTANCE.convert(String.class, (Collection<?>) addresseeParamValue);
        }
        else
        {
            recipients = Arrays.asList(DefaultTypeConverter.INSTANCE.convert(String.class, addresseeParamValue));
        }

        final List<String> addressees = recipients.stream().filter(auth -> auth != null && !auth.trim().isEmpty()).map(auth -> {
            final AuthorityType authType = AuthorityType.getAuthorityType(auth);

            Collection<String> authResolved;
            if (authType == AuthorityType.GROUP)
            {
                authResolved = this.authorityService.getContainedAuthorities(AuthorityType.USER, auth, false);
            }
            else if (authType == AuthorityType.EVERYONE)
            {
                throw new AlfrescoRuntimeException(
                        "Sending emails to all users is not supported - use of a dedicated mailing list address is recommended");
            }
            else
            {
                authResolved = Collections.singleton(auth);
            }
            return authResolved;
        }).flatMap(Collection::stream).filter(t -> {
            boolean keep = true;
            if (this.personExists(t) && !this.personService.isEnabled(t))
            {
                final NodeRef person = this.getPerson(t);
                keep = this.nodeService.hasAspect(person, ContentModel.ASPECT_ANULLABLE);
            }
            return keep;
        }).collect(Collectors.toList());

        LOGGER.debug("Resolved effective addressees {} from input {}", addressees, addresseeParamValue);

        return addressees;
    }

    protected MimeMessage prepareMessage(final Action action, final NodeRef actionedUponNodeRef, final Locale effectiveLocale,
            final MessageAddressHandler addressHandler) throws MailException, MessagingException
    {
        final MimeMessage mimeMessage = this.mailService.createMimeMessage();
        final MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);
        this.prepareMessageHeaders(effectiveLocale, action, mimeMessage);

        messageHelper.setSubject(this.prepareSubject(effectiveLocale, action));

        final Pair<String, String> textOrHtml = this.prepareTextOrHtml(effectiveLocale, action, actionedUponNodeRef);
        final String text = textOrHtml.getFirst();
        final String html = textOrHtml.getSecond();

        if (text != null && !text.trim().isEmpty() && html != null && !html.trim().isEmpty())
        {
            messageHelper.setText(text, html);
        }
        else if (text != null && !text.trim().isEmpty())
        {
            messageHelper.setText(text, false);
        }
        else if (html != null && !html.trim().isEmpty())
        {
            messageHelper.setText(html, true);
        }
        else
        {
            throw new ActionServiceException("Mail content must be specified");
        }

        addressHandler.addAddressDetails(messageHelper);

        return mimeMessage;
    }

    protected void prepareMessageHeaders(final Locale effectiveLocale, final Action action, final MimeMessage mimeMessage)
            throws MessagingException
    {
        if (this.headerEncoding != null && !this.headerEncoding.trim().isEmpty())
        {
            mimeMessage.setHeader("Content-Transfer-Encoding", this.headerEncoding);
        }
        mimeMessage.setHeader("Content-Language", effectiveLocale.toString().replace('_', '-'));

        final Serializable headers = action.getParameterValue(PARAM_MAIL_HEADERS);
        if (headers instanceof Map<?, ?>)
        {
            final Map<?, ?> headersM = (Map<?, ?>) headers;
            for (final Entry<?, ?> header : headersM.entrySet())
            {
                final Object key = header.getKey();
                final Object value = header.getValue();

                if (key instanceof String && value instanceof String)
                {
                    mimeMessage.setHeader((String) key, (String) value);
                }
                else
                {
                    throw new ActionServiceException("Mail headers parameter must be a map of String header names to String header values");
                }
            }
        }
        else if (headers != null)
        {
            throw new ActionServiceException("Mail headers parameter must be a map of String header names to String header values");
        }
    }

    protected String prepareSubject(final Locale effectiveLocale, final Action action)
    {
        final String subject = DefaultTypeConverter.INSTANCE.convert(String.class, action.getParameterValue(PARAM_SUBJECT));
        final Object subjectParams = action.getParameterValue(PARAM_SUBJECT_PARAMS);
        Object[] subjectParamsA;
        if (subjectParams instanceof Collection<?>)
        {
            subjectParamsA = ((Collection<?>) subjectParams).toArray();
        }
        else if (subjectParams instanceof Object[])
        {
            subjectParamsA = (Object[]) subjectParams;
        }
        else if (subjectParams != null)
        {
            subjectParamsA = new Object[] { subjectParams };
        }
        else
        {
            subjectParamsA = new Object[0];
        }
        final String localiedSubject = I18NUtil.getMessage(subject, effectiveLocale, subjectParamsA);
        return localiedSubject != null ? localiedSubject : subject;
    }

    @SuppressWarnings("unchecked")
    protected Pair<String, String> prepareTextOrHtml(final Locale effectiveLocale, final Action action, final NodeRef actionedUponNodeRef)
    {
        final NodeRef template = DefaultTypeConverter.INSTANCE.convert(NodeRef.class, action.getParameterValue(PARAM_TEMPLATE));
        String text = null;
        String html = null;
        if (template != null)
        {
            Map<String, Object> templateModelM = Collections.emptyMap();
            final Serializable templateModel = action.getParameterValue(PARAM_TEMPLATE_MODEL);
            if (templateModel != null)
            {
                if (templateModel instanceof Map<?, ?>)
                {
                    final boolean validMap = ((Map<?, ?>) templateModel).isEmpty()
                            || ((Map<?, ?>) templateModel).keySet().stream().allMatch(String.class::isInstance);
                    if (!validMap)
                    {
                        throw new ActionServiceException("Template model must be a map of String keys to any-typed values");
                    }
                    templateModelM = (Map<String, Object>) templateModel;
                }
                else
                {
                    throw new ActionServiceException("Template model must be a map of String keys to any-typed values");
                }
            }

            final Map<String, Object> model = this.createTemplateModel(templateModelM, actionedUponNodeRef, template);
            final String templateResult = this.templateService.processTemplate("freemarker", template.toString(), model, effectiveLocale)
                    .trim();
            final String initialFragment = templateResult.substring(0, templateResult.length() < 16 ? templateResult.length() : 16)
                    .toLowerCase(Locale.ENGLISH);

            if (initialFragment.startsWith("<html ") || initialFragment.startsWith("<html>")
                    || initialFragment.startsWith("<!doctype html>"))
            {
                html = templateResult;
            }
            else
            {
                text = templateResult;
            }
        }
        else
        {
            html = DefaultTypeConverter.INSTANCE.convert(String.class, action.getParameterValue(PARAM_HTML));
            text = DefaultTypeConverter.INSTANCE.convert(String.class, action.getParameterValue(PARAM_TEXT));

            if (text.matches("^\\s*<[hH][tT][mM][lL]") || text.matches("\\s*<!DOCTYPE [hH][tT][mM][lL]"))
            {
                if (html != null)
                {
                    throw new ActionServiceException(
                            "Invalid parameters - text contains HTML content while HTML content was explicitly specified");
                }
                html = text;
                text = null;
            }
        }

        return new Pair<>(text, html);
    }

    protected Map<String, Object> createTemplateModel(final Map<String, Object> suppliedModel, final NodeRef ref, final NodeRef template)
    {
        final String currentUserName = this.authenticationService.getCurrentUserName();
        NodeRef fromPerson = null;
        if (!this.authenticationService.isCurrentUserTheSystemUser() && !this.authorityService.isGuestAuthority(currentUserName))
        {
            fromPerson = this.repository.getPerson();
        }
        final NodeRef companyHome = this.repository.getCompanyHome();
        final NodeRef userHome = fromPerson != null ? this.repository.getUserHome(fromPerson) : null;
        final Map<String, Object> model = this.templateService.buildDefaultModel(fromPerson, companyHome, userHome, template,
                this.imageResolver);

        if (ref != null)
        {
            model.put("document", new TemplateNode(ref, this.serviceRegistry, this.imageResolver));
            final NodeRef parent = this.serviceRegistry.getNodeService().getPrimaryParent(ref).getParentRef();
            model.put("space", new TemplateNode(parent, this.serviceRegistry, this.imageResolver));
        }

        model.put("date", new Date());
        model.put("hasAspect", new HasAspectMethod());
        model.put("message", new I18NMessageMethod());
        model.put("dateCompare", new DateCompareMethod());

        model.put("url", new URLHelper(this.sysAdminParams));
        model.put(TemplateService.KEY_SHARE_URL, UrlUtil.getShareUrl(this.sysAdminParams));

        suppliedModel.forEach((k, v) -> {
            if (model.containsKey(k))
            {
                LOGGER.debug("Not allowing overwriting of built in model parameter {}", k);
            }
            else
            {
                model.put(k, v);
            }
        });

        return model;
    }

    protected void handleAttachments(final boolean inline, final Action action, final MimeMessage mimeMessage)
    {
        final Serializable attachments = action.getParameterValue(inline ? PARAM_INLINE_ATTACHMENTS : PARAM_ATTACHMENTS);
        Collection<NodeRef> attachmentsC;
        if (attachments instanceof Collection<?>)
        {
            attachmentsC = DefaultTypeConverter.INSTANCE.convert(NodeRef.class, (Collection<?>) attachments);
        }
        else
        {
            final NodeRef attachment = DefaultTypeConverter.INSTANCE.convert(NodeRef.class, attachments);
            attachmentsC = attachment != null ? Collections.singleton(attachment) : Collections.emptySet();
        }

        if (!attachmentsC.isEmpty())
        {
            LOGGER.debug("Handling {} attachments {}", inline ? "inline" : "regular", attachmentsC);

            final NodeService pNodeService = this.serviceRegistry.getNodeService();
            final ContentService pContentService = this.serviceRegistry.getContentService();

            final MimeMessageHelper mmh = new MimeMessageHelper(mimeMessage);
            try
            {
                for (final NodeRef attachment : attachmentsC)
                {
                    final ContentReader reader = pContentService.getReader(attachment, ContentModel.PROP_CONTENT);
                    if (reader == null || !reader.exists())
                    {
                        throw new ActionServiceException("Cannot add attachment from content-less node " + attachment);
                    }

                    if (inline)
                    {
                        mmh.addInline(attachment.getId(), reader::getContentInputStream, reader.getMimetype());
                    }
                    else
                    {
                        final String name = DefaultTypeConverter.INSTANCE.convert(String.class,
                                pNodeService.getProperty(attachment, ContentModel.PROP_NAME));
                        mmh.addAttachment(name, reader::getContentInputStream, reader.getMimetype());
                    }
                }
            }
            catch (final MessagingException mex)
            {
                throw new ActionServiceException("Failed to handle attachments", mex);
            }
        }
    }

    protected void sendMails(final Action action, final List<MimeMessage> messages)
    {
        final boolean sendAfterCommit = Boolean.TRUE
                .equals(DefaultTypeConverter.INSTANCE.convert(Boolean.class, action.getParameterValue(PARAM_SEND_AFTER_COMMIT)));
        if (sendAfterCommit)
        {
            this.sendMailsAfterCommit(action, messages);
        }
        else
        {
            messages.forEach(m -> this.sendMailImpl(action, m));
        }
    }

    protected void sendMailsAfterCommit(final Action action, final List<MimeMessage> messages)
    {
        LOGGER.debug("Scheduling {} messages to be sent after commit", messages.size());
        TransactionSupportUtil.bindListener(new TransactionListenerAdapter()
        {

            /**
             *
             * {@inheritDoc}
             */
            @Override
            public void afterCommit()
            {
                SendMailActionExecuter.this.transactionService.getRetryingTransactionHelper().doInTransaction(() -> {
                    messages.forEach(m -> SendMailActionExecuter.this.sendMailImpl(action, m));
                    return null;
                }, false, true);
            }
        }, 0);
    }

    protected void sendMailImpl(final Action action, final MimeMessage message)
    {
        Address[] recipients = null;
        try
        {
            recipients = message.getRecipients(RecipientType.TO);
            LOGGER.debug("Sending mail to {} with subject: {}", recipients, message.getSubject());
        }
        catch (final MessagingException ignore)
        {
            // NO-OP
        }

        try
        {
            this.mailService.send(message);
            LOGGER.debug("Successfully delivered mail to configured mail server");
        }
        catch (final MailException e)
        {
            final Serializable toParam = action.getParameterValue(PARAM_TO);

            LOGGER.error("Failed to send email to {}", recipients != null ? recipients : toParam, e);

            final Boolean ignoreError = DefaultTypeConverter.INSTANCE.convert(Boolean.class,
                    action.getParameterValue(PARAM_IGNORE_SEND_FAILURE));

            if (!Boolean.TRUE.equals(ignoreError))
            {
                throw new ActionServiceException("Failed to send email to " + (recipients != null ? Arrays.toString(recipients) : toParam));
            }
        }
    }

    protected boolean personExists(final String user)
    {
        final boolean exists;
        final String domain = this.tenantService.getPrimaryDomain(user);
        if (domain != null)
        {
            exists = TenantUtil.runAsTenant(() -> SendMailActionExecuter.this.personService.personExists(user), domain);
        }
        else
        {
            exists = this.personService.personExists(user);
        }
        return exists;
    }

    protected NodeRef getPerson(final String user)
    {
        final NodeRef person;
        final String domain = this.tenantService.getPrimaryDomain(user); // get primary tenant
        if (domain != null)
        {
            person = TenantUtil.runAsTenant(() -> SendMailActionExecuter.this.personService.getPersonOrNull(user), domain);
        }
        else
        {
            person = this.personService.getPersonOrNull(user);
        }
        return person;
    }

    protected String getPersonEmail(final String user)
    {
        final String email;
        final NodeRef person = this.getPerson(user);
        final String domain = this.tenantService.getPrimaryDomain(user);
        if (domain != null)
        {
            email = TenantUtil.runAsTenant(() -> DefaultTypeConverter.INSTANCE.convert(String.class,
                    SendMailActionExecuter.this.nodeService.getProperty(person, ContentModel.PROP_EMAIL)), domain);
        }
        else
        {
            email = DefaultTypeConverter.INSTANCE.convert(String.class, this.nodeService.getProperty(person, ContentModel.PROP_EMAIL));
        }
        return email;
    }

    protected Locale getLocaleForUser(final String user)
    {
        Locale locale = null;

        final String domain = this.tenantService.getPrimaryDomain(user);

        if (domain != null)
        {
            locale = TenantUtil.runAsSystemTenant(() -> DefaultTypeConverter.INSTANCE.convert(Locale.class,
                    SendMailActionExecuter.this.preferenceService.getPreference(user, "locale")), domain);
        }
        else
        {
            if (this.personExists(user))
            {
                locale = AuthenticationUtil.runAsSystem(() -> DefaultTypeConverter.INSTANCE.convert(Locale.class,
                        SendMailActionExecuter.this.preferenceService.getPreference(user, "locale")));
            }
        }

        return locale;
    }

    protected boolean isValidAddress(final String address)
    {
        return !this.validateAddresses || EmailValidator.getInstance(true).isValid(address);
    }

    protected interface MessageAddressHandler
    {

        void addAddressDetails(MimeMessageHelper mimeMessageHelper) throws MessagingException, AddressException;
    }
}
