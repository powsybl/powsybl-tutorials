---
layout: default
---

# Single Line Diagram - Customized node

## Prerequisites

Download and unzip the [source repository](https://github.com/powsybl/powsybl-tutorials), or clone it using Git: `git clone https://github.com/powsybl/powsybl-tutorials`.
In the __sld-custom-node__ subdirectory, you will find a ready-to-run code along with its resources and pom file.

## Maven dependencies

First of all, some Maven dependencies are added in our `pom.xml` file:

* powsybl-single-line-diagram-core (core module of single-line-diagram)
* powsybl-iidm-api and powsybl-iidm-impl (manipulation of networks)
* slf4j-simple (simple logging capabilities)

## Creation of a test network

A very simple Node/Breaker network is created through the function createNetwork().

## Customized NetworkGraphBuilder and ResourcesComponentLibrary

A specific node in the powsybl-single-line-diagram underlying graph is used for all wind-powered generators. This is achieved by creating the graph through a custom NetworkGraphBuilder.
In the generated diagram, the wind-powered generators are then displayed with the chosen wind-turbine icon.

## SVG writing

The final SVG, including the customized icon for the wind turbine, is the following:
![final_svg](img/sld.svg){: width="20%" .center-image}



