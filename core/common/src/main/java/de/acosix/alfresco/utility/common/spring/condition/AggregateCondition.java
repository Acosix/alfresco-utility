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
package de.acosix.alfresco.utility.common.spring.condition;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.util.ParameterCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * @author Axel Faust
 */
public class AggregateCondition extends BaseBeanDefinitionPostProcessorCondition
{

    private static final Logger LOGGER = LoggerFactory.getLogger(AggregateCondition.class);

    /**
     *
     * @author Axel Faust
     */
    public static enum AggregateMode
    {
        AND,
        OR;
    }

    protected AggregateMode aggregateMode = AggregateMode.AND;

    protected final List<BeanDefinitionPostProcessorCondition> conditions = new ArrayList<>();

    /**
     * Constructs a new instance of this condition for subsequent configuration.
     */
    public AggregateCondition()
    {
        // NO-OP
    }

    /**
     * Constructs a new instance of this condition with an initial configuration.
     *
     * @param conditions
     *            the conditions to aggregate
     * @param aggregateMode
     *            the aggregation mode to apply to the collection of conditions
     * @param negate
     *            {@code true} if the result of this condition's check should be negated / inversed, {@code false} if not
     */
    public AggregateCondition(final List<BeanDefinitionPostProcessorCondition> conditions, final AggregateMode aggregateMode,
            final boolean negate)
    {
        super();
        ParameterCheck.mandatoryCollection("conditions", conditions);
        this.conditions.addAll(conditions);
        this.aggregateMode = aggregateMode;
        this.negate = negate;
    }

    /**
     * @return the aggregation mode to apply to the collection of conditions
     */
    public AggregateMode getAggregateMode()
    {
        return this.aggregateMode;
    }

    /**
     * @param aggregateMode
     *            the aggregation mode to apply to the collection of conditions
     */
    public void setAggregateMode(final AggregateMode aggregateMode)
    {
        this.aggregateMode = aggregateMode;
    }

    /**
     * @return the conditions to aggregate
     */
    public List<BeanDefinitionPostProcessorCondition> getConditions()
    {
        return new ArrayList<>(this.conditions);
    }

    /**
     * @param conditions
     *            the conditions to aggregate
     */
    public void setConditions(final List<BeanDefinitionPostProcessorCondition> conditions)
    {
        ParameterCheck.mandatoryCollection("conditions", conditions);
        this.conditions.clear();
        this.conditions.addAll(conditions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean applies(final BeanFactory factory)
    {
        boolean baseApplies = false;

        if (this.aggregateMode == AggregateMode.AND)
        {
            baseApplies = this.conditions.stream().allMatch(condition -> condition.applies(factory));
        }
        else
        {
            baseApplies = this.conditions.stream().anyMatch(condition -> condition.applies(factory));
        }

        LOGGER.debug("Result of applying {} conditions with aggregate mode {}: {} (negation: {})", this.conditions.size(),
                this.aggregateMode, baseApplies, this.negate);

        return baseApplies != this.negate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean applies(final BeanDefinitionRegistry registry)
    {
        boolean baseApplies = false;

        if (this.aggregateMode == AggregateMode.AND)
        {
            baseApplies = this.conditions.stream().allMatch(condition -> condition.applies(registry));
        }
        else
        {
            baseApplies = this.conditions.stream().anyMatch(condition -> condition.applies(registry));
        }

        LOGGER.debug("Result of applying {} conditions with aggregate mode {}: {} (negation: {})", this.conditions.size(),
                this.aggregateMode, baseApplies, this.negate);

        return baseApplies != this.negate;
    }
}
