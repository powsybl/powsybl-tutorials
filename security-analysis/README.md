# Security Analysis tutorial

This tutorial aims to compute security analysis on a network, loading a network Then:
- [x] Make a contingency on a line and compute a security analysis.
- [x] Add operator strategies which contains references for actions
- [x] Add limit reduction parameters
- [x] Add state monitor parameters

The complete tutorial is described in the [security analysis](https://powsybl.readthedocs.io/projects/powsybl-tutorials/en/latest/security-analysis.html) powsybl-tutorials documentation.

# How to install the security analysis simulator
In the tutorial, we use the OpenLoadFlow implementation. Please visit this page: [security analysis](https://powsybl.readthedocs.io/projects/powsybl-core/en/stable/simulation/security/implementations.html) in powsybl-core documentation for more information about it.

# How to configure this tutorial
The configuration file is:
```
<path_to_powsybl_tutorials>/security-analysis/src/main/resources/config.yml
```

# Running the tutorial
You just need to execute the following command lines:
```
cd <path_to_powsybl_tutorials>/security-analysis/
mvn clean package exec:exec@run
```

