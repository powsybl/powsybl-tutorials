# PowSyBl Tutorials

PowSyBl (**Pow**er **Sy**stem **Bl**ocks) is an open source framework written in Java, that makes it easy to write complex software for power systems’ simulations and analysis. Its modular approach allows developers to extend or customize its features.

PowSyBl is part of the LF Energy Foundation, a project of The Linux Foundation that supports open source innovation projects within the energy and electricity sectors.

<p align="center">
<img src="https://raw.githubusercontent.com/powsybl/powsybl-gse/master/gse-spi/src/main/resources/images/logo_lfe_powsybl.svg?sanitize=true" alt="PowSyBl Logo" width="50%"/>
</p>

Read more at https://www.powsybl.org !

This project and everyone participating in it is governed by the [PowSyBl Code of Conduct](https://github.com/powsybl/.github/blob/master/CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code. Please report unacceptable behavior to [powsybl.ddl@rte-france.com](mailto:powsybl.ddl@rte-france.com).


## Tutorials

This document describes how to build and run small tutorial projects. For more in depth explanations on each tutorial, please visit the https://www.powsybl.org/docs/tutorials page.

## Environment requirements

  * JDK *(1.8 or greater)*
  * Maven *(3.3.9 or greater)*

Most tutorials show simple code and can be run directly from maven using the `exec:java` goal on them:

```
$ cd <TUTORIAL-FOLDER>
$ mvn compile exec:java
```

Example:
```
$ cd csv-exporter
$ mvn compile exec:java -pl csv-exporter
[INFO] Scanning for projects...
[INFO]
[INFO] -------------< com.powsybl.tutorials:powsybl-csv-exporter >-------------
[INFO] Building Export Network to CSV 1.0.0
[INFO] --------------------------------[ jar ]---------------------------------
[...snip...]
[INFO] --- exec-maven-plugin:1.6.0:java (default-cli) @ powsybl-csv-exporter ---
[powsybl.tutorials.csv.export.Main.main()] INFO powsybl.tutorials.csv.export.CsvLinesExporter - CSV export done in 8 ms
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS

$ cat /tmp/test.csv
LineId,SubstationId1,SubstationId2,VoltageLevelId1,VoltageLevelId2,BusId1,BusId2,R,X,G1,B1,G2,B2
NHV1_NHV2_1,P1,P2,VLHV1,VLHV2,NHV1,NHV2,3.00000,33.0000,0.00000,0.000193000,0.00000,0.000193000
NHV1_NHV2_2,P1,P2,VLHV1,VLHV2,NHV1,NHV2,3.00000,33.0000,0.00000,0.000193000,0.00000,0.000193000
```

Alternatively, you can easily launch a tutorial from the root by using `mvn compile exec:java -pl <TUTORIAL-FOLDER>` or import the projects in you favorite IDE and use its facilities to run code and start experimenting !

Some tutorials (e.g. javafx-packager) are not about running code but about packaging and therefore have different instructions. Please visit the https://www.powsybl.org/docs/tutorials page to find dedicated instructions.
