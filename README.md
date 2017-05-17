[![Build Status](https://travis-ci.org/Acosix/alfresco-utility.svg?branch=master)](https://travis-ci.org/Acosix/alfresco-utility)

# About
This project defines sets of utility constructs for Repository- or Share-tier modules as well as cross-cutting utilities. The resulting technical modules are primarily used to avoid redundancy in Acosix GmbH modules, as well as abstracting and consolidating similar functionality without tying it to specific functional constructs.

## Compatbility

This module is built to be compatible with Alfresco 5.0d and above. It may be used on either Community or Enterprise Edition.

## Features

### Common
- Collection of Spring bean factory / bean definition registry post processors for customising out-of-the-box Alfresco Spring beans without overriding / copy&pasting Spring XML
- Thread-safe SSL socket factory (default Alfresco SSL socket factory uses static, shared state)
- Enhanced default declarative web script which respects the web script format during lookup of JS customisations (e.g. site.put.json.js will look for site.put.json.js and site.put.js customisation files instead of only site.put.js) 

### Repository-tier
- Enhanced subsystem factory/manager classes that allow reverse-lookup of the subsystem instance ID from its application context as well as access to the resolved locations of *.properties files
- Custom subsystem factory/manager variant that use a custom class loader for subsystems so that they can load custom library JARs or different versions from JARs already included in Alfresco 
- Subsystem properties factory bean allowing subsystem properties to be exposed to other components as regular java.util.Properties objects
- Enhanced web script container capable of handling [web script extensibility](https://www.alfresco.com/blogs/developer/2012/05/23/webscript-extensibility-on-the-alfresco-repository/) - raised via [ALF-21794](https://issues.alfresco.com/jira/browse/ALF-21794)
- Simple override to site.get/site.put/sites.post JSON FTL to allow web script extension templates to augment the site data, e.g. as basis for simpler "Edit Site" dialog customisations either in YUI or Aikau

### Share-tier
- Support for share-global.properties files to hold simple configuration key-value pairs which can be provided by modules (similarly to Repository-tier) and overriden by administrators via a share-global.properties file in the Tomcat configuration root folder (./shared/classes/) - properties provided that way are automatically exposed in Spring XML files for placeholder resolution
- Support for log4j.properties files to be provided by modules (similarly to Repository-tier) and overriden by administrators via a *-log4j.properties in the Tomcat configuration root folder (./shared/classes/alfresco/web-extension/) - raised by an Enterprise customer in 2013 via [MNT-14972](https://issues.alfresco.com/jira/browse/MNT-14972)
- Minor enhancements to Surf CSS theme handlers (clean state separation between different theme CSS tokens)
- Minor enhancements to Surf Dojo widget dependency collection (JSON instead of RegEx-parsing instead of JSON model; improved RegEx-pattern for dependencies detection in JS source files)
- Minor enhancements to Surf CSS dependency collection (JSON instead of RegEx-parsing of JSON model; improved RegEx-pattern for dependencies detection in JS source files) - effectively adding the ability to load additional CSS files via JSON model

# Maven usage

This addon is being built using the [Acosix Alfresco Maven framework](https://github.com/Acosix/alfresco-maven) and produces both AMP and installable JAR artifacts. Depending on the setup of a project that wants to include the addon, different approaches can be used to include it in the build.

## Build

This project can be build simply by executing the standard Maven build lifecycles for package, install or deploy depending on the intent for further processing. A Java Development Kit (JDK) version 8 or higher is required for the build of the master branch, while the Alfresco 4.2 branch requires Java 7.

By inheritance from the Acosix Alfresco Maven framework, this project uses the [Maven Toolchains plugin](http://maven.apache.org/plugins/maven-toolchains-plugin/) to allow potential cross-compilation against different Java versions. This is used for instance in a [separate branch to provide an Alfresco 4.2 compatible version](https://github.com/Acosix/alfresco-utility/tree/alfresco-42) of this addon. In order to build the project it is necessary to provide a basic toolchain configuration via the user specific Maven configuration home (usually ~/.m2/). That file (toolchains.xml) only needs to list the path to a compatible JDK for the Java version required by this project. The following is a sample file defining a Java 7 and 8 development kit.

```xml
<?xml version='1.0' encoding='UTF-8'?>
<toolchains xmlns="http://maven.apache.org/TOOLCHAINS/1.1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/TOOLCHAINS/1.1.0 http://maven.apache.org/xsd/toolchains-1.1.0.xsd">
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>1.8</version>
      <vendor>oracle</vendor>
    </provides>
    <configuration>
      <jdkHome>C:\Program Files\Java\jdk1.8.0_112</jdkHome>
    </configuration>
  </toolchain>
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>1.7</version>
      <vendor>oracle</vendor>
    </provides>
    <configuration>
      <jdkHome>C:\Program Files\Java\jdk1.7.0_80</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
```

## Dependency in Alfresco SDK

The simplest option to include the addon in an All-in-One project is by declaring a dependency to the installable JAR artifact. Alternatively, the AMP package may be included which typically requires additional configuration in addition to the dependency.

### Using SNAPSHOT builds

In order to use a pre-built SNAPSHOT artifact published to the Open Source Sonatype Repository Hosting site, the artifact repository may need to be added to the POM, global settings.xml or an artifact repository proxy server. The following is the XML snippet for inclusion in a POM file.

```xml
<repositories>
    <repository>
        <id>ossrh</id>
        <url>https://oss.sonatype.org/content/repositories/snapshots</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

### Repository

```xml
<!-- JAR packaging -->
<dependency>
    <groupId>de.acosix.alfresco.utility</groupId>
    <artifactId>de.acosix.alfresco.utility.common</artifactId>
    <version>1.0.2.0-SNAPSHOT</version>
    <type>jar</type>
</dependency>

<dependency>
    <groupId>de.acosix.alfresco.utility</groupId>
    <artifactId>de.acosix.alfresco.utility.repo</artifactId>
    <version>1.0.2.0-SNAPSHOT</version>
    <type>jar</type>
    <classifier>installable</classifier>
</dependency>

<!-- OR -->

<!-- AMP packaging -->
<dependency>
    <groupId>de.acosix.alfresco.utility</groupId>
    <artifactId>de.acosix.alfresco.utility.repo</artifactId>
    <version>1.0.2.0-SNAPSHOT</version>
    <type>amp</type>
</dependency>

<plugin>
    <artifactId>maven-war-plugin</artifactId>
    <configuration>
        <overlays>
            <overlay />
            <overlay>
                <groupId>${alfresco.groupId}</groupId>
                <artifactId>${alfresco.repo.artifactId}</artifactId>
                <type>war</type>
                <excludes />
            </overlay>
            <!-- other AMPs -->
            <overlay>
                <groupId>de.acosix.alfresco.utility</groupId>
                <artifactId>de.acosix.alfresco.utility.repo</artifactId>
                <type>amp</type>
            </overlay>
        </overlays>
    </configuration>
</plugin>
```

For Alfresco SDK 3 beta users:

```xml
<platformModules>
    <moduleDependency>
        <groupId>de.acosix.alfresco.utility</groupId>
        <artifactId>de.acosix.alfresco.utility.repo</artifactId>
        <version>1.0.2.0-SNAPSHOT</version>
        <type>amp</type>
    </moduleDependency>
</platformModules>
```

### Share

```xml
<!-- JAR packaging -->
<dependency>
    <groupId>de.acosix.alfresco.utility</groupId>
    <artifactId>de.acosix.alfresco.utility.common</artifactId>
    <version>1.0.2.0-SNAPSHOT</version>
    <type>jar</type>
</dependency>

<dependency>
    <groupId>de.acosix.alfresco.utility</groupId>
    <artifactId>de.acosix.alfresco.utility.share</artifactId>
    <version>1.0.2.0-SNAPSHOT</version>
    <type>jar</type>
    <classifier>installable</classifier>
</dependency>

<!-- OR -->

<!-- AMP packaging -->
<dependency>
    <groupId>de.acosix.alfresco.utility</groupId>
    <artifactId>de.acosix.alfresco.utility.share</artifactId>
    <version>1.0.2.0-SNAPSHOT</version>
    <type>amp</type>
</dependency>

<plugin>
    <artifactId>maven-war-plugin</artifactId>
    <configuration>
        <overlays>
            <overlay />
            <overlay>
                <groupId>${alfresco.groupId}</groupId>
                <artifactId>${alfresco.share.artifactId}</artifactId>
                <type>war</type>
                <excludes />
            </overlay>
            <!-- other AMPs -->
            <overlay>
                <groupId>de.acosix.alfresco.utility</groupId>
                <artifactId>de.acosix.alfresco.utility.share</artifactId>
                <type>amp</type>
            </overlay>
        </overlays>
    </configuration>
</plugin>
```

For Alfresco SDK 3 beta users:

```xml
<shareModules>
    <moduleDependency>
        <groupId>de.acosix.alfresco.utility</groupId>
        <artifactId>de.acosix.alfresco.utility.share</artifactId>
        <version>1.0.2.0-SNAPSHOT</version>
        <type>amp</type>
    </moduleDependency>
</shareModules>
```

# Other installation methods

Using Maven to build the Alfresco WAR is the **recommended** approach to install this module. As an alternative it can be installed manually.

## alfresco-mmt.jar / apply_amps

The default Alfresco installer creates folders *amps* and *amps_share* where you can place AMP files for modules which Alfresco will install when you use the apply\_amps script. Place the AMP for the *de.acosix.alfresco.utility.repo* module in the *amps* directory, *de.acosix.alfresco.utility.share* in the *amps_share* directory, and execute the script to install them. You must restart Alfresco for the installation to take effect.

Alternatively you can use the alfresco-mmt.jar to install the modules as [described in the documentation](http://docs.alfresco.com/5.1/concepts/dev-extensions-modules-management-tool.html).

## Manual "installation" using JAR files

Some addons and some other sources on the net suggest that you can install **any** addon by putting their JARs in a path like &lt;tomcat&gt;/lib, &lt;tomcat&gt;/shared or &lt;tomcat&gt;/shared/lib. This is **not** correct. Only the most trivial addons / extensions can be installed that way - "trivial" in this case means that these addons have no Java class-level dependencies on any component that Alfresco ships, e.g. addons that only consist of static resources, configuration files or web scripts using pure JavaScript / Freemarker.

The only way to manually install an addon using JARs that is **guaranteed** not to cause Java classpath issues is by dropping the JAR files directly into the &lt;tomcat&gt;/webapps/alfresco/WEB-INF/lib (Repository-tier) or &lt;tomcat&gt;/webapps/share/WEB-INF/lib (Share-tier) folders.

For this addon the following JARs need to be dropped into &lt;tomcat&gt;/webapps/alfresco/WEB-INF/lib:

 - de.acosix.alfresco.utility.common-&lt;version&gt;.jar
 - de.acosix.alfresco.utility.repo-&lt;version&gt;-installable.jar
 
For this addon the following JARs need to be dropped into &lt;tomcat&gt;/webapps/share/WEB-INF/lib:

 - de.acosix.alfresco.utility.common-&lt;version&gt;.jar
 - de.acosix.alfresco.utility.share-&lt;version&gt;-installable.jar