package de.acosix.alfresco.utility.share.surf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.config.ConfigElement;
import org.springframework.extensions.config.WebFrameworkConfigElement;
import org.springframework.extensions.surf.CssThemeHandler;
import org.springframework.extensions.surf.LessForJavaCssThemeHandler;
import org.springframework.extensions.surf.RequestContext;
import org.springframework.extensions.surf.support.ThreadLocalRequestContext;
import org.springframework.extensions.surf.types.Theme;
import org.springframework.extensions.webscripts.ScriptConfigModel;

import com.asual.lesscss.LessEngine;
import com.asual.lesscss.LessException;

/**
 * This class is a combined copy of the Alfresco {@code ExternalLessCssThemeHandler} and {@code LessCssThemeHandler} available from Alfresco
 * 5.1 onwards (Surf version 6.0+). This
 * copy was necessary to allow this module to compile against Alfresco 5.0 API baseline.
 *
 * This is a LESS CSS handler driven by a pre-configured external LESS process.
 * <p>
 * Typically uses a Node "lessc" module preinstalled via NPM or similar. This is not
 * expected to be used in production environments where adding additional 3rd party
 * modules to the web-tier is not desired or permitted.
 *
 * @see <a href="http://lesscss.org/#using-less-installation">http://lesscss.org/</a>
 *
 * @author Kevin Roast
 */
public class ExternalLessCssThemeHandler extends CssThemeHandler
{

    public static final String LESS_TOKEN = "less-variables";

    private static final Log logger = LogFactory.getLog(ExternalLessCssThemeHandler.class);

    /**
     * The default LESS configuration. This will be populated with the contents of a file referenced by the
     * web-framework > defaults > dojo-pages > default-less-configuration.
     */
    private String defaultLessConfig = null;

    private String cmd;

    /**
     * @param cmd
     *            The external cmd to execute. For example Node lessc this would be "lessc -".
     *            The command must be able to accept LESS CSS as stdin and return output from stdout.
     */
    public void setCmd(final String cmd)
    {
        this.cmd = cmd;
    }

    /**
     * Sets up a new instance.
     */
    public ExternalLessCssThemeHandler()
    {
    }

    /**
     * Overrides the default implementation to add LESS processing capabilities.
     *
     * @param path
     *            The path of the file being processed (used only for error output)
     * @param cssContents
     *            The CSS to process
     * @throws IOException
     *             when accessing file contents.
     */
    @Override
    public String processCssThemes(final String path, final StringBuilder cssContents) throws IOException
    {
        if (this.cmd == null || this.cmd.length() == 0)
        {
            throw new IllegalArgumentException("External LESS 'cmd' not set correctly in bean config.");
        }

        // setup our external process and retrieve streams - IO exception is handled in caller
        final Process proc = Runtime.getRuntime().exec(this.cmd);

        // if we get here, retrieve the streams for processing
        final BufferedWriter stdIn = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream(), StandardCharsets.UTF_8.name()));
        final BufferedReader stdOut = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8.name()));
        final BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream(), StandardCharsets.UTF_8.name()));

        try
        {
            // push our CSS to the standard Input of the external process
            stdIn.append(this.getLessVariables());
            stdIn.append(cssContents.toString());
            stdIn.close();

            // read the output from the command
            StringBuilder buf = new StringBuilder(1024);
            String s;
            while ((s = stdOut.readLine()) != null)
            {
                buf.append(s);
            }
            stdOut.close();

            // read any errors from the attempted command
            if ((s = stdError.readLine()) != null)
            {
                // error occured, collect information and throw exception with the message
                buf = new StringBuilder("Error during external LESS compilation for path: ").append(path).append("\r\n");
                do
                {
                    buf.append(s);
                }
                while ((s = stdError.readLine()) != null);
                stdError.close();
                throw new IOException(buf.toString());
            }

            return buf.toString();
        }
        finally
        {
            stdError.close();
            stdOut.close();
            stdIn.close();
        }
    }

    /**
     * Looks for the LESS CSS token which should contain the LESS style variables that
     * can be applied to each CSS file. This will be prepended to each CSS file processed.
     *
     * @return The String of LESS variables.
     */
    public String getLessVariables()
    {
        String variables = this.getDefaultLessConfig();
        Theme currentTheme = ThreadLocalRequestContext.getRequestContext().getTheme();
        if (currentTheme == null)
        {
            currentTheme = ThreadLocalRequestContext.getRequestContext().getObjectService().getTheme("default");
        }
        final String themeVariables = currentTheme.getCssTokens().get(LessForJavaCssThemeHandler.LESS_TOKEN);
        if (themeVariables != null)
        {
            // Add a new line just to make sure the first theme specific variable isn't appended to
            // the end of the last default variable!
            variables += "\n" + themeVariables;
        }
        return variables;
    }

    /**
     * This function is used to log exceptions that occur during LESS compilation. Unfortunately the
     * {@link LessException} that is thrown from the {@link LessEngine} does not capture all exception
     * eventualities. When a JavaScript error occurs in Rhino this can result in a {@link ClassCastException}
     * which needs to be caught separately. Currently Surf still supports Java 6 so cannot process
     * multiple exceptions so the error handling has been abstracted to a helper method.
     *
     * @param e
     *            The exception that has been thrown.
     * @param path
     *            The path being processed that caused the exception
     * @return The error message generated
     */
    public String logLessException(final Exception e, final String path)
    {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        final String errorMsg = "LESS for Java Engine error compiling: '" + path + "': " + sw.toString();
        if (logger.isErrorEnabled())
        {
            logger.error(errorMsg);
        }
        return errorMsg;
    }

    /**
     * Returns the current default LESS configuration. If it has not previously been retrieved then it will
     * attempt to load it.
     *
     * @return A String containing the default LESS configuration variables.
     */
    @SuppressWarnings("unchecked")
    protected String getDefaultLessConfig()
    {
        final RequestContext rc = ThreadLocalRequestContext.getRequestContext();
        if (this.defaultLessConfig == null)
        {
            String defaultLessConfigPath = null;
            final ScriptConfigModel config = rc.getExtendedScriptConfigModel(null);
            final Map<String, ConfigElement> configs = (Map<String, ConfigElement>) config.getScoped().get("WebFramework");
            if (configs != null)
            {
                final WebFrameworkConfigElement wfce = (WebFrameworkConfigElement) configs.get("web-framework");
                defaultLessConfigPath = wfce.getDojoDefaultLessConfig();
            }
            else
            {
                defaultLessConfigPath = this.getWebFrameworkConfigElement().getDojoDefaultLessConfig();
            }
            try
            {
                final InputStream in = this.getDependencyHandler().getResourceInputStream(defaultLessConfigPath);
                if (in != null)
                {
                    this.defaultLessConfig = this.getDependencyHandler().convertResourceToString(in);
                }
                else
                {
                    if (logger.isErrorEnabled())
                    {
                        logger.error("Could not find the default LESS configuration at: " + defaultLessConfigPath);
                    }
                    // Set the configuration as the empty string as it's not in the configured location
                    this.defaultLessConfig = "";
                }
            }
            catch (final IOException e)
            {
                if (logger.isErrorEnabled())
                {
                    logger.error("An exception occurred retrieving the default LESS configuration from: " + defaultLessConfigPath, e);
                }
            }
        }
        return this.defaultLessConfig;
    }
}
