---
layout: default
latex: true
---

# Write the Java code to perform a European Merging Function

This tutorial shows how to merge multiple IGMs from different TSOs and scale the resulting CGM according to actual market data. It implements the [European Merging Function](https://eepublicdownloads.entsoe.eu/clean-documents/Network%20codes%20documents/Implementation/cacm/cgmm/European_Merging_Function_Requirements_Specification.pdf) whose requirements can be found on the website of ENTSOE. 
The forecast net positions of the IGMs are computed through the [Pan European Verification Function](https://eepublicdownloads.entsoe.eu/clean-documents/EDI/Library/cim_based/schema/PEVF%20Implementation%20Guide_V1.0.pdf).

* TOC 
{:toc}

## What will you build?
First, your individual CGMES files will be imported and merged. Optionally, you will be able to compute a power flow on each IGM before merging. Then a load flow will be run on the CGM. The load flow simulator used in this tutorial is [OpenLoadFlow](../../simulation/powerflow/openlf.md).
After the load flow performed on the merged area, the net positions of each control area will be computed. The algorithm used for the balance computation is in the `powsybl-balances-adjustment` API. The PEVF (for Pan European Verification Function) file will be read and gives the expected AC net positions (DC net positions are not yet supported), and the balance adjustment is computed. Then the SV file of the CGM will be exported.

## What will you need?
- About two hours
- A favourite text editor or IDE
- JDK 1.11 or later
- Some IGMs that you want to merge and the corresponding PEVF and CGMES boundary files (EQBD, TPBD) 
- You can also import the code straight into your IDE:
  - [IntelliJ IDEA](intellij.md)
  
## How to complete this tutorial?
You can start from scratch and complete each step. Or you can directly check the written code from the git repository and change the configuration to make it work on your data. Either way, you end up with a working code. To start from scratch, move on to [Create a new project](#create-a-new-project-from-scratch).

For the input data, you need:
- A folder containing your IGMs in CIM-CGMES format. Each IGM needs to be zipped with the EQ, TP, SSH and SV.
- A PEVF file corresponding to the AC net position of the IGMs and the DC net position of the cross-border HVDC lines. It should not be zipped.
- A folder containing the CGMES boundary files, EQBD and TPBD, unzipped as well.

When you are done with the tutorial, you can compare your code with the code in the Github repository.

## Create a new project from scratch
To start from scratch, you need to create a file called `pom.xml` in `emf` with the following content:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
      <groupId>com.powsybl</groupId>
      <artifactId>powsybl-parent</artifactId>
      <version>3</version>
      <relativePath/>
    </parent>

    <artifactId>emf</artifactId>
    <name>Emf</name>
    <version>1.3.0-SNAPSHOT</version>
  
    <properties>
      <powsybl-dependencies.version>2023.0.1</powsybl-dependencies.version>
      <slf4j.version>1.7.22</slf4j.version>
      <logback.version>1.2.9</logback.version>
    </properties>
</project>
```
This file is creating your project, linking it to the parent `pom.xml` of `powsybl-tutorials` and setting the versions of the specific APIs that are used, as well as the properties and dependencies of the project. Do not hesitate to upgrade versions to benefit from new features.

## Configure the maven pom file
First, in the `pom.xml`, add the following lines in the `<properties>` section to make it possible to run the future main class through Maven:
```xml
<exec.cleanupDaemonThreads>false</exec.cleanupDaemonThreads>
<exec.mainClass>powsybl.tutorials.emf.EmfTutorial</exec.mainClass>
```
When you have created the `EmfTutorial` class and its main function, you are able to execute your code through:
```
$> mvn clean package exec:java
```
You also need to configure the pom file in order to use a configuration file taken in the classpath, instead of the one that is global to your system:
```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.codehaus.mojo</groupId>
      <artifactId>exec-maven-plugin</artifactId>
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
Now, we add all the **required** maven dependencies:
- `com.powsybl:powsybl-config-classic`: to provide a way to read the configuration.
- `org.slf4j:slf4j-simple`: to provide an implementation of `slf4j`.
- `com.powsybl:powsybl-open-loadflow`: to provide an [implementation](../../simulation/powerflow/openlf.md) for the load flow calculation.
- `com.powsybl:powsybl-iidm-impl`: to work with network core model API and in-memory implementation.
- `com.powsybl:powsybl-action-util`: to provide a set of common actions such as scaling.
- `com.powsybl:powsybl-balances-adjustment` and `com.powsybl:powsybl-entsoe-cgmes-balances-adjustment`: to provide an implementation to run an active power balance adjustment computation over several control areas. Through this API, it is possible to keep the power factor constant during the process by readjusting the reactive power as well (since version 1.6.0 indeed).
- `com.powsybl:powsybl-cgmes-conversion`, `com.powsybl:powsybl-triple-store-impl-rdf4j`, `com.powsybl:powsybl-cgmes-extensions`, `com.powsybl:powsybl-iidm-api`: to import/export the CGMES files and convert them into the network core model.
- `com.powsybl:powsybl-commons`: to provide a lot of really basic and technical utilities used everywhere in PowSyBl such as XML or JSON helpers, configuration, exceptions...

**Note:** PowSyBl uses [slf4j](http://www.slf4j.org/) as a facade for various logging framework, but some APIs we use in PowSyBl use [log4j](https://logging.apache.org/log4j), which is not compatible with slf4j, making it necessary to create a bridge between the two logging system.

You can add the following dependencies to the `pom.xml` file, with their corresponding versions:
```xml
<dependencies>
  <dependency>
    <groupId>com.powsybl</groupId>
    <artifactId>powsybl-action-util</artifactId>
  </dependency>
  <dependency>
    <groupId>com.powsybl</groupId>
    <artifactId>powsybl-balances-adjustment</artifactId>
  </dependency>
  <dependency>
    <groupId>com.powsybl</groupId>
    <artifactId>powsybl-cgmes-conversion</artifactId>
  </dependency>
  <dependency>
    <groupId>com.powsybl</groupId>
    <artifactId>powsybl-cgmes-extensions</artifactId>
  </dependency>
  <dependency>
    <groupId>com.powsybl</groupId>
    <artifactId>powsybl-commons</artifactId>
  </dependency>
  <dependency>
    <groupId>com.powsybl</groupId>
    <artifactId>powsybl-iidm-api</artifactId>
  </dependency>
  <dependency>
    <groupId>com.powsybl</groupId>
    <artifactId>powsybl-open-loadflow</artifactId>
  </dependency>

  <dependency>
    <groupId>com.powsybl</groupId>
    <artifactId>powsybl-config-classic</artifactId>
    <scope>runtime</scope>
  </dependency>
  <dependency>
    <groupId>com.powsybl</groupId>
    <artifactId>powsybl-iidm-impl</artifactId>
    <scope>runtime</scope>
  </dependency>
  <dependency>
    <groupId>com.powsybl</groupId>
    <artifactId>powsybl-triple-store-impl-rdf4j</artifactId>
  </dependency>
  <dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>log4j-over-slf4j</artifactId>
    <version>${slf4j.version}</version>
    <scope>runtime</scope>
  </dependency>
  <dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>${logback.version}</version>
    <scope>runtime</scope>
  </dependency>
  <dependency>
    <groupId>com.powsybl</groupId>
    <artifactId>powsybl-entsoe-cgmes-balances-adjustment</artifactId>
    <scope>compile</scope>
  </dependency>
</dependencies>
```

## Configure PowSyBl
The configuration file for PowSyBl is located in the folder `emf/src/main/resources`. It gathers all the default configuration parameters about the load flow simulator, the parameters for the import/export of CGMES files, the path to the CGMES files, the path to the PEVF file, the output directory, etc.

We need to configure PowSyBl to use the OpenLoadFlow implementation. To do that, you need to edit the `config.yml` file: 
```yaml
load-flow:
  default-impl-name: "OpenLoadFlow"
```
The parameters for the import and export of CGMES files are, with the proper path to the folder containing the boundary files:
```yaml
import-export-parameters-default-value:
  iidm.import.cgmes.boundary-location: path-EQBD-TPBD #complete with the path to your EQBD-TPBD files folder
  iidm.import.cgmes.profile-used-for-initial-state-values: SSH
  iidm.import.cgmes.store-cgmes-conversion-context-as-network-extension: true
  iidm.export.xml.version: "1.5"
  iidm.import.cgmes.ensure-id-alias-unicity: true
  iidm.import.cgmes.create-cgmes-export-mapping: true
```
Then, you can add the path to your PEVF file, the name and path to each IGM on a different line (line beginning with `-`) and the path to the directory where you want the SV file and log file to be saved.
```yaml
balances-adjustment-validation-parameters:
  data-exchanges-path: path-PEVF #complete with the path to your PEVF file

  igm-paths: 
    - IGM1-name, path-to-IGM1 #complete with the name of your first IGM and the path to the zipfile
    - IGM2-name, path-to-IGM2 #complete with the name of your second IGM and the path to the zipfile
    
  output-dir: path-to-directory
```

## Create `BalancesAdjustmentValidationParameters` class to load the parameters from the configuration file
First, we create a java class called `BalancesAdjustmentValidationParameters` containing the parameters that we want to use, such as the paths of the IGMs, the path to the PEVF file and the output directory. All these parameters are read from the configuration file created before. This class has a method to load the parameters from the configuration file. 

The IGM paths are stored in a HashMap and the output directory and PEVF in Strings. You can also create the getter/setter associated with each variable. Then, you need create a method `load` that will read the inputs from the configuration file and store the data in each variable. If you have difficulties creating this class, you can check the result `powsybl-tutorials/emf` from the Github repository.
If you want to learn more about the configuration file and how it is  handled by Powsybl, you can find more details [here](../../user/configuration/index.md).

Now with this class, we are able to read the extra parameters from the `config.yml` file. We will move on to create the `EmfTutorial` main class, that will perform the merging and the balance computation.

## Create `EmfTutorial` class to run the computation

In everything that follows, if you have difficulties creating the methods, you can refer to the code in `emf`, in the Github repository.

### Set the load flow parameters
Once you have created the `EmfTutorial` class, just before the main method, define the variable `LOAD_FLOW_PARAMETERS` of type `LoadFlowParameters`.
This variable gathers all the parameters to be used in the load flow pre-processing of the IGMs, in the load flow calculation on the CGM and for the balance computation iterations. In this tutorial, we set the initial angle values to angles computed through a DC power flow, the balance type to proportional to the maximum active power target of generators, the `ReadSlackBus` to true (the slack bus defined in each IGM). The tap changers regulation is also set to true (either for phase tap changers than for ratio tap changers), and the power flow must be computed over all connected components. These parameters are chosen to comply with the European merging function. For more information on the power flow parameters available and how to implement them, you can visit this [page](../../simulation/powerflow/openlf.md). In case of non-convergence, these parameters can be relaxed.

### Create parameters to add options to the calculation
Then we define three parameters: a boolean indicating whether we want to perform the load flow pre-processing on the IGMs, a boolean indicating whether we want to prepare the balance computation or not and the name of the synchronous area, set to `10YEU-CONT-SYNC0`, representing Continental Europe.

### Import the CIM-CGMES IGMs
Now we move to the main method.
First, we create the `BalancesAdjustmentValidationParameters` variable, from the class you have created before, to read the paths to the IGMs, the PEVF file and the output directory. You can now start logging into the output directory by creating a specific `log` method taking the `validationParameters` as an argument and checking if the output directory exists or not to set it as the output path.

```java
BalancesAdjustmentValidationParameters validationParameters = BalancesAdjustmentValidationParameters.load();
log(validationParameters);
```
Then we create a special method to import the IGMs outside the Main method. In this method, a HashMap of networks is created and for each IGM path in the validation parameters, the corresponding IGM is loaded through `Importers`. We call this method in Main to import the networks with:
```java
Map<String, Network> networks = importNetworks(validationParameters);
```

### Power flow on the IGMs
Then we compute a power flow on networks in order to have an idea of the valid ones, those for which the load flow is successful. In case of non-convergence of the load flow on an IGM, it is preferable to relax the parameters (tap changer and shunt regulations can be switched off, the reactive power limits of generators can be violated). In our tutorial, we have decided to remove the IGM but it should be the last solution. We create a new method `loadflowPreProcessing`, that runs the load flow via OpenLoadFlow and we add the corresponding code in the main method:
```java
Map<String, Network> validNetworks = new HashMap<>(networks);
if (LOAD_FLOW_PREPROCESSING) {
    loadflowPreProcessing(networks, validNetworks);
}
```

### Merge of the IGMs and power flow on the CGM
Finally, we merge the IGMs to create the CGM:
```java
Network mergedNetwork = Network.merge(validNetworks.values().toArray(Network[]::new));
```
And run a load flow on the CGM. Note that the slack bus is not defined in the CGMES files for the whole CGM, so we prefer to select the most meshed one.
```java
if (!PREPARE_BALANCE_COMPUTATION) {
    LOAD_FLOW_PARAMETERS.setReadSlackBus(false);
    LOAD_FLOW_PARAMETERS.setDistributedSlack(true);
    LoadFlowResult result = LoadFlow.run(mergedNetwork, LOAD_FLOW_PARAMETERS);
    for (Generator gen : mergedNetwork.getGenerators()) {
        gen.setTargetP(-gen.getTerminal().getP());
    }
    System.out.println(result.isOk());
    System.out.println(result.getMetrics());
}
```

### Balance computation
Now we start the balance computation. The PEVF input gives the forecast AC net position of each IGM that has been merged. As it is market data, this net position is the one we should obtain after the load flow, it is the target net position. However, after the load flow, the AC net position of each IGM can be different from the target. The mismatch between what is expected and what is computed has to be balanced via a loop scaling the loads inside the merged area until each net position matches the target one. For that, we use the `powsybl-balances-adjustment` API. In case of partial merge, we also need to scale the net position with the rest of the synchronous area.

First, we create the targets and the `scalables`. `Loads` will be scaled inside the CGM area and `DanglingLines` outside of the CGM.
We import the PEVF file as a `DataExchanges` object. The balances adjustment is done with constant power factor as the reactive power of the loads is adjusted as well (since version 1.6.0 indeed).
```java
List<BalanceComputationArea> balanceComputationAreas = new ArrayList<>();
DataExchanges dataExchanges;
try (InputStream is = Files.newInputStream(Paths.get(validationParameters.getDataExchangesPath()))) {
    dataExchanges = DataExchangesXml.parse(is);
}
```
We now write two methods: `igmPreprocessing` and `prepareFictitiousArea`.
The `igmPreprocessing` method calculates the target AC net position from the PEVF and the actual net position of each control area after the load flow and before the balance computation. To do that, for each network, you need first to retrieve the `CgmesControlArea`. After that, you retrieve the target AC net position from the PEVF file with the method `getNetPosition` applied to `dataExchanges`. Then, you create a `NetworkArea` from the `CgmesControlArea` in order to have access to the utility method that creates scalables (called `createConformLoadScalable`). You have everything to run a balances adjustment. 

The `prepareFictitiousArea` method does the same but for the borders of the CGM. To do so, you need to retrieve the `DanglingLineScalable` of each TSO perimeter through the `CgmesControlArea` and create the fictitious CGMES control area as a `NetworkArea`. Then you can retrieve its AC net position from the PEVF file. You have one more area.

In the main method, you can call these methods through:
```java
if (PREPARE_BALANCE_COMPUTATION) {
    igmPreprocessing(mergedNetwork, validNetworks, dataExchanges, balanceComputationAreas);
    prepareFictitiousArea(mergedNetwork, validNetworks, dataExchanges, balanceComputationAreas);
} else {
    igmPreprocessing(mergedNetwork, validNetworks, dataExchanges);
    prepareFictitiousArea(mergedNetwork, validNetworks, dataExchanges);
}
```

Finally, we create the `BalanceComputationParameters`, launch the balance computation and export the corresponding SV:
```java
if (PREPARE_BALANCE_COMPUTATION) {
    // Create Balance computation parameters.
    BalanceComputationParameters parameters = new BalanceComputationParameters(1, 10);
    LOAD_FLOW_PARAMETERS.setReadSlackBus(false);
    LOAD_FLOW_PARAMETERS.setDistributedSlack(true);
    LOAD_FLOW_PARAMETERS.setBalanceType(LoadFlowParameters.BalanceType.PROPORTIONAL_TO_GENERATION_P_MAX);
    parameters.setLoadFlowParameters(LOAD_FLOW_PARAMETERS);

    // Run the balances ajustment.
    BalanceComputation balanceComputation = new BalanceComputationFactoryImpl()
            .create(balanceComputationAreas, new LoadFlow.Runner(new OpenLoadFlowProvider()), LocalComputationManager.getDefault());
    BalanceComputationResult result = balanceComputation.run(mergedNetwork, mergedNetwork.getVariantManager().getWorkingVariantId(), parameters).join();
    System.out.println(result.getStatus());

      // Generate merged SV file for the CGM.
      validationParameters.getOutputDir().ifPresent(outputDir ->
      exportNetwork(mergedNetwork, Path.of(outputDir), "SV", Set.of("SV")));
}
```

Now, if you run the code and check the output directory, you should get the logs and the SV file.

## Summary
In this tutorial, you have learned how to import multiple CIM-CGMES IGMs and then run a loadflow on them. Your IGMs were then merged and a load flow on the whole CGM was computed. Then you ran a balance adjustment based on market data and exported the SV result file. 

## Going further
The following link can be also useful:
- [Run a power flow with OpenLoadFlow](./loadflow.md): Learn about how to compute a power flow on an IGM