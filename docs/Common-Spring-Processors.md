## Common Spring Post Processors

In order to customise Alfresco Content Services or Share, it may sometimes be necessary to hook into, override or otherwise customise Alfresco beans, or even selectively have only a specific sub-set of custom beans active, e.g. depending on the specific Alfresco version. Such changes should generally not be hard-configured via Spring XML, as bean structures / requirements for customisation may differ between Alfresco versions, making modules with hard Spring XML overrides difficult to support across multiple Alfresco versions. Spring post processors offer a means to have logic-based modification of beans which may either be Alfresco default beans or some pre-configured basic beans that may only need small version-dependent adjustments. The Common sub-module of this project provides a set of reusable, generic post processor implementations for common bean definitions / customisations.
All classes for this can be found in the `de.acosix.alfresco.utility.common.spring` and its sub-packages.

### Types of Post Processors

- Implementation class altering (`ImplementationClassReplacingBeanFactoryPostProcessor` + `ImplementationClassReplacingBeanDefinitionRegistryPostProcessor`)
- Property altering (`PropertyAlteringBeanFactoryPostProcessor` and `PropertyAlteringBeanDefinitionRegistryPostProcessor`)
    - single + multi-valued (list, set, map) properties
    - simple values and bean references
    - support merge with pre-defined and/or parent bean-defined multi-valued property value
    - support index-based insertion (list property value)
- Parent altering (`BeanParentAlteringBeanFactoryPostProcessor` and `BeanParentAlteringBeanDefinitionRegistryPostProcessor`)
- Bean de-abstractifying (`BeanDeAbstractifyingBeanFactoryPostProcessor` and `BeanDeAbstractifyingBeanDefinitionRegistryPostProcessor`), useful e.g. to have selectively activateable features where the bean is not even instantiated unless enabled (compared to most Alfresco enablement flags which are only checked way after instantiation)
- Extensive bean emitter / modifier using properties-based configuration (`BeanDefinitionFromPropertiesPostProcessor`), useful to process extensive customisations / dynamic bean definitions via one instance, simplyfing configuration and allowing lay-people to perform changes without the hassle of XML

#### Common configuration

The following applies to all post-processors listed above:

- Enablement governed via `enabled`, `enabledPropertyKey` or `enabledPropertyKeys` property - the former takes the effective value while the latter takes the name(s) of properties to look up (via required `propertiesSource` bean reference) for the actual value (`enabledPropertyKeys` acts as AND-join of multiple values) - see section about difference between bean factory and definition registry post processors
- Optional `dependsOn` list of processors that must have been run before this particular instance
- Optional `condition` to make processor conditional on specific, testable pre-conditions - see section about conditional processing

The following applies to all post-processors listed above, except for the `BeanDefinitionFromPropertiesPostProcessor`:

- Either `targetBeanName` (preferred) or `targetBeanNamePattern` (fallback; regex) to specify bean to be modified
- Optional `failIfTargetBeanMissing` if `targetBeanName` is specified (startup terminates if enabled and target bean has not been defined)

#### Specific configuration

- Implementation class altering
    - Mandatory `replacementClassName` property (warning emitted if missing, but startup continues)
    - Optional `originalClassName` as simple pre-condition check (info if mismatch, but startup continues)
- Property altering
    - Mandatory `propertyName`
    - Either `value`, `beanReferenceName`, `valueList`, `beanReferenceNameList`, `valueSet`, `beanReferenceNameSet`, `valueMap` or `beanReferenceNameMap` as specific value of property to set / add
    - Optional `merge` / `mergeParent` to control merge behaviour for list, set or map-based properties - `mergeParent` triggers the Spring native merge handling, whereas `merge` ensures any existing value on the target bean is retained and new values are added to it (see next)
    - Optional `addAtIndex` / `addAsFirst` to control insertion behaviour for list-based properties
    - Optional `expectedClassName` as simple pre-condition check (info if mismatch, but startup continues)
- Parent altering
    - Optional `parentBeanName` (if not set, any parent is effectively removed)

#### Properties-based Bean Definition Emitter

TBD

### Conditional Processing

The `BeanDefinitionPostProcessorCondition` provides an abstraction for post processor activation conditions that need to be checked at runtime. This can be checks whether specific beans are defined in the default Alfresco application context or are of particular types, or anything else that can be expressed in logic, and may even involve checking of Alfresco edition / version descriptor information.

The following default conditions are currently provided:

- `BeanDefinedCondition` checks whether a bean denoted via `beanName` is defined in the application context
- `BeanPropertySetCondition` checks whether the property denoted via `propertyName` is set to a non-null value on a bean denoted via `beanName`
- `BeanTypeMatchCondition` checks whether a bean denoted via `beanName` matches `expectedTypeName`, using `typeMatchMode` (either `INSTANCE_OF` or `CLASS_MATCH` - the latter does not require `expectedTypeName` to exist as a class on the classpath)
- `AggregateCondition` grouping multiple granular `conditions` via a specified `aggregateMode` (`AND` or `OR`)

All default conditions can be inverted via `negate`.

### Bean Factory vs. Bean Definition Registry Post Processor

Since some Alfresco beans implement interfaces specific to the Spring dependency management framework, they may be handled as part of the overall Spring lifecycle. A prominent case of this are any beans which extends from the `AbstractLifecycleBean` or implement the `ApplicationListener` interface. Such beans may be eagerly instantiated and bound, even before any `BeanFactoryPostProcessor` has had a chance to be instantiated and modify bean definitions. In such a case, `BeanDefinitionRegistryPostProcessor` variants of the same post-processor may be used to handle the specific use case. But the latter should be used sparingly, as a significant drawback of that interface is the fact that at the point registry post processors are handled, configuration properties (e.g. from `alfresco-global.properties`) have not yet been loaded and Spring placeholders (e.g. `${alfresco.config.property}`) have not yet been resolved, so configuration is handled differently.

The following highlights the difference by contrasting a simple implementation class altering post processor:

```xml
<bean class="de.acosix.alfresco.utility.common.spring.ImplementationClassReplacingBeanFactoryPostProcessor">
    <!-- Spring placeholder can be used and will be transparently resolved from configured Spring resolvers + property sources -->
    <property name="enabled" value="${acosix-utility.email.inboundSMTP.subsystemEnhancement.enabled}" />

    <property name="targetBeanName" value="InboundSMTP" />
    <property name="originalClassName" value="org.alfresco.repo.management.subsystems.ChildApplicationContextFactory" />
    <property name="replacementClassName" value="de.acosix.alfresco.utility.repo.subsystems.SubsystemChildApplicationContextFactory" />
</bean>

<bean class="de.acosix.alfresco.utility.common.spring.ImplementationClassReplacingBeanDefinitionRegistryPostProcessor">
    <!-- "enabled" can not specified directly, rather the value of the key + reference to the properties bean must be set -->
    <!-- by referencing the properties bean, this forces the configuration properties to be eagerly loaded into the bean, but even then any placeholders are not yet resolved and need to be looked up by the implementation via an explicit getProperty(String) -->
    <property name="enabledPropertyKey" value="acosix-utility.email.inboundSMTP.subsystemEnhancement.enabled" />
    <property name="propertiesSource" ref="global-properties" />

    <property name="targetBeanName" value="InboundSMTP" />
    <property name="originalClassName" value="org.alfresco.repo.management.subsystems.ChildApplicationContextFactory" />
    <property name="replacementClassName" value="de.acosix.alfresco.utility.repo.subsystems.SubsystemChildApplicationContextFactory" />
</bean>
```