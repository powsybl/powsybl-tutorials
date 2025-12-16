# PowSyBl Tutorials

[![Actions Status](https://github.com/powsybl/powsybl-tutorials/workflows/CI/badge.svg)](https://github.com/powsybl/powsybl-tutorials/actions)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=com.powsybl.tutorials%3Apowsybl-tutorials&metric=alert_status)](https://sonarcloud.io/dashboard?id=com.powsybl%3Apowsybl-tutorials)
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)
[![Slack](https://img.shields.io/badge/slack-powsybl-blueviolet.svg?logo=slack)](https://join.slack.com/t/powsybl/shared_invite/zt-36jvd725u-cnquPgZb6kpjH8SKh~FWHQ)

PowSyBl (**Pow**er **Sy**stem **Bl**ocks) is an open source framework written in Java that makes it easy to write complex software for power systems’ simulations and analysis. Its modular approach allows developers to extend or customize its features.

PowSyBl is part of the LF Energy Foundation, a project of The Linux Foundation that supports open source innovation projects within the energy and electricity sectors.

<p align="center">
<img src="https://raw.githubusercontent.com/powsybl/powsybl-gse/main/gse-spi/src/main/resources/images/logo_lfe_powsybl.svg?sanitize=true" alt="PowSyBl Logo" width="50%"/>
</p>

Read more at https://www.powsybl.org!

This project and everyone participating in it is governed by the [PowSyBl Code of Conduct](https://www.lfenergy.org/community/code-of-conduct/). By participating, you are expected to uphold this code. Please report unacceptable behavior to [powsybl.ddl@rte-france.com](mailto:powsybl.ddl@rte-france.com).

## Tutorials

This document describes how to build and run small tutorial projects. For more in-depth explanations on each tutorial, please visit the [documentation](https://powsybl.readthedocs.io/projects/powsybl-tutorials/en/latest/).

## Environment requirements

  * JDK *(21 or greater)*
  * Maven *(3.9.0 or greater)*

Most tutorials show simple code and can be run directly from maven using the `exec:exec@run` goal on them:

```
$ cd <TUTORIAL-FOLDER>
$ mvn compile exec:exec@run
```

Example:
```
$ cd csv-exporter
$ mvn compile exec:exec@run
[INFO] Scanning for projects...
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] Building Export Network to CSV 1.3.0
[INFO] ------------------------------------------------------------------------
[INFO] 
[...snip...]
[INFO] --- exec:3.5.1:java (default-cli) @ csv-exporter ---
[com.powsybl.tutorials.csv.export.Main.main()] INFO com.powsybl.commons.config.PlatformConfig - Using platform configuration provider classic
[com.powsybl.tutorials.csv.export.Main.main()] INFO com.powsybl.commons.config.PlatformConfig - Platform configuration defined by YAML file /home/rolnic/.itools/config.yml
[com.powsybl.tutorials.csv.export.Main.main()] INFO com.powsybl.tutorials.csv.export.CsvLinesExporter - CSV export done in 4 ms
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS

$ cat /tmp/test.csv
LineId,SubstationId1,SubstationId2,VoltageLevelId1,VoltageLevelId2,BusId1,BusId2,R,X,G1,B1,G2,B2
NHV1_NHV2_1,P1,P2,VLHV1,VLHV2,NHV1,NHV2,3.00000,33.0000,0.00000,0.000193000,0.00000,0.000193000
NHV1_NHV2_2,P1,P2,VLHV1,VLHV2,NHV1,NHV2,3.00000,33.0000,0.00000,0.000193000,0.00000,0.000193000
```

Alternatively, you can easily launch a tutorial from the root by using `mvn compile exec:exec@run -pl <TUTORIAL-FOLDER>` or import the projects in your favorite IDE and use its facilities to run code and start experimenting!
