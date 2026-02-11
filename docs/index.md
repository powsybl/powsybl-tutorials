---
layout: default
---

```{toctree}
---
maxdepth: 2
hidden: true
---
intellij.md
iTools<itools/index.md>
IIDM import/export<iidm/index.md>
Load flow<loadflow.md>
Sensitivity analysis<sensitivity-analysis.md>
Security analysis<security-analysis.md>
Topology<topology.md>
Diagrams<diagram/index.md>
Downscaling<downscaling.md>
Network modifications in Groovy<network_modifications_groovy.md>
```

# Tutorials

## IDE configuration
- [IntelliJ](intellij.md): Setup IntelliJ to work with the PowSyBl tutorials

## iTools
- [Bundle an iTools package](itools/itools-packager.md): Learn how to use the `itools-packager` maven plugin
- [Create an iTools command](itools/itools-command.md): Learn how to create your own `iTools` command in Java

## Simulation
- [Create the Java code to run power flows](loadflow.md): Learn how to create the Java code to setup and run power flows
- [Create the Java code to run sensitivity analyses](sensitivity-analysis.md): Learn how to create the Java code to setup and run sensitivity analyses
- [Security analyses](security-analysis.md): Learn how to use security analyses

## Topology
- [Manage bus/breaker and node/breaker topology](topology.md): Learn how to manipulate topological views of the network

## Diagram
- [Display a single-line diagram](diagram/svg-writing.md): Learn how to create the Java code to generate the svg file of a single-line diagram
- [Display a single-line diagram with a customized node](diagram/sld-custom-node.md): Learn how to create the Java code to generate the svg file of a single-line diagram with a customized node
- [Display a network-area diagram](diagram/nad-svg-writing.md): Learn how to create the Java code to generate the svg of a network-area diagram

## Downscaling
- [Create the Java code to map hypothesis on a network](downscaling.md): Learn how to create the Java code to map study state hypothesis on a network

## Network modifications scripts in groovy
- [Modify a network through groovy scripts](network_modifications_groovy.md): Learn how to use Powsybl network modification functions in Groovy.