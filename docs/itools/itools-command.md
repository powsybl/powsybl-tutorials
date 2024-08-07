---
layout: default
---

# Create an iTools command in Java
In this tutorial, you will learn how to create a simple `iTools` command in Java.

The `iTools` script is designed to be easily extended with new commands that would be added to the set of
available commands, providing users with new command line functionalities.

In order to create a new `iTools` command:
1. Create a new maven project and add all the required dependencies.
2. Implement the `com.powsybl.tools.Tool` interface.
3. Compile your project and add the jar to your powsybl installation.

In the following sections we will see how, following these steps, you can implement a new `iTools` command to display
how many lines there are in a network.

The complete example described in this tutorial is available on GitHub:
```shell
$> git clone https://github.com/powsybl/powsybl-tutorials.git
$> cd powsybl-tutorials/count-network-lines
$> mvn package
``` 

* TOC
{:toc}

## Maven dependencies

After creating a new Maven project, you need to add the necessary dependencies to your `pom.xml` file. 

Start by adding the powsybl-dependencies module that ensures compatibility between the different PowSyBl artifacts.

```xml
<dependencyManagement>
<dependencies>
    <dependency>
        <groupId>com.powsybl</groupId>
        <artifactId>powsybl-dependencies</artifactId>
        <version>2023.0.1</version>
        <type>pom</type>
        <scope>import</scope>
    </dependency>
</dependencies>
</dependencyManagement>
```

The required dependencies to implement a new `iTools` command are the following:
- Google Auto Service to declare your new tool as a plugin
- The Powsybl tools module which contains the base interfaces for all `iTools` commands

```xml
<dependency>
    <groupId>com.google.auto.service</groupId>
    <artifactId>auto-service</artifactId>
    <version>1.0-rc2</version>
</dependency>
<dependency>
    <groupId>com.powsybl</groupId>
    <artifactId>powsybl-tools</artifactId>
</dependency>
```

In your project you also need to add the other dependencies required by your command business logic implementation, e.g.
to implement the `iTools` command displaying the number of lines of a network, you would have to add the following
dependency to get the IIDM API, needed to import IIDM networks:

```xml
<dependency>
    <groupId>com.powsybl</groupId>
    <artifactId>powsybl-iidm-api</artifactId>
</dependency>
```

## Implement the Tool interface

To create a new `iTools` command, you need to implement the `com.powsybl.tools.Tool` interface. Following is a sample
class, where you will put the code to display the number of lines of an IIDM network.

```java
import com.google.auto.service.AutoService;

import org.apache.commons.cli.CommandLine;

import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import com.powsybl.tools.ToolRunningContext;

@AutoService(Tool.class)
public class CountNetworkLinesTool implements Tool {

    @Override
    public Command getCommand() {
        return null;
    }

    @Override
    public void run(CommandLine line, ToolRunningContext context) {
    }
}
```

You have to declare the class as a service implementation, using the `@Autoservice` annotation. This allows you to
have the new command automatically added to the list of available `iTools` commands, and to be able to run it (see last
section).

The methods of the `Tool` interface to override in your class are:
- the `getCommand` method, that returns the declaration of your command
- the `run` method, in charge of running your command

### Implementing the `getCommand` method
The `getCommand` method returns an instance of the `com.powsybl.tools.Command` interface. This interface declares your
command, defining its name, its description and a theme. The theme is used to group the commands by category. Please read this documentation [page](../../user/itools/index.md#available-commands) to discover the existing
themes. In our tutorial, we chose to create a new theme, called `Network`.
                      
```java
    private static final String CASE_FILE = "case-file";

    @Override
    public Command getCommand() {
        return new Command() {

            @Override
            public String getName() {
                return "count-network-lines";
            }

            @Override
            public String getTheme() {
                return "Network";
            }

            @Override
            public String getDescription() {
                return "Count network lines";
            }

            @Override
            public Options getOptions() {
                Options options = new Options();
                options.addOption(Option.builder().longOpt(CASE_FILE)
                        .desc("the case path")
                        .hasArg()
                        .argName("FILE")
                        .required()
                        .build());             
                return options;
            }

            @Override
            public String getUsageFooter() {
                return null;
            }
        };
    }
```

The `Command` class also defines your command options (input parameters), if they are required or optional and if they
need an argument or not. The only option defined in our sample class, `case-file`, allows the user to specify the network
file to analyze. This option is required and has an argument named `FILE` to get the input case file:
```java
    options.addOption(Option.builder().longOpt(CASE_FILE)
            .desc("the case path")
            .hasArg()
            .argName("FILE")
            .required()
            .build());
```
Read the [commons-cli](https://www.javadoc.io/doc/commons-cli/commons-cli/) documentation page
to learn more.

### Implementing the `run` method
The `run` method is in charge of running your command, implementing your business logic. This methods has two parameters:
```java
    @Override
    public void run(CommandLine line, ToolRunningContext context) {
    }
```

The `line` parameter gives you access to the input options provided by the user through the command line. In our example,
we use it to read the path to the input network file.

The `context` parameter provides you with some context objects, such as an `OutputStream` object allowing you to print some
information in the console, a `ComputationManager` object sometimes required to run computations or a `FileSystem`
object for accessing the local file system (see [ToolRunningContext](https://www.javadoc.io/doc/powsybl-core/powsybl-core/)
for more information).

In our tutorial, we load the input case file to get an IIDM network instance and assert that the network was loaded
successfully:
```java
    Path caseFile = context.getFileSystem().getPath(line.getOptionValue(CASE_FILE));
    context.getOutputStream().println("Loading network '" + caseFile + "'");
    Network network = Network.read(caseFile);
    if (network == null) {
        throw new PowsyblException("Case '" + caseFile + "' not found");
    }
```

Then we get the number of lines in this network and print a message in the console:
```java
    int lineCount = network.getLineCount();
    context.getOutputStream().println("Network contains '" + lineCount + "' lines");
```

## Update your installation with the new command

Run the following command to build your project jar:
```bash
$> mvn package
```

The generated jar will be located under the target folder of your project. Copy the generated jar to the `share/java`
folder of your Powsybl distribution (you might need to copy in this directory other dependencies jars, specific to your
new command).

Then run `iTools` to check if your command is available:
```shell
$> cd <POWSYBL_HOME>/bin
$> ./itools count-network-lines --help
usage: itools [OPTIONS] count-network-lines --case-file <FILE> [--help]

Available options are:
    --config-name <CONFIG_NAME>   Override configuration file name

Available arguments are:
    --case-file <FILE>   the case path
    --help               display the help and quit
```

You can run the new command, using the following command:
```shell
$> cd <POWSYBL_HOME>/bin
$> ./itools count-network-lines --case-file ~/network.xiidm
Loading network '~/network.xiidm'
Network contains '2' lines
```

## Going further
- [Bundle an iTools package](itools-packager.md): Learn how to use the `itools-packager` maven plugin