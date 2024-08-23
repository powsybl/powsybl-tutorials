---
layout: default
---

# Write the Java code to perform power flows
This tutorial shows you how to write Java code to perform load flow calculations on a network, not only in its current 
state, but also in an N-1 state by applying a contingency. You'll see how to configure PowSyBl by using a YML file and 
overwriting it with a JSON file, how to provide the input network, and how to output the load flow results to the terminal.

## What will you build?
The tutorial can be expressed in a short and simple workflow: all the input data is stored in an XIIDM file. This file 
is imported with the IIDM importer. Then, a load flow simulator is launched to get flows on all nodes. In this tutorial,
the simulator is OpenLoadflow, but it could be any other load flow simulator, as long as the API contract is followed. 
A contingency is created, and finally, the flows are recalculated to get the final state.  

![Workflow](./img/loadflow/Workflow.svg){width="75%" .center-image}

## What will you need?
- About 1/2 hour
- A favorite text editor or IDE
- JDK 1.17 or later
- You can also import the code straight into your IDE:
    - [IntelliJ IDEA](intellij.md)
- A network to work with, it can either be your own network (see supported formats [here](inv:powsyblcore:*:*#grid_exchange_formats/index)) or the example network from
this tutorial available on GitHub [here](https://github.com/powsybl/powsybl-tutorials/tree/main/loadflow/src/main/resources). 

## How to complete this tutorial?
To complete this tutorial, you can start from scratch and complete each step to write your own code. You can also 
download or clone the sources directly from the [tutorial repository](https://github.com/powsybl/powsybl-tutorials) 
on GitHub and run the completed code.

## Create a new project from scratch
Create a new Maven `pom.xml` file in a directory called `loadflow` with the following content:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.powsybl</groupId>
        <artifactId>powsybl-parent</artifactId>
        <version>8</version>
        <relativePath/>
    </parent>

    <artifactId>powsybl-loadflow</artifactId>

    <properties>
        <maven.exec.version>1.6.0</maven.exec.version>
        <powsybl-dependencies.version>2024.2.0</powsybl-dependencies.version>
    </properties>
</project>
```

## Configure the maven pom file

First, in the `pom.xml`, add the following lines in the `<properties>` section to make it possible to run the future 
main class through maven:
```xml
<exec.cleanupDaemonThreads>false</exec.cleanupDaemonThreads>
<exec.mainClass>powsybl.tutorials.loadflow.LoadflowTutorial</exec.mainClass>
```
When you'll have created the `LoadflowTutorial` class and its main function, you'll then be able to
execute your code through:
```
$> mvn clean package exec:exec
```

Also, configure the pom file to use a configuration file taken in the classpath, instead of the one
that is global to your system:
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>exec-maven-plugin</artifactId>
            <version>1.6.0</version>
            <configuration>
                <systemProperties>
                    <systemProperty>
                        <key>powsybl.config.dirs</key>
                        <value>${project.build.directory}/classes</value>
                    </systemProperty>
                </systemProperties>
            </configuration>
        </plugin>
    </plugins>
</build>
```

Now, we'll add a few **required** maven dependencies:
- `com.powsybl:powsybl-config-classic`: to provide a way to read the configuration
- `org.slf4j:slf4j-simple`: to provide an implementation of `slf4j`.
- `com.powsybl:powsybl-open-loadflow` to provide an implementation for the load flow calculation.
- `com.powsybl:powsybl-iidm-impl` to load and work with networks.

**Note:** PowSyBl uses [slf4j](http://www.slf4j.org/) as a facade for various logging frameworks, but some of the APIs 
we use in PowSyBl use [log4j](https://logging.apache.org/log4j), which is not compatible with slf4j, so it is necessary 
to create a bridge between the two logging systems.

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
            <artifactId>powsybl-config-classic</artifactId>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
            <version>${slf4j.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.powsybl</groupId>
            <artifactId>powsybl-open-loadflow</artifactId>
        </dependency>
        <dependency>
            <groupId>com.powsybl</groupId>
            <artifactId>powsybl-iidm-impl</artifactId>
        </dependency>
</dependencies>
```

## Configure PowSyBl
We have configured this tutorial to use a locally defined `config.yml` file.
Now you need to create this file at the location `loadflow/src/main/resources`.
Start the configuration by writing:
```yaml
load-flow:
  default-impl-name: "OpenLoadFlow"
```
In this way, PowSyBl will be set to use the OpenLoadflow implementation for the power flow.

## Import the network from an XML IIDM file
Now we are going to write the main Java class of this tutorial. In `loadflow/src/main/java/com/powsybl/tutorials/loadflow/`,
create a class `LoadFlowTutorial.java`. Remember to specify the package: 
```java
package com.powsybl.tutorials.loadflow;
```

First, create a logger outside your `main` method but inside your `LoadFlowTutorial` class. We will use it to display
information about the objects we handle.
```java
private static final Logger LOGGER = LoggerFactory.getLogger(LoadflowTutorial.class);
```

In this tutorial, the network that is considered is quite simple and consists of two parallel lines, with a generator 
on the left side and a load on the right side. 
The load consumes 600 MW and the generator produces 606.5 MW. 

![Initial simple network](./img/loadflow/Network_Simple_Initial.svg){width="50%" .center-image}

![File](./img/loadflow/File.svg){width="3%"} The network is modeled in [IIDM](inv:powsyblcore:*:*#grid_exchange_formats/iidm/index), which is the
internal model of Powsybl. This model can be serialized in an XML format for experimental purposes.
It is available on GitHub in the [tutorial repository](https://github.com/powsybl/powsybl-tutorials) under 
`powsybl-tutorials/loadflow/src/main/resources/eurostag-tutorial1-lf.xml`. You can download it and add it to your 
resources directory or use your own network.
In the `LoadFlowTutorial` class, create the `Main` method and add this code to load the network from the resources.

```java
final String networkFileName = "eurostag-tutorial1-lf.xml";
final InputStream is = LoadflowTutorial.class.getClassLoader().getResourceAsStream(networkFileName);
```

![Import](./img/loadflow/Import.svg){width="5%"} The file is imported through a gateway that converts the file
to an in-memory model.

```java
Network network = Network.read(networkFileName, is);
```

Let's quickly scan the network.
In this tutorial, it consists of two substations. Each substation has two voltage levels and a two-winding transformer.
```java
for (Substation substation : network.getSubstations()) {
    LOGGER.info("Substation {}", substation.getNameOrId());
    LOGGER.info("Voltage levels:");
    for (VoltageLevel voltageLevel : substation.getVoltageLevels()) {
        LOGGER.info("Voltage level: {} {}kV", voltageLevel.getId(), voltageLevel.getNominalV());
    }
    LOGGER.info("Two windings transformers:");
    for (TwoWindingsTransformer t2wt : substation.getTwoWindingsTransformers()) {
        LOGGER.info("Two winding transformer: {}", t2wt.getNameOrId());
    }
    LOGGER.info("Three windings transformers:");
    for (ThreeWindingsTransformer t3wt : substation.getThreeWindingsTransformers()) {
        LOGGER.info("Three winding transformer: {}", t3wt.getNameOrId());
    }
}
```

There are two lines in the network.
```java
for (Line line : network.getLines()) {
    LOGGER.info("Line: {}", line.getNameOrId());
    LOGGER.info(" > Terminal 1 power: {}", line.getTerminal1().getP());
    LOGGER.info(" > Terminal 2 power: {}", line.getTerminal2().getP());
}
```

## Run a power flow calculation
![compute_lf](./img/loadflow/Compute_LF.svg){width="5%"}
Then, flows are computed with a load flow simulator. In this tutorial, we use the OpenLoadflow implementation, which is 
open-source software, natively based on the Powsybl network grid model. For more details, please visit the 
[documentation](inv:powsyblopenloadflow:*:*#index) to learn more about it. 

A load flow is run on a variant of the network. 
A network variant is close to a state vector and gathers variables such as injections, productions, tap positions, 
states of buses, etc.
The computed flows are stored in the variant given in input. Defining the variant specifically is actually optional. 
If it is not the case, the computation will be run on the default initial variant created by PowSyBl by default.

Let us define the variant first:
```java
final String variantId = "loadflowVariant";
network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, variantId);
network.getVariantManager().setWorkingVariant(variantId);
```
Here we have saved the initial variant and set the new variant as the one to be used.

In order to run the load flow calculation, we also need to define the set of parameters to be used.
The default parameters are listed [here](inv:powsyblcore:*:*#simulation/loadflow/configuration). Here, angles are set to zero and voltages are set to one per unit. 

```java
LoadFlowParameters loadflowParameters = new LoadFlowParameters()
        .setVoltageInitMode(LoadFlowParameters.VoltageInitMode.UNIFORM_VALUES);
LoadFlow.run(network, loadflowParameters);
```

The flow through the upper line is of 302.4 MW at its entrance and of 300.4 MW at its exit. The flow through the lower line is the same. 
The power losses are of 2 MW on each line.   

If you wish to set the parameters in the config file and use them directly, you can write instead:
```java
LoadFlowParameters loadflowParameters = LoadFlowParameters.load();
LoadFlow.run(network, loadflowParameters);
```
You'll have to fill two configuration sections in the `config.yml` file, for example:
```yaml
load-flow-default-parameters:
  voltageInitMode: DC_VALUES
  transformerVoltageControlOn: false
  twtSplitShuntAdmittance: true
  dc: false

open-loadflow-default-parameters:
  lowImpedanceBranchMode: REPLACE_BY_ZERO_IMPEDANCE_LINE
```

## Output the results in the terminal

Let us compare the voltages and angles before and after the calculation:

```java
double angle;
double v;
double oldAngle;
double oldV;
for (Bus bus : network.getBusView().getBuses()) {
    network.getVariantManager().setWorkingVariant(VariantManagerConstants.INITIAL_VARIANT_ID);
    oldAngle = bus.getAngle();
    oldV = bus.getV();
    network.getVariantManager().setWorkingVariant(variantId);
    angle = bus.getAngle();
    v = bus.getV();
    LOGGER.info("Angle difference  : {}", angle - oldAngle);
    LOGGER.info("Tension difference: {}", v - oldV);
}
```

## Apply a contingency on the network and run a load flow again

![Final simple network](./img/loadflow/Network_Simple_Final.svg){width="50%" .center-image}

![Modify_N-1](./img/loadflow/Modify_N-1.svg){width="5%"}
A contingency is simply simulated by disconnecting both terminals of the `NHV1_NHV2_1` line.

```java
network.getLine("NHV1_NHV2_1").getTerminal1().disconnect();
network.getLine("NHV1_NHV2_1").getTerminal2().disconnect();
```
![compute_LF](./img/loadflow/Compute_LF.svg){width="5%"}
Once the contingency is applied on the network, the post-contingency state of the network is computed through a load flow in the same way as above.

A new load flow computes the flow on the lower line: it is now of 610.6 MW at its entrance and of 601 MW at its exit. 
The rest of the difference between load and generation represents the losses during the voltage transformation process.

```java
final String contingencyVariantId = "contingencyLoadflowVariant";
network.getVariantManager().cloneVariant(variantId, contingencyVariantId);
network.getVariantManager().setWorkingVariant(contingencyVariantId);
LoadFlow.run(network);
```

Let's analyze the results. First, we make some simple prints in the terminal: 
```java
for (Line l : network.getLines()) {
    LOGGER.info("Line: {}", line.getNameOrId());
    LOGGER.info(" > Terminal 1 power: {}", line.getTerminal1().getP());
    LOGGER.info(" > Terminal 2 power: {}", line.getTerminal2().getP());
}
```

We will also show how to define a visitor object, that can be used to loop over equipment. We will use it to display 
the power sources and loads of the network. Visitors are usually used to efficiently access the network components, 
and change their properties, for example. Here we will just print some data about the generators and loads.
```java
final TopologyVisitor visitor = new DefaultTopologyVisitor() {
    @Override
    public void visitGenerator(Generator generator) {
        LOGGER.info("Generator: {} [{} MW]", generator.getNameOrId(), generator.getTerminal().getP());
    }

    @Override
    public void visitLoad(Load load) {
        LOGGER.info("Load: {} [{} MW]", load.getNameOrId(), load.getTerminal().getP());
    }
};
for (VoltageLevel voltageLevel : network.getVoltageLevels()) {
    voltageLevel.visitEquipments(visitor);
}
```
The power now flows only through the line `NHV1_NHV2_2`, as expected.

## Summary
We have learnt how to write Java code to run power flows. 
We have shown how to load a network file, how to create and use network variants, and how to set the load flow
parameters. We've also seen how to output the results in the terminal.

## Going further
The following links could also be useful:
- [Run a power flow through an iTools command](inv:powsyblcore:std:doc#user/itools/loadflow): Learn how to perform a power flow calculation from the command line 
- [Sensitivity analysis tutorial](./sensitivity-analysis.md): Learn how to write the Java code to perform sensitivity analyses
