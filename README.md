# About
This project defines sets of utility constructs for Repository- or Share-tier modules as well as cross-cutting utilities. The resulting technical modules are primarily used to avoid redundancy in Acosix GmbH modules, as well as abstracting and consolidating similar functionality without tying it to specific functional constructs.

## Compatbility

This module is built to be compatible with Alfresco 5.0d and above. It may be used on either Community or Enterprise Edition. The following **special** compatibility conditions apply:

 - Spring 5 included in Alfresco 6.2+ introduced some internal changes which may break earlier versions of this addon. Only version 1.1.0+ is fully compatible with Alfresco 6.2+.
 - ACS 7.0 has finalised the removal of ContentService-based (legacy) transformers which breaks earlier versions of this addon. Only version 1.3.0+ is fully compatible with Aflresco 7.0+.

## Features

### Common
- Collection of [Spring bean factory / bean definition registry post processors](./docs/Common-Spring-Processors.md) for customising out-of-the-box Alfresco Spring beans without overriding / copy&pasting Spring XML
- Thread-safe SSL socket factory (default Alfresco SSL socket factory uses static, shared state)
- Enhanced default declarative web script which respects the web script format during lookup of JS customisations (e.g. site.put.json.js will look for site.put.json.js and site.put.js customisation files instead of only site.put.js) 

