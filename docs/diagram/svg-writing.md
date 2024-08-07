---
layout: default
---

# Single Line Diagram - SVG Writing

![Amsterdam_substation](img/svg-writing/example_AmsterdamSubstation.svg)

We are showing in this guide how to create some single line diagrams, like the one above. 
We first generate such a diagram from a test network, then from a [CGMES](../../../grid/formats/cim-cgmes.md) file.
To that end, we use the `com.powsybl.sld.SingleLineDiagram` class, which is the central API of [powsybl-single-line-diagram](../../repositories/powsybl-diagram.md). 

## Prerequisites

### Maven dependencies

First of all, we need some Maven dependencies.

- If you want to get a quick start, please add the [powsybl-starter](https://github.com/powsybl/powsybl-starter) dependency to your pom file:

```xml
<dependency>
    <groupId>com.powsybl</groupId>
    <artifactId>powsybl-starter</artifactId>
    <version>2023.3.0</version>
</dependency>
```

- If you only want to import the strictly needed dependencies for this tutorial, you can write a more detailed pom file:

<details>
<summary>Roll/unroll dependencies</summary>

{% highlight xml %}

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
        <artifactId>powsybl-single-line-diagram-core</artifactId>
    </dependency>
    <dependency>
        <groupId>com.powsybl</groupId>
        <artifactId>powsybl-iidm-impl</artifactId>
    </dependency>
    <dependency>
        <groupId>com.powsybl</groupId>
        <artifactId>powsybl-iidm-test</artifactId>
    </dependency>
    <dependency>
        <groupId>com.powsybl</groupId>
        <artifactId>powsybl-cgmes-conversion</artifactId>
    </dependency>
    <dependency>
        <groupId>com.powsybl</groupId>
        <artifactId>powsybl-triple-store-impl-rdf4j</artifactId>
    </dependency>
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-simple</artifactId>
        <version>${slf4j.version}</version>
    </dependency>
</dependencies>

<properties>
    <powsybl-dependencies.version>2023.3.0</powsybl-dependencies.version>
    <slf4j.version>1.7.22</slf4j.version>
</properties>

{% endhighlight %}

<div markdown="1">
Here are some details about these dependencies (see also the [powsybl artifacts documentation page](../../artifacts.md)):
- `powsybl-single-line-diagram-core` is the core module of single-line-diagram,
- `powsybl-iidm-impl` is used to deal with the network model,
- `powsybl-iidm-test` is used to load the test network,
- `powsybl-cgmes-conversion` and `powsybl-triple-store-impl-rdf4j` are used to import a CGMES file,
- `slf4j-simple` allows you to have simple logging capabilities.
</div>

</details>



## Diagrams from a test network
We first create the node/breaker test `Network` we are interested in:
```java
Network network = FictitiousSwitchFactory.create();
```

### Generating a voltage level diagram
We can generate the SVG diagram file for voltage level `N` with a single line of code:
```java
SingleLineDiagram.draw(network, "N", "/tmp/n.svg");
```

Note that we could also use a method specific for voltage levels: 

```java
SingleLineDiagram.drawVoltageLevel(network, "N", "/tmp/n.svg");
```

We obtain the following SVG:

![N_voltageLevel](img/svg-writing/example_n.svg){: width="70%" .center-image}

Similarly, we could generate a SVG for voltage level `C`:
 
```java
SingleLineDiagram.draw(network, "C", "/tmp/c.svg");
```
 
leading to the following diagram:

![C_voltageLevel](img/svg-writing/example_c.svg){: width="18%" .center-image}

### Generating the substation diagram
In order to build the diagram for the whole substation, named `A`, containing both voltage levels displayed previously, we may use the same interface:
```java
SingleLineDiagram.draw(network, "A", "/tmp/a.svg");
```

Similarly to before, there is also a method specific for substations which we could have used:
```java
SingleLineDiagram.drawSubstation(network, "A", "/tmp/a.svg");
```

In both cases, we obtain the following wider SVG file:

![A_substation](img/svg-writing/example_a.svg){: width="85%" .center-image}

## Diagrams from a CGMES file

First of all, we need to download a sample file from ENTSO-E [here](CGMES_v2_4_15_MicroGridTestConfiguration_T4_Assembled_NB_Complete_v2.zip)

This file is named `CGMES_v2_4_15_MicroGridTestConfiguration_T4_Assembled_NB_Complete_v2.zip`.

We first import this sample `Network` we are interested in:
```java
String file = "/path/to/file/CGMES_v2_4_15_MicroGridTestConfiguration_T4_Assembled_NB_Complete_v2.zip";
Network network = Network.read(file);
```

### Generating a voltage level diagram
Once the network is loaded, we can generate diagrams like in previous section.
We first generate a SVG for the voltage level named `110` in substation `PP_Brussels` (corresponding id is `_8bbd7e74-ae20-4dce-8780-c20f8e18c2e0`). 
Note that, as the ids are not very human-readable, we customize the parameters to have the names displayed instead of the ids.
Therefore, we use the slightly more complex interface `SingleLineDiagram.draw(network, id, path, parameters)`.

```java
// Use custom parameters to have the names displayed instead of the ids
SvgParameters svgParameters = new SvgParameters().setUseName(true);
SldParameters sldParameters = new SldParameters().setSvgParameters(svgParameters);
// Draw the diagram of voltage level 110 in substation PP_Brussels (id _8bbd7e74-ae20-4dce-8780-c20f8e18c2e0)
SingleLineDiagram.draw(network, "8bbd7e74-ae20-4dce-8780-c20f8e18c2e0", Paths.get("/tmp/Brussels110.svg"), sldParameters);
```

We obtain the following SVG:

![Brussels_voltageLevel](img/svg-writing/example_Brussels110.svg){: width="40%" .center-image}

### Generating a substation diagram
Similarly to voltage level diagrams, we can generate substation diagrams. 
We generate the SVG diagram for the substation called `PP_Amsterdam`, which is containing four voltage levels. 
We customize a bit further the parameters: the feeder names in this substation are quite long, hence we rotate them to avoid overlapping.

```java
// Customize further the parameters to have the feeders label rotated, in order to avoid overlapping
svgParameters.setLabelDiagonal(true);

// Draw the diagram of substation PP_Amsterdam (id _c49942d6-8b01-4b01-b5e8-f1180f84906c)
SingleLineDiagram.draw(network, "c49942d6-8b01-4b01-b5e8-f1180f84906c", Paths.get("/tmp/AmsterdamSubstation.svg"), sldParameters);
```

We then obtain the following SVG file representing the whole PP_Amsterdam substation with its three voltage levels:

![Amsterdam_substation](img/svg-writing/example_AmsterdamSubstation.svg)

That's it, you are now able to generate diagrams for substations and voltage levels! You can now try to change the default layout settings by reading the next guide [SVG Layouts]().

