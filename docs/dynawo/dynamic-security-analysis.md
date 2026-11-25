---
layout: default
---

# Write the Java code to perform Dynawo dynamic security analysis

This tutorial shows how to use the PowSyBl Dynamic Security Analysis API with the Dynawo algorithms provider. A dynamic security analysis runs a time-domain simulation for each contingency and checks the simulated states against configured security criteria.

The example uses the IEEE 14 network and the `DynawoSecurityAnalysis` class in the `dynawo` tutorial module. It simulates 2 contingencies, one is the loss of a line and the other is the loss of a generator, with a common line-disconnection event in each simulation.

## What will you build?

You will write a Java application that:
1. Creates a `DynawoSecurityAnalysisProvider` configured with the local Dynawo algorithms installation.
2. Loads the IEEE 14 network (`IEEE14.iidm`) and assigns dynamic models using a Groovy script.
3. Defines line and generator contingencies, plus a common event applied in every simulation.
4. Configures Dynawo model, network, solver, and security criteria files.
5. Runs the analysis over a 100 second window and prints the pre-contingency and post-contingency results as text and JSON.

```text
Network + dynamic models
          +
Contingencies + common events
          +
Dynawo parameters + security criteria
          |
          v
DynawoSecurityAnalysisProvider
          |
          v
Pre-contingency and post-contingency statuses, violations, and JSON result
```

## What will you need?

