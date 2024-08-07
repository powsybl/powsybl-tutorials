---
layout: default
---

# Write the Java code to perform sensitivity analysis
This tutorial shows you how to write a Java code to perform sensitivity analysis on a network, just on its current state but also on N-1 states by providing a set of contingencies. You'll see how to configure PowSyBl, through a YML file and overwriting it with a JSON file, how to provide input sensitivity factors, and how to output the sensitivity results to a JSON file. Please find [here](https://github.com/powsybl/powsybl-tutorials/tree/main/sensitivity) the code described in this tutorial

* TOC
{:toc}

## What will you build?
You will create a Java code to run a single sensitivity analysis on a network, and also a sensitivity analysis on various network states by providing a set of contingencies. You will use a small fictive network constituted of 12 buses, considering schematic exchanges between France, Belgium, Germany and the Netherlands. The setup is the following:
- 3 buses per country
- one phase-shift transformer in Belgium
- 4 lines to be monitored
  - Belgium - France
  - France - Germany
  - Germany - the Netherlands
  - the Netherlands - Belgium
- they will be monitored with respect to the phase-shift transformer's tap position, or to the generators' injections.

A standard sensitivity analysis input comprises a list of sensitivity factors, each one composed of a sensitivity variable (the variable of impact), a sensitivity function (the observed function) and a contingency context (that could be `NONE`). They correspond to partial derivatives measuring the sensitivity of the observed function (for example the flow through a line) with respect to the variable of impact (for example a phase tap changer tap position or an injection). There are several sensitivity factors available, check the [sensitivity analysis documentation](/pages/documentation/simulation/sensitivity/index.html#sensitivity-factors) for more information. Here we study the effect of the Belgian phase shift transformer on the flow through each of the lines connecting the countries. We thus define these lines as monitored branches, and create one factor (with sensitivity function type `BRANCH_ACTIVE_POWER_1` and sensitivity variable type `TRANSFORMER_PHASE`) for each monitoredBranch. We create the factors twice in order to show different ways to do it:
- once directly with Java code
- once by reading a JSON factors file

You will first run one sensitivity analysis, and write the results in a JSON file. Then, you will define contingencies on each of the lines that are not monitored, and compute a sensitivity analysis for each of them. The factors' values will be printed in the terminal for this second case.

## Configure PowSyBl
We have configured this tutorial to use a locally defined `config.yml` file.
Edit the file named `config.yml` at the location `sensitivity/src/main/resources`.
Start the configuration by writing:
```yaml
load-flow:
  default-impl-name: "OpenLoadFlow"
```
In this way, PowSyBl will be set to use the OpenLoadFlow implementation of the load flow.

You can also add:
```yaml
sensitivity-analysis:
  default-impl-name: "OpenLoadFlow"
```
In this way, PowSyBl will be set to use the OpenLoadFlow implementation of sensitivity analysis.

In order to configure the sensitivity analysis parameters, we need to fill also two sections relative 
to the load flow calculations:
```yaml
load-flow-default-parameters:
    voltageInitMode: DC_VALUES
    transformerVoltageControlOn: false
    dc: false    
```

These parameters will be used by the sensitivity solver: in order to compute the sensitivity values themselves OpenLoadFlow has to perform first a load flow calculation. You can use the `dc` parameter to switch between AC and DC modes sensitivity calculations.

## Import the network from an XML IIDM file

The network we use here is available in the tutorial's resources and is described in the iTesla Internal Data Model format. Start by adding the following lines in the main function of the tutorial:
```java
Network network = Network.read("sensi_network_12_nodes.xml", SensitivityTutorial.class.getResourceAsStream("/sensi_network_12_nodes.xml"));
```
We now have a network in memory.

## Create sensitivity factors through Java code

In order to show how the factors creation work in Java, we start by creating them directly from within the Java code.

First, we need to define which branches (in our case lines) will be monitored. We'll just create a list of `Line`,
and add the ones we wish to monitor in the list by using their IDs:
```java
List<Line> monitoredLines = List.of(network.getLine("BBE2AA1  FFR3AA1  1"),
        network.getLine("FFR2AA1  DDE3AA1  1"),
        network.getLine("DDE2AA1  NNL3AA1  1"),
        network.getLine("NNL2AA1  BBE3AA1  1"));
```
Here we will monitor all the lines that link countries together. The initial flow through each of the monitored lines constitutes the `function reference` values in the sensitivity analysis results. Here, since we did not run a load flow calculation on the network, these flows are not set yet. If you wish to display them, add the following lines in the file (optional):
```java
LoadFlowParameters parameters = new LoadFlowParameters();
LoadFlow.run(network, parameters);
LOGGER.info("Initial active power through the four monitored lines");
for (Line line : monitoredLines) {
    LOGGER.info("LINE {} - P: {} MW", line.getId(), line.getTerminal1().getP());
}
```

To create the factors themselves, we need to create a list in the following way:
```java
List<SensitivityFactor> factors =  monitoredLines.stream()
        .map(l -> new SensitivityFactor(SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, l.getId(),
                                        SensitivityVariableType.TRANSFORMER_PHASE, "BBE2AA1  BBE3AA1  1",
                                        false, ContingencyContext.all())).collect(Collectors.toList());
```
In this provider, we first define the function of interest: here the branch flow (we may also choose current) on the monitored lines. Then we choose to monitor the branch flow with respect to the Belgian phase shift transformer's angle.

## Run a single sensitivity analysis

Now the sensitivity inputs are prepared, we can run a sensitivity analysis. This is done in the following way:
```java
SensitivityAnalysisResult results = SensitivityAnalysis.run(network, factors, Collections.emptyList());;
```
When no variants are explicitly specified, the analysis will be performed on network working variant. Here we directly load the sensitivity analysis parameters from the YML configuration file in the resources. We also pass an empty list to run a simulation without any contingency.

## Output the results in the terminal

We can now print the results in the terminal:
```java
LOGGER.info("Initial sensitivity results");
results.getPreContingencyValues().forEach(value ->
       LOGGER.info("Value: {} MW/°", value.getValue()));
```
The values of the four factors are expressed in MW/°, which means that for a 1° phase change introduced by the phase tap changer, the flow through a given monitored line is modified by the value of the factor (in MW). Here we see that each line will undergo a variation of 25MW for a phase change of 1° introduced by the PST (and -25MW for a -1° change). The four monitored lines are affected identically, because in this very simple network all lines have the same reactance.

## Output the results in a JSON file

It is also possible to output the results to a JSON file. First we will define the path to the file
where we want to write the results.
```java
File jsonResultsFile = File.createTempFile("sensitivity_results_", ".json");
JsonUtil.createObjectMapper()
        .registerModule(new SecurityAnalysisJsonModule())
        .writerWithDefaultPrettyPrinter()
        .writeValue(jsonResultsFile, results);
LOGGER.info("Results written in file {}", jsonResultsFile);
```

## Create a set of contingencies 

We now reach the last part of this tutorial, where we'll run a series of sensitivity calculations on a network, given a list of contingencies and sensitivity factors.

Here the list of contingencies is composed of the lines that are not monitored in the sensitivity analysis.
```java
List<Contingency> contingencies = network.getLineStream()
       .filter(l -> monitoredLines.stream().noneMatch(l::equals))
       .map(l -> Contingency.branch(l.getId()))
       .collect(Collectors.toList());
```

This makes a total of 11 contingencies, which you can check through:
```java
LOGGER.info("Number of contingencies: {}", contingencies.size());
```
and then 
```java
SensitivityAnalysisResult results = SensitivityAnalysis.run(network, factors, contingencies);
```

## Summary
We have learnt how to write Java code to run sensitivity analysis in single mode or in a systematic way, by providing contingencies to the `run` call. We've seen how to create sensitivity factors in Java. We've shown how to set the sensitivity parameters, and how to overwrite them using a JSON file. We've also seen how to output the results, in the terminal but also into a JSON file.

## Going further
The following links could also be useful:
- [Run a sensitivity analysis through an iTools command](../../user/itools/sensitivity-analysis.md): Learn how to perform a sensitivity analysis from the command line 
