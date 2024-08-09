---
layout: default
---

# Bundle an iTools package with maven
This tutorial shows you how to bundle an `iTools` package using the `itools-packager` maven plugin.

## What will you build?
You will create an `iTools` redistribuable package with only a single `plugins-info` command. You will learn to how to configure the `itools-packager` plugin to override the default configurations.

## What will you need?
- About 30 minutes
- A favorite text editor or IDE
- JDK 1.8 or later
- You can also import the code straight into your IDE:
    - [IntelliJ IDEA](../intellij.md)

## How to complete this tutorial?
Like most tutorials, you can start from scratch and complete each step or you can bypass basic setup steps that are already familiar to you. Either way, you end up with working code.

To start from scratch, move on to [Create a new project](#create-a-new-project-from-scratch).

To skip the basics, do the following:
- Download and unzip the [source repository](https://github.com/powsybl/powsybl-tutorials), or clone it using Git: `git clone https://github.com/powsybl/powsybl-tutorials`.
- Change directory to `itools-packager/initial`
- Jump ahead to [Configure itools-packager](#configure-itools-packager)

When you finish, you can check your results against the code in `itools-packager/complete`.

## Create a new project from scratch
Create a new Maven's `pom.xml` file with the following content:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.powsybl.tutorials</groupId>
    <artifactId>powsybl-itools-packager</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <properties>
        <powsybl-dependencies.version>2023.0.1</powsybl-dependencies.version>
        <slf4j.version>1.7.22</slf4j.version>
        <logback.version>1.2.9</logback.version>
    </properties>

</project>
```
This will create a Maven artifact `com.powsybl.tutorials:powsybl-itools-packager:1.0.0` of type pom.

## Configure itools-packager
In the `pom.xml`, add the following lines to enable the `itools-packager` maven plugin during the compilation.
```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.powsybl</groupId>
            <artifactId>powsybl-itools-packager-maven-plugin</artifactId>
            <version>3.3.0</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>package-zip</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

Running `mvn package` command will run the `package-zip` goal of the plugin with the default configuration and produce:
- an `iTools` distribution in the `target/powsybl-itools-packager-1.0.0` directory
- a `powsybl-itools-packager-1.0.0.zip` redistribuable package

**Content of the `target` folder:**
```
$> ls -l target
total 12
drwxrwxr-x 6 mathbagu mathbagu 4096  7 mai   18:33 powsybl-itools-packager-1.0.0
-rw-rw-r-- 1 mathbagu mathbagu 5311  7 mai   18:33 powsybl-itools-packager-1.0.0.zip
```

**Content of the `target/powsybl-itools-packager-1.0.0` folder**
```
$> tree target/powsybl-itools-packager-1.0.0
target/powsybl-itools-packager-1.0.0
├── bin
│   ├── itools
│   ├── itools.bat
│   └── powsyblsh
├── etc
│   ├── itools.conf
│   ├── logback-itools.xml
│   └── logback-powsyblsh.xml
├── lib
└── share
    └── java

5 directories, 6 files
```

Then we'll add a few **required** maven dependencies:
- `com.powsybl:powsybl-tools`: to provide the iTools main java class
- `com.powsybl:powsybl-config-classic`: to provide a way to read the configuration
- `com.powsybl:powsybl-computation-local`: to run the simulations locally
- `ch.qos.logback:logback-classic`: to provide a `slf4j` implementation
- `org.slf4j:log4j-over-slf4j`: to create a bridge between `log4j` and `slf4j` 

**Note:** PowSyBl uses [slf4j](http://www.slf4j.org/) as a facade for various logging framework, but some APIs we use in PowSyBl use [log4j](https://logging.apache.org/log4j), which is not compatible with slf4j, make it necessary to create a bridge between the two logging system.

Add the following dependencies to the `pom.xml` file:
```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.powsybl</groupId>
      <artifactId>powsybl-dependencies</artifactId>
      <version>${powsybl-dependencies.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>com.powsybl</groupId>
        <artifactId>powsybl-tools</artifactId>
    </dependency>
    <dependency>
        <groupId>com.powsybl</groupId>
        <artifactId>powsybl-config-classic</artifactId>
    </dependency>
    <dependency>
        <groupId>com.powsybl</groupId>
        <artifactId>powsybl-computation-local</artifactId>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>${logback.version}</version>
    </dependency>
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>log4j-over-slf4j</artifactId>
        <version>${slf4j.version}</version>
    </dependency>
</dependencies>
```

## Bundle the package
Now that the dependencies have been added, run again the `mvn package` command to update the distribution. The `share/java` has been completed with the dependencies we add and their transitive dependencies, making the package works.
```
$> ./target/powsybl-itools-packager-1.0.0/bin/itools 
usage: itools [OPTIONS] COMMAND [ARGS]

Available options are:
    --config-name <CONFIG_NAME>   Override configuration file name

Available commands are:

Misc:
    plugins-info                             List the available plugins
```

You can also run the `plugins-info` commands:
```
$> ./target/powsybl-itools-packager-1.0.0/bin/itools plugins-info
Plugins:
+------------------+----------------------+
| Plugin type name | Available plugin IDs |
+------------------+----------------------+
```

## Play with itools-packager
The `itools-packager` plugin can be configured to change the distribution or the archive name, to change the settings in `itools.conf` or to provide a default PowSyBl configuration.

### Configure the packaging
Add the following `configuration` section to the `itools-packager` plugin.
```xml
<configuration>
    <packageName>itools-1.0.0</packageName>
    <archiveName>itools</archiveName>
</configuration>
```
Run the `mvn clean package` command to generate the new distribution and look to content of the `target` folder:
```
$> ls -l target
total 12772
drwxrwxr-x 6 mathbagu mathbagu     4096  7 mai   19:46 itools-1.0.0
-rw-rw-r-- 1 mathbagu mathbagu 13071022  7 mai   19:46 itools.zip
```

As you can see, the `archiveName` property defines the name of the redistribuable package, whereas the `packageName` property defines the prefix directory.

### Configure the logs
By default, the logger is configured to log only error messages, but it's possible to provide your own configuration file. Create a new `logback-itools.xml` with the following content:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <Pattern>%d{yyyy-MM-dd_HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</Pattern>
        </encoder>
    </appender>
    <root level="INFO">
        <appender-ref ref="STDOUT" />
    </root>
</configuration>
```  

Then add the new `copyToEtc` section to the plugin configuration:
```xml
<configuration>
    <packageName>itools-1.0.0</packageName>
    <archiveName>itools</archiveName>
    <copyToEtc>
        <files>
            <file>logback-itools.xml</file>
        </files>
    </copyToEtc>
</configuration>
```

Run the `mvn package` command to refresh the package, and run the `iTools` script to see the configuration changes:
```
$> ./target/itools-1.0.0/bin/itools
2020-05-07_19:46:30.174 [main] INFO  c.p.commons.config.PlatformConfig - Using platform configuration provider classic
2020-05-07_19:46:30.231 [main] INFO  c.p.commons.config.PlatformConfig - Platform configuration defined by YAML file /home/baguemat/.itools/config.yml
2020-05-07_19:46:30.289 [main] INFO  c.p.commons.config.PlatformConfig - Platform configuration defined by .properties files of directory /home/baguemat/Mathieu/projects/powsybl/sources/powsybl-tutorials/itools-packager/complete/target/itools-1.0.0/etc
2020-05-07_19:46:30.301 [main] INFO  c.p.c.DefaultComputationManagerConfig - DefaultComputationManagerConfig(shortTimeExecutionComputationManagerFactoryClass=com.powsybl.computation.local.LocalComputationManagerFactory, longTimeExecutionComputationManagerFactoryClass=com.powsybl.computation.local.LocalComputationManagerFactory)
usage: itools [OPTIONS] COMMAND [ARGS]

Available options are:
    --config-name <CONFIG_NAME>   Override configuration file name

Available commands are:

Misc:
    plugins-info                             List the available plugins

``` 

## Summary
You have learnt to create a redistribuable `iTools` package, and to configure the `itools-packager` plugin. Refer to the [iTools manual](inv:powsyblcore:std:doc#user/itools/index.html#available-commands) to know the list of available commands. 

## Going further
The following links could also be useful:
- [iTools manual](inv:powsyblcore:std:doc#user/itools/index): Learn how to use `iTools`, and which commands are available
- [Create an iTools command](itools-command.md): Learn how to create your own `iTools` command
