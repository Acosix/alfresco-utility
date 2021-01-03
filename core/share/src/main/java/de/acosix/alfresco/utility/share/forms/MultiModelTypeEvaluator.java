package de.acosix.alfresco.utility.share.forms;

import java.util.Arrays;

import org.alfresco.web.config.forms.ModelTypeEvaluator;

/**
 * This evaluator implementation supports the evaluation of a model type against a list of types (separated by semi-colons) in the
 * condition of a config section.
 *
 * @author Axel Faust
 */
public class MultiModelTypeEvaluator extends ModelTypeEvaluator
{

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean applies(final Object obj, final String condition)
    {
        final String[] conditionFragments = condition.split(";");
        final boolean result = Arrays.asList(conditionFragments).stream().anyMatch(c -> super.applies(obj, c));
        return result;
    }
}