### Repository-tier
- Enhanced subsystem factory/manager classes that allow reverse-lookup of the subsystem instance ID from its application context as well as access to the resolved locations of *.properties files
- Custom subsystem factory/manager variant that use a custom class loader for subsystems so that they can load custom library JARs or different versions from JARs already included in Alfresco 
- Subsystem properties factory bean allowing subsystem properties to be exposed to other components as regular java.util.Properties objects
- Enhanced web script container capable of handling [web script extensibility](https://www.alfresco.com/blogs/developer/2012/05/23/webscript-extensibility-on-the-alfresco-repository/) - raised via [ALF-21794](https://issues.alfresco.com/jira/browse/ALF-21794)
- Simple override to site.get/site.put/sites.post JSON FTL to allow web script extension templates to augment the site data, e.g. as basis for simpler "Edit Site" dialog customisations either in YUI or Aikau
- [Common utility functions](./core/repository/src/main/java/de/acosix/alfresco/utility/repo/job/JobUtilities.java) for Quartz job processing, i.e. running a job with a lock and an optional transaction
- Quartz 1.x/2.x API incompatibility abstraction utilities, allowing addons to be compatible against Alfresco 5.x and 6.x despite binary incompatibilities in Quartz API (requires use of [generic job interface](./core/repository/src/main/java/de/acosix/alfresco/utility/repo/job/GenericJob.java) + [job details factory](./core/repository/src/main/java/de/acosix/alfresco/utility/repo/job/GenericJobDetailsFactoryBean.java) instead of default Alfresco + Quartz classes; utility functions from previous bullet point provide means to extract config from job context without compiling against / using Quartz API)
- Basic batch process work provider handling cm:people nodes, using transactional metadata queries (TMQ) combined with metadata-based pagination for efficient loading, specifically for larger user bases
- Transactionally-safe, full XPath-supporting XPathNodeLocator (using selectNodes() API instead of index query)
- (opt-in) Improved inbound SMTP handling allowing for the full, original RFC 822 email to be processed by handlers (not just some of its parts)
- (opt-in) Improved inbound SMTP folder handler, storing the RFC 822 email as received, and optional extracting attachments (as siblings or children of the email) - includes simple meta model
- site bootstrap utilities / template to support flexible site bootstrap during Repository initialisation without hacky call to Share web scripts
- chaining remote user mapper, in case more than one Authentication subsystem is capable of determining a user from an external authentication system (useful e.g. to combine Alfresco Identity Services with certificate based client authentication / legacy CAS)

### Share-tier
- Support for share-global.properties files to hold simple configuration key-value pairs which can be provided by modules (similarly to Repository-tier) and overriden by administrators via a share-global.properties file in the Tomcat configuration root folder (./shared/classes/) - properties provided that way are automatically exposed in Spring XML files for placeholder resolution
- Support for log4j.properties files to be provided by modules (similarly to Repository-tier) and overriden by administrators via a *-log4j.properties in the Tomcat configuration root folder (./shared/classes/alfresco/web-extension/) - raised by an Enterprise customer in 2013 via [MNT-14972](https://issues.alfresco.com/jira/browse/MNT-14972)
- Minor enhancements to Surf CSS theme handlers (clean state separation between different theme CSS tokens)
- Minor enhancements to Surf Dojo widget dependency collection (JSON instead of RegEx-parsing instead of JSON model; improved RegEx-pattern for dependencies detection in JS source files)
- Minor enhancements to Surf CSS dependency collection (JSON instead of RegEx-parsing of JSON model; improved RegEx-pattern for dependencies detection in JS source files) - effectively adding the ability to load additional CSS files via JSON model
- Enhanced local web script container addressing an [issue](https://issues.alfresco.com/jira/browse/ALF-21949) with the Surf extensibility handling interfering with AbstractWebScript implementations that directly stream a response to the client
- Extensible, structured code editor for d:content property form fields, based on the Ace editor (from Cloud9 IDE) - HTML variant provided as a base reference

# Maven usage

This addon is being built using the [Acosix Alfresco Maven framework](https://github.com/Acosix/alfresco-maven) and produces both AMP and installable JAR artifacts. Depending on the setup of a project that wants to include the addon, different approaches can be used to include it in the build.

## Build

This project can be built simply by executing the standard Maven build lifecycles for package, install or deploy depending on the intent for further processing. A Java Development Kit (JDK) version 8 or higher is required for the build of the master branch, with some sub-modules requiring a JDK 17 for cross-compilation and API version bridging purposes.

By inheritance from the Acosix Alfresco Maven framework, this project uses the [Maven Toolchains plugin](http://maven.apache.org/plugins/maven-toolchains-plugin/) to allow potential cross-compilation against different Java versions.In order to build the project it is necessary to provide a basic toolchain configuration via the user specific Maven configuration home (usually ~/.m2/). That file (toolchains.xml) only needs to list the path to a compatible JDK for the Java version required by this project. The following is a sample file defining a Java 8 and 17 development kit.

```xml
<?xml version='1.0' encoding='UTF-8'?>
<toolchains xmlns="http://maven.apache.org/TOOLCHAINS/1.1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/TOOLCHAINS/1.1.0 http://maven.apache.org/xsd/toolchains-1.1.0.xsd">
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>8</version>
      <vendor>eclipse</vendor>
      <id>jdk1.8</id>
    </provides>
    <configuration>
      <jdkHome>C:\Program Files\Eclipse Adoptium\jdk-8.0.345.1-hotspot</jdkHome>
    </configuration>
  </toolchain>
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>17</version>
      <vendor>eclipse</vendor>
      <id>jdk17</id>
    </provides>
    <configuration>
      <jdkHome>C:\Program Files\Eclipse Adoptium\jdk-17.0.4.101-hotspot</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
```

## Installation

The mode of installation varies noticeably based on version of Alfresco SDK, personal preference in packaging, deployment method and/or other aspects. It is therefore difficult to compile a comprehensive guide on how to install the addon in each of the possible scenarios.

This addon produces both installable JAR artifacts as well as more traditional AMP packages. Releases and Snapshots are published on Maven Central. In order to use Snapshots from Maven Central, an explicit repository has to be added in POMs since Maven by default only uses Maven Central to lookup release artifacts.

```xml
<repositories>
    <repository>
        <id>central-snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

This addon consists of two layers of modules - a **core** layer with various utilities but without any (more or less aggressive) patches / changes to default Alfresco, and the **full** layer with all the utilities, patches and changes.

The following artifact coordinates are relevant for AMP based installations:

- de.acosix.alfresco.utility:de.acosix.alfresco.utility.core.repo:&lt;version&gt;:amp - for installing the **core** layer on the Repository tier alone
- de.acosix.alfresco.utility:de.acosix.alfresco.utility.repo:&lt;version&gt;:amp - for installing the full addon on the Repository tier (includes the **core** layer)
- de.acosix.alfresco.utility:de.acosix.alfresco.utility.core.share:&lt;version&gt;:amp - for installing the **core** layer on the Share tier alone
- de.acosix.alfresco.utility:de.acosix.alfresco.utility.share:&lt;version&gt;:amp - for installing the full addon on the Share tier (includes the **core** layer)

The following artifact coordinates are relevant for JAR based installations:

- **core** layer only
    - de.acosix.alfresco.utility:de.acosix.alfresco.utility.common:&lt;version&gt;:jar - for Repository AND Share tiers
    - de.acosix.alfresco.utility:de.acosix.alfresco.utility.core.repo.quartz1:&lt;version&gt;:jar
    - de.acosix.alfresco.utility:de.acosix.alfresco.utility.core.repo.quartz2:&lt;version&gt;:jar
    - de.acosix.alfresco.utility:de.acosix.alfresco.utility.core.repo:&lt;version&gt;:jar:installable
    - de.acosix.alfresco.utility:de.acosix.alfresco.utility.core.share:&lt;version&gt;:jar:installable
- additional to form a full install
    - de.acosix.alfresco.utility:de.acosix.alfresco.utility.repo:&lt;version&gt;:jar:installable
    - de.acosix.alfresco.utility:de.acosix.alfresco.utility.share:&lt;version&gt;:jar:installable

