---
layout: default
---

# Write the Java code to perform Dynawo dynamic simulation

This tutorial shows how to write Java code using the PowSyBl Dynamic Simulation API and the Dynawo simulation engine to perform time-domain dynamic simulations on a power system network.

Dynamic simulation allows to analyze the transient and long-term dynamic behavior of a power grid following events such as line trips, generator outages, or short circuits. In this tutorial, you will learn how to configure and run a dynamic simulation on an IEEE 14-bus test network using the `DynawoSimulation` class from the `dynawo` tutorial module.

## What will you build?

You will write a Java application that:
1. Configures a `DynawoSimulationProvider` pointing to the local Dynawo installation.
2. Loads a power grid model (`IEEE14.iidm`).
3. Maps dynamic models onto static network components (such as exponential loads and synchronous generators with automatic voltage regulation) using a Groovy DSL script ([dynamicModels.groovy](../dynawo/src/main/resources/ieee14/dynamicModels.groovy)).
4. Defines dynamic events (a transmission line disconnection at $t = 1\text{ s}$) using a Groovy DSL script ([eventModels.groovy](../dynawo/src/main/resources/ieee14/eventModels.groovy)).
5. Specifies output variables to monitor (bus voltages, generator rotational speeds, active/reactive powers, field voltage) using a Groovy DSL script ([outputVariables.groovy](../dynawo/src/main/resources/ieee14/outputVariables.groovy)).
6. Sets solver options and model parameters through Dynawo parameter files (`.par` files in the `config` folder).
7. Executes the simulation asynchronously through PowSyBl computation manager and logs the simulation report.

```text
Static Network (IIDM)
         +
Dynamic Models (Groovy DSL + .par)   ==>  DynawoSimulationProvider  ==>  Dynamic Results & Curves
         +                                                                (Time series, timeline XML,
Events (Groovy DSL)                                                        ReportNode execution log)
         +
Monitored Variables (Groovy DSL)
```

## What will you need?

