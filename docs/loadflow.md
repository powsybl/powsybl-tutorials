---
layout: default
---

# Write the Java code to perform power flows
This tutorial shows you how to write a Java code to perform load flow calculations on a network, just on its current state but also on an N-1 state by applying a contingencies. You'll see how to configure PowSyBl, through a YML file and overwriting it with a JSON file, how to provide the input network, and how to output the load flow results to the terminal.

* TOC
{:toc}

## What will you build?

The tutorial can be expressed in a short and easy workflow: all the input data is stored in an XIIDM file. This file is imported with the IIDM importer. 
Then, a load flow simulator is launched to get flows on all nodes. In this tutorial, the simulator is Open Loadflow, but it could be another load flow simulator, 
as long as the API contract is respected. A contingency is created and finally, the flows are computed again in order to get the final state.  

![Workflow](./img/loadflow/Workflow.svg){: width="75%" .center-image}

## What will you need?
- About 1/2 hour
- A favorite text editor or IDE
- JDK 1.17 or later
- You can also import the code straight into your IDE:
    - [IntelliJ IDEA](intellij.md)

## How to complete this tutorial?
Like most tutorials, you can start from scratch and complete each step, or you can bypass basic setup steps that are already familiar to you. Either way, you end up with working code.

To start from scratch, move on to [Create a new project](#create-a-new-project-from-scratch).

To skip the basics, do the following:
- Download and unzip the [source repository](https://github.com/powsybl/powsybl-tutorials), or clone it using Git: `git clone https://github.com/powsybl/powsybl-tutorials`.
- Change directory to `loadflow/initial`
- Jump ahead to [Configure the pom file](#configure-the-maven-pom-file)

When you finish, you can check your results against the code in `loadflow/complete`.

## Create a new project from scratch
Create a new Maven's `pom.xml` file in `loadflow` with the following content:
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
        <powsybl-dependencies.version>2023.3.1</powsybl-dependencies.version>
    </properties>
</project>
```

## Configure the maven pom file

First, in the `pom.xml`, add the following lines in the `<properties>` section to make it possible to run the future main class through maven:
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
- `com.powsybl:powsybl-open-loadflow` to provide an implementation for the loadflow calculation.
- `com.powsybl:powsybl-iidm-impl` to load and work with networks.

**Note:** PowSyBl uses [slf4j](http://www.slf4j.org/) as a facade for various logging framework, but some APIs we use in PowSyBl use [log4j](https://logging.apache.org/log4j), which is not compatible with slf4j, making it necessary to create a bridge between the two logging system.

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
Edit the file named `config.yml` at the location `loadflow/src/main/resources`.
Start the configuration by writing:
```yaml
load-flow:
  default-impl-name: "OpenLoadFlow"
```
In this way, PowSyBl will be set to use the OpenLoadflow implementation for the power flow.

## Import the network from an XML IIDM file

In this tutorial, the network is quite simple and made of two lines in parallel, with a generator on the left side and a load on the right side. 
The load consumes 600 MW and the generator produces 606.5 MW. 

![Initial simple network](./img/loadflow/Network_Simple_Initial.svg){: width="50%" .center-image}

First, create a logger outside your main method. We will use it to display information about the objects we handle.
```java
private static final Logger LOG = LoggerFactory.getLogger(LoadflowTutorial.class);
```

<img src="./img/loadflow/File.svg" alt="" style="vertical-align: bottom"/>
The network is modeled in [IIDM](../../grid/formats/xiidm.md), which is the internal model of Powsybl. This model can be serialized in an XML format for experimental purposes.
```java
final String networkFileName = "eurostag-tutorial1-lf.xml";
final InputStream is = LoadflowTutorial.class.getClassLoader().getResourceAsStream(networkFileName);
```
<br />
<img src="./img/loadflow/Import.svg" alt="" style="vertical-align: bottom"/>
The file is imported through a gateway that converts the file to an in-memory model.
```java
Network network = Network.read(networkFileName, is);
```
<br />

Let's just quickly scan the network.
In this tutorial it is composed of two substations. Each substation has two voltage
levels and one two-windings transformer.
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

<img src="./img/loadflow/Compute_LF.svg" alt="" style="vertical-align: bottom"/>
Then, flows are computed with a load flow simulator. In this tutorial, we use the OpenLoadflow implementation, which is open-source software, natilevly based on the Powsybl network grid model. For more details, please visit the [documentation](../../simulation/powerflow/openlf.md) to learn more about it. 

A loadflow is run on a variant of the network. 
A network variant is close to a state vector and gathers variables such as 
injections, productions, tap positions, states of buses, etc.
The computed flows are stored in the variant given in input. 
Defining the variant specifically is actually optional. 
If it is not the case, the computation will be run on the default initial variant created by PowSyBl by default.
Let us first define the variant:
```java
final String variantId = "loadflowVariant";
network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, variantId);
network.getVariantManager().setWorkingVariant(variantId);
```
Here we have saved the initial variant and set the new variant as the one to be used.

In order to run the load flow calculation, we also need to define the set of parameters to be used.
The default parameters are listed [here](../configuration/parameters/LoadFlowParameters.md). Here, angles are set to zero and voltages are set to one per unit. 

```java
LoadFlowParameters loadflowParameters = new LoadFlowParameters()
        .setVoltageInitMode(LoadFlowParameters.VoltageInitMode.UNIFORM_VALUES);
LoadFlow.run(network, loadflowParameters);
```

The flow through the upper line is of 302.4 MW at its entrance and of 300.4 MW at its exit. The flow through the lower line is the same. The power losses are of 2 MW on each line.   

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

![Final simple network](./img/loadflow/Network_Simple_Final.svg){: width="50%" .center-image}

<br />
<img src="./img/loadflow/Modify_N-1.svg" alt="" style="vertical-align: bottom"/>
A contingency is simply simulated by disconnecting both terminals of the `NHV1_NHV2_1` line.

```java
network.getLine("NHV1_NHV2_1").getTerminal1().disconnect();
network.getLine("NHV1_NHV2_1").getTerminal2().disconnect();
```
<br />
<img src="./img/loadflow/Compute_LF.svg" alt="" style="vertical-align: bottom"/>
Once the contingency is applied on the network, the post-contingency state of the network is computed through a loadflow in the same way as above.

A new load flow computes the flow on the lower line: it is now of 610.6 MW at its entrance and of 601 MW at its exit. The rest of the difference between load and generation represents the losses during the voltage transformation process.

```java
final String contingencyVariantId = "contingencyLoadflowVariant";
network.getVariantManager().cloneVariant(variantId, contingencyVariantId);
network.getVariantManager().setWorkingVariant(contingencyVariantId);
LoadFlow.run(network);
```

Let's analyze the results. First we make some simple prints in the terminal: 
```java
for (Line l : network.getLines()) {
    LOGGER.info("Line: {}", line.getNameOrId());
    LOGGER.info(" > Terminal 1 power: {}", line.getTerminal1().getP());
    LOGGER.info(" > Terminal 2 power: {}", line.getTerminal2().getP());
}
```

Here we will also show how to define a visitor object, that may be used to loop over equipments. We will use it to print the energy sources and the loads of the network. Visitors are usually used to access the network equipments efficiently, and modify their properties for instance. Here we just print some data about generators and loads.
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
We have shown how to load a network file, how to create and use network variants, and how to set the load flow parameters. We've also seen how to output the results in the terminal.

## Going further
The following links could also be useful:
- [Run a power flow through an iTools command](../../user/itools/loadflow.md): Learn how to perform a power flow calculation from the command line 
- [Sensitivity analysis tutorial](./sensitivity-analysis.md): Learn how to write the Java code to perform sensitivity analyses
