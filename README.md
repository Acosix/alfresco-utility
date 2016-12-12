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
- Subsystem properties factory bean allowing subsystem properties to be exposed to other components as regular java.util.Properties objects
- Enhanced web script container capable of handling [web script extensibility](https://www.alfresco.com/blogs/developer/2012/05/23/webscript-extensibility-on-the-alfresco-repository/) - raised via [ALF-21794](https://issues.alfresco.com/jira/browse/ALF-21794)

### Share-tier
- Support for share-global.properties files to hold simple configuration key-value pairs which can be provided by modules (similarily to Repository-tier) and overriden by administrators via a share-global.properties file in the Tomcat configuration root folder (./shared/classes/) - properties provided that way are automatically exposed in Spring XML files for placeholder resolution
- Support for log4j.properties files to be provided by modules (similarily to Repository-tier) and overriden by administrators via a *-log4j.properties in the Tomcat configuration root folder (./shared/classes/alfresco/web-extension/) - raised by an Enterprise customer in 2013 via [MNT-14972](https://issues.alfresco.com/jira/browse/MNT-14972)
- Minor enhancements to Surf CSS theme handlers (clean state separation between different theme CSS tokens)
- Minor enhancements to Surf Dojo widget dependency collection (JSON instead of RegEx-parsing instead of widget models; improved RegEx-pattern for dependencies detection in JS source files)

# Maven usage

This addon is being built using the [Acosix Alfresco Maven framework](https://github.com/Acosix/alfresco-maven) and produces both AMP and installable JAR artifacts. Depending on the setup of a project that wants to include the addon, different approaches can be used to include it in the build.

## Build

This project can be build simply by executing the standard Maven build lifecycles for package, install or deploy depending on the intent for further processing. A Java Development Kit (JDK) version 8 or higher is required for the build.

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
    <version>1.0.0.0</version>
    <type>jar</type>
</dependency>

<dependency>
    <groupId>de.acosix.alfresco.utility</groupId>
    <artifactId>de.acosix.alfresco.utility.repo</artifactId>
    <version>1.0.0.0</version>
    <type>jar</type>
    <classifier>installable</classifier>
</dependency>

<!-- OR -->

<!-- AMP packaging -->
<dependency>
    <groupId>de.acosix.alfresco.utility</groupId>
    <artifactId>de.acosix.alfresco.utility.repo</artifactId>
    <version>1.0.0.0</version>
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
        <version>1.0.0.0</version>
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
    <version>1.0.0.0</version>
    <type>jar</type>
</dependency>

<dependency>
    <groupId>de.acosix.alfresco.utility</groupId>
    <artifactId>de.acosix.alfresco.utility.share</artifactId>
    <version>1.0.0.0</version>
    <type>jar</type>
    <classifier>installable</classifier>
</dependency>

<!-- OR -->

<!-- AMP packaging -->
<dependency>
    <groupId>de.acosix.alfresco.utility</groupId>
    <artifactId>de.acosix.alfresco.utility.share</artifactId>
    <version>1.0.0.0</version>
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
        <version>1.0.0.0</version>
        <type>amp</type>
    </moduleDependency>
</shareModules>
```