- **Java Development Kit (JDK)**: Version 21 or later.
- **Maven**: Version 3.6 or later.
- **Dynawo executable**: Dynawo is an open-source hybrid C++/Modelica power system simulation engine. Download the latest release from the [Dynawo GitHub repository](https://github.com/dynawo/dynawo/releases/) and install/extract it on your machine.
- An IDE such as [IntelliJ IDEA](intellij.md).

## Maven Configuration

To run dynamic simulations with Dynawo in your Maven project, include the following dependencies in your `pom.xml`:

```xml
<dependencies>
    <!-- PowSyBl Dynamic Simulation API & DSL -->
    <dependency>
        <groupId>com.powsybl</groupId>
        <artifactId>powsybl-dynamic-simulation-api</artifactId>
    </dependency>
    <dependency>
        <groupId>com.powsybl</groupId>
        <artifactId>powsybl-dynamic-simulation-dsl</artifactId>
    </dependency>

    <!-- Dynawo Provider & Dynawo DSL -->
    <dependency>
        <groupId>com.powsybl</groupId>
        <artifactId>powsybl-dynawo-simulation</artifactId>
    </dependency>
    <dependency>
        <groupId>com.powsybl</groupId>
        <artifactId>powsybl-dynawo-dsl</artifactId>
    </dependency>

    <!-- IIDM Network Implementation -->
    <dependency>
        <groupId>com.powsybl</groupId>
        <artifactId>powsybl-iidm-impl</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Logging & Reporting -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-simple</artifactId>
    </dependency>
    <dependency>
        <groupId>com.powsybl.tutorials</groupId>
        <artifactId>commons</artifactId>
    </dependency>
</dependencies>
```

## How to complete this tutorial?
To complete this tutorial, you can either start from scratch and write the code or you can download the sources from the
[tutorial repository](https://github.com/powsybl/powsybl-tutorials) on GitHub.

## Create a new project from scratch
Create a new Maven `pom.xml` file in a directory called `dynawo-simulation` with the following content:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.powsybl</groupId>
        <artifactId>powsybl-parent</artifactId>
        <version>24</version>
        <relativePath/>
    </parent>

    <artifactId>dynawo-simulation</artifactId>
    <name>Dynawo dynamic simulation</name>

    <properties>
      <maven.exec.version>3.6.2</maven.exec.version>
      <java.version>21</java.version>

      <powsybl-dependencies.version>2026.2.1</powsybl-dependencies.version>
      <powsybl-core.version>7.4.3</powsybl-core.version>
      <slf4j.version>2.0.17</slf4j.version>
    </properties>
</project>
```

## Configure the maven pom file
In the `pom.xml`, add first the following lines in the `<properties>` section to make it possible to run the future main class through Maven:

```xml
<exec.cleanupDaemonThreads>false</exec.cleanupDaemonThreads>
<exec.mainClass>com.powsybl.tutorials.dynawo.DynawoSimulation</exec.mainClass>
<exec.optionalArgument>${env.HOME}/dynawo/dynawo</exec.optionalArgument>
```
The optional argument should point to the installed dynawo on your computer. 
When you'll have created the `DynawoSimulation` class and its main function, you'll then be able to execute your code through:

```
$> mvn clean install exec:exec@run"
```

## Tutorial Steps

### Initialize the Dynawo simulation provider and parameters

The `DynawoSimulationProvider` acts as the bridge between PowSyBl's high-level dynamic simulation interfaces and the underlying Dynawo executable.

In the `main` method, we read the path to the Dynawo executable from the first program argument (`args[0]`), configure the provider, and set general simulation parameters such as the start and stop times:

```java
// Path to the Dynawo executable binary
LOGGER.info("args[0]: {} (path to Dynawo executable)", args[0]);

DynawoSimulationProvider provider = new DynawoSimulationProvider(
        new DynawoSimulationConfig(Path.of(args[0]), true)
);

DynamicSimulationParameters parameters = new DynamicSimulationParameters()
        .setDebugDir(TMP_DIR + "/tutorials_dynamic_simulation/")
        .setStartTime(0)
        .setStopTime(100);
```

- `setDebugDir(...)`: Specifies the directory where input files, logs, and simulation output curves are generated.
- `setStartTime(0)` and `setStopTime(100)`: Sets the simulation time span (from $t = 0\text{ s}$ to $t = 100\text{ s}$).

### Load the network

Load the static network model from an IIDM file (here the IEEE 14-bus network located in resources):

```java
String path = DynawoSimulation.class.getClassLoader().getResource("IEEE14.iidm").getPath();
Network network = Network.newReader(path).read();
```

### Assign dynamic models via Groovy

Static network components in IIDM (loads, generators) must be mapped to dynamic Modelica models in Dynawo. PowSyBl provides a Groovy to assign dynamic models to network equipment.

In `src/main/resources/ieee14/dynamicModels.groovy`:

```groovy
package ieee14.disconnectline

import com.powsybl.iidm.network.Load
import com.powsybl.iidm.network.Generator

// Assign voltage-dependent exponential load models
for (Load load : network.loads) {
    if (load.id != "_LOAD___8_EC") {
        LoadAlphaBeta {
            staticId load.id
            parameterSetId "LAB"
        }
    }
}

// Assign 4-winding or 3-winding synchronous generator models with voltage regulators
for (Generator gen : network.generators) {
    if (gen.id != "_GEN____6_SM" && gen.id != "_GEN____8_SM") {
        GeneratorSynchronousFourWindingsProportionalRegulations {
            staticId gen.id
            parameterSetId "GSFWPR" + gen.id
        }
    } else {
        GeneratorSynchronousThreeWindingsProportionalRegulations {
            staticId gen.id
            parameterSetId "GSTWPR" + gen.id
        }
    }
}
```

In Java, instantiate the `GroovyDynamicModelsSupplier`:

```java
GroovyDynamicModelsSupplier dynamicModelsSupplier = new GroovyDynamicModelsSupplier(
        getResourceAsStream("/ieee14/dynamicModels.groovy"),
        GroovyExtension.find(DynamicModelGroovyExtension.class, DynawoSimulationProvider.NAME));
```

### Define dynamic events via Groovy

Events represent perturbations applied during the simulation (e.g. line tripping, load variation, generator disconnection).

In `src/main/resources/ieee14/eventModels.groovy`, we schedule a disconnection on side 2 of line `_BUS____1-BUS____5-1_AC` at $t = 1\text{ s}$:

```groovy
package ieee14.disconnectline

import com.powsybl.iidm.network.TwoSides

Disconnect {
    staticId "_BUS____1-BUS____5-1_AC"
    startTime 1
    disconnectOnly TwoSides.TWO
}
```

In Java, instantiate the `GroovyEventModelsSupplier`:

```java
GroovyEventModelsSupplier eventModelsSupplier = new GroovyEventModelsSupplier(
        getResourceAsStream("/ieee14/eventModels.groovy"),
        GroovyExtension.find(EventModelGroovyExtension.class, DynawoSimulationProvider.NAME));
```

### Configure monitored output variables and curves

To inspect how electrical variables evolve over time, we specify the output curves to record.

In `src/main/resources/ieee14/outputVariables.groovy`:

```groovy
package ieee14.disconnectline

import com.powsybl.iidm.network.Bus
import com.powsybl.iidm.network.Generator
import com.powsybl.iidm.network.Load

// Record voltage magnitude (Upu_value) for all buses
for (Bus bus : network.busBreakerView.buses) {
    fsv {
        id bus.id
        variable "Upu_value"
    }
}

// Record generator angular speed, active/reactive power, stator voltage and field voltage
for (Generator gen : network.generators) {
    curve {
        id gen.id
        variables "generator_omegaPu", "generator_PGen", "generator_QGen", "generator_UStatorPu", "voltageRegulator_EfdPu"
    }
}

// Record active and reactive power for load _LOAD___2_EC
for (Load load : network.loads) {
    if (load.id == "_LOAD___2_EC") {
        curve {
            id load.id
            variables "load_PPu", "load_QPu"
        }
    }
}
```

In Java, instantiate the `GroovyOutputVariablesSupplier`:

```java
GroovyOutputVariablesSupplier outputVariablesSupplier = new GroovyOutputVariablesSupplier(
        getResourceAsStream("/ieee14/outputVariables.groovy"),
        GroovyExtension.find(OutputVariableGroovyExtension.class, DynawoSimulationProvider.NAME));
```

### Configure Dynawo parameters and solvers

Dynawo model parameters (time constants, gains, limits), network settings, and numerical solver configurations are loaded via `DynawoSimulationParameters`:

```java
DynawoSimulationParameters dynawoSimulationParameters = DynawoSimulationParameters.load();
parameters.addExtension(DynawoSimulationParameters.class, dynawoSimulationParameters);

dynawoSimulationParameters.setModelsParameters(getResourceAsStream("/config/models.par"))
        .setNetworkParameters(getResourceAsStream("/config/network.par"), "8")
        .setSolverParameters(getResourceAsStream("/config/solvers.par"), "3")
        .setSolverType(DynawoSimulationParameters.SolverType.SIM)
        .setTimelineExportMode(ExportMode.XML);
```

- `models.par`: Contains parameters referenced by `parameterSetId` (e.g. `LAB` for loads, `GSFWPR...` for generators).
- `network.par`: Defines network-wide settings (e.g. transformer tap delays, load recovery parameters).
- `solvers.par`: Configures solver parameters (integration time step bounds `hMin`/`hMax`, convergence tolerances).
- `SolverType`: Choice between `SIM` (simplified solver) and `IDA` (SUNDIALS DAE solver with adaptive time-stepping).
- `timelineExportMode`: Exports the discrete events timeline to XML.

### Run the simulation and print the report

We create a structured `ReportNode` to capture execution messages and run the simulation asynchronously using `provider.run(...)`:

```java
ReportNode reportNode = ReportNode.newRootReportNode()
        .withAllResourceBundlesFromClasspath()
        .withMessageTemplate("tutorial.dynawo.root")
        .build();

DynamicSimulationRunParameters runParameters = new DynamicSimulationRunParameters()
        .setParameters(parameters)
        .setEventModelsSupplier(eventModelsSupplier)
        .setOutputVariablesSupplier(outputVariablesSupplier)
        .setReportNode(reportNode);

DynamicSimulationResult report = provider.run(
    network,
    VariantManagerConstants.INITIAL_VARIANT_ID,
    dynamicModelsSupplier,
    runParameters
    ).join();

StringWriter sw = new StringWriter();
reportNode.print(sw);
System.out.println(sw);
```

The method `.join()` blocks until the simulation computation finishes. The `reportNode` prints execution status, milestones, and any diagnostic warnings to standard output.

## Running the tutorial

To run the `DynawoSimulation` class, supply the path to your Dynawo installation as the first argument.

Using Maven:

```bash
mvn clean compile exec:java -Dexec.mainClass="com.powsybl.tutorials.dynawo.DynawoSimulation" -Dexec.args="/path/to/dynawo/bin/dynawo"
```

Or run directly from your IDE by creating an Application Run Configuration with `com.powsybl.tutorials.dynawo.DynawoSimulation` as the main class and `/path/to/dynawo/bin/dynawo` in Program arguments.

### Outputs

After execution, check the debug directory configured in `setDebugDir(...)` (e.g. `/tmp/tutorials_dynamic_simulation/dynawo_XXX`):
- `outputs/`
  - `curves/`: Time-series data files containing the trajectories of all monitored variables over the 100 seconds simulation.
  - `timeLine/`: XML file logging discrete state changes and protection activations.
  - `logs/dynawo.log`: Solver execution logs and convergence details.
- `.jobs` files containing simulation parameters generated for Dynawo run
- `.dyd` files containing dynamic models generated for Dynawo run
- `.par` files containing parameters sets generated for Dynawo run 
- Optionnaly:
- `.crv` files containing curves to compute
- `.fsv` files containing 'final sate values' to compute


## Summary
We have learnt how to run a dynamic simulation with a dynawo provider.
We have shown how to load a network file and how to set the dynamic simulation parameters.
We've also seen what kind of outputs are generated through this simulation.

## Going further
The following links could also be useful:
- [Configure a dynamic simulation](inv:powsybldynawo#dynamic_simulation/configuration): Documentation to set all the parameters to run a dynamic simulation with powsybl dynawo
- [Configure dynamic models](inv:powsybldynawo#dynamic_simulation/dynamic-models-configuration): configure dynamic models used in Dynawo
- [Configure events models](inv:powsybldynawo#dynamic_simulation/event-models-configuration): configure events that occur during the simulation
- [Configure outputs variables](inv:powsybldynawo#dynamic_simulation/output-variables-configuration): configure the outputs variables you want Dynawo to export after the simulation to create curves...