- **Java Development Kit (JDK)**: Version 21 or later.
- **Maven**: Version 3.6 or later.
- **Dynawo algorithms executable**: Download dynawo algorithms from the [Dynawo algorithms GitHub repository](https://github.com/dynawo/dynawo-algorithms/releases/) and install/extract it on your machine.
- An IDE such as [IntelliJ IDEA](../intellij.md), if you want to run the example from an IDE.

## Tutorial files

The tutorial source and input files are in the [`dynawo` module](../../dynawo/):

- [`DynawoSecurityAnalysis.java`](../../dynawo/src/main/java/com/powsybl/tutorials/dynawo/DynawoSecurityAnalysis.java): configures and runs the analysis.
- [`IEEE14.iidm`](../../dynawo/src/main/resources/ieee14/IEEE14.iidm): static network model.
- [`dynamicModels.groovy`](../../dynawo/src/main/resources/ieee14/dynamicModels.groovy): associates Dynawo dynamic models with network loads and generators.
- [`models.par`](../../dynawo/src/main/resources/config/models.par), [`network.par`](../../dynawo/src/main/resources/config/network.par), and [`solvers.par`](../../dynawo/src/main/resources/config/solvers.par): Dynawo model, network, and solver parameters.
- [`criteria.crt`](../../dynawo/src/main/resources/config/criteria.crt): criteria used to assess the simulated states.
- [`config.yml`](../../dynawo/src/main/resources/config.yml): PowSyBl and Dynawo configuration.

## Configure Dynawo algorithms

The `main` method takes the Dynawo algorithms path as its first program argument and uses it to create the provider:

```java
DynawoSecurityAnalysisProvider provider = new DynawoSecurityAnalysisProvider(
    new DynawoAlgorithmsConfig(Path.of(args[0]), true)
);
```

The example `config.yml` also contains a `dynawo-algorithms` section. Replace its `/path_to_folder/...` placeholder with the path for your installation if you use this configuration. The tutorial's Java code passes the provider path explicitly, so pass the same valid path as the first program argument when running the class.

The parameter files and criteria file are loaded from `src/main/resources/config` on the classpath. The source resolves them relative to the `config` resource folder, so keep the files together if you adapt the example.

## How the analysis is configured

### Load the network and dynamic models

The example reads `IEEE14.iidm` and uses `GroovyDynamicModelsSupplier` to load the model associations from `dynamicModels.groovy`. That script maps loads and generators in the static network to Dynawo models and parameter sets IDs. Each equipment ID and parameter set referenced by the script must exist in the network and in the parameter files.

### Define contingencies and common events

The example defines two contingencies:

```java
List<Contingency> contingencies = List.of(
    Contingency.line("_BUS____1-BUS____5-1_AC", "_BUS____5_VL"),
    Contingency.generator("_GEN____2_SM"));
ContingenciesProvider contingenciesProvider = n -> contingencies;
```

The `ContingenciesProvider` supplies this list to the analysis. The IDs must match equipment in `IEEE14.iidm`.

An `EventModelsSupplier` adds a disconnection of line `_BUS____9-BUS___10-1_AC` at 10 seconds to every simulation. This common event is separate from the contingency: it is replayed along with each contingency. The example also applies each contingency after a 10-second delay, so both operations are scheduled at the same simulation time; adjust either time if that is not the intended scenario.

### Configure Dynawo parameters and security criteria

The example loads its Dynawo parameter sets from the `.par` files and attaches them to the dynamic simulation parameters:

```java
dynawoSimulationParameters
    .setModelsParameters(models)
    .setNetworkParameters(networkPar, "8")
    .setSolverParameters(solvers, "3")
    .setSolverType(DynawoSimulationParameters.SolverType.SIM)
    .setCriteriaFilePath(CONFIG_FOLDER.resolve("criteria.crt"));
```

- `models.par` contains parameter sets referenced by the dynamic-model script.
- `network.par` supplies network parameter set `8`.
- `solvers.par` supplies solver parameter set `3`; the selected solver type is `SIM`.
- `criteria.crt` defines the limits used by the Dynawo security analysis to assess simulated states and report violations.

The dynamic simulation window is set to 0–100 seconds. `DynamicSecurityAnalysisParameters.ContingenciesParameters(10)` sets the contingency delay to 10 seconds. These values can be changed to match the study being performed.

### Run the analysis and inspect results

`DynamicSecurityAnalysisRunParameters` combines the analysis parameters, common-event supplier, and local computation manager. The provider runs the analysis asynchronously; `.join()` waits for the result:

```java
SecurityAnalysisResult result = provider.run(
    network,
    VariantManagerConstants.INITIAL_VARIANT_ID,
    dynamicModelsSupplier,
    contingenciesProvider,
    runParameters).join().getResult();
```

The example prints the pre-contingency status and each post-contingency status, followed by any limit violations. It then serializes the complete result as JSON with `SecurityAnalysisResultSerializer`.

## Running the tutorial

Run the command from the repository root, replacing the argument with the path to your Dynawo algorithms executable or installation:

```bash
mvn -pl dynawo compile exec:java \
  -Dexec.mainClass="com.powsybl.tutorials.dynawo.DynawoSecurityAnalysis" \
  -Dexec.args="/path/to/dynawo-algorithms"
```

If you want to load the module's `config.yml`, set `powsybl.config.dirs` to the resources directory as well:

```bash
mvn -pl dynawo compile exec:java \
  -Dexec.mainClass="com.powsybl.tutorials.dynawo.DynawoSecurityAnalysis" \
  -Dexec.args="/path/to/dynawo-algorithms" \
  -Dpowsybl.config.dirs="$(pwd)/dynawo/src/main/resources"
```

For an IDE run configuration, use `com.powsybl.tutorials.dynawo.DynawoSecurityAnalysis` as the main class, set the Dynawo algorithms path as the first program argument, and add the `powsybl.config.dirs` system property if required. The module dependencies include the Dynawo security-analysis provider, dynamic simulation APIs, IIDM implementation, and PowSyBl configuration support.

## Results

The console output contains a summary for the pre-contingency state and for each contingency, including status and any reported violations. A JSON serialization of the complete `SecurityAnalysisResult` follows. A failed simulation status and a converged simulation with criterion violations are distinct outcomes; inspect both the status and the violations when evaluating a case.

## Summary

This tutorial demonstrates how to run a Dynawo dynamic security analysis from Java: load a network and its dynamic models, define contingencies and common events, configure the simulation and security criteria, and inspect results for each simulated case.

## Going further

- [Dynamic simulation tutorial](dynamic-simulation.md): configure and run a single Dynawo time-domain simulation.
- [Dynawo documentation](inv:powsybldynawo#dynamic_security_analysis/index): learn more about configuring Dynawo dynamic security analysis.
