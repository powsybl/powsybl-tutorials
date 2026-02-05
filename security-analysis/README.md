# Security Analysis tutorial

This tutorial aims at computing security analysis on a network. After loading a network then:
- [x] Create a contingency on a line and compute a security analysis.
- [x] Create an operator strategy containing references to actions.
- [x] Create a limit reduction.
- [x] Add a state monitor to monitor elements on the network.

The complete tutorial is described in the [security analysis](https://powsybl.readthedocs.io/projects/powsybl-tutorials/en/latest/security-analysis.html) powsybl-tutorials documentation.

# How to install the security analysis simulator
In the tutorial, we use the OpenLoadFlow implementation. Please visit this page: [security analysis](https://powsybl.readthedocs.io/projects/powsybl-core/en/stable/simulation/security/index.html#implementation) in powsybl-core documentation for more information about it.

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

