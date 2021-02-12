# Flow transfer tutorial
This tutorial aims to compute flows transfer after a contingency on a small fictive network. This is based on a sensitivity analysis.

# How to install the loadflow simulator  
In the tutorial, we use Hades2, a RTE load-flow tool. Please visit the [Hades2 documentation](https://rte-france.github.io/hades2/index.html) to learn how to install Hades2 on your computer. Note that Hades2 only works on Linux and on Windows for the moment.

# How to configure this tutorial
The configuration file is :
```
<path_to_powsybl_tutorials>/loadflow/complete/src/main/resources/config.yml
```
In the tutorial, you have to configure the path to Hades2. Please visit the [loadflow feature](https://www.powsybl.org/pages/documentation/simulation/powerflow/hades2.html) to configure it.

In the tutorial, you also have to configure the path to a local computation directory (Hades2 temporary files will be stored here). Please visit [this page](https://www.powsybl.org/docs/configuration/modules/computation-local.html) to learn how to do it.

# Running the tutorial
You just need to execute the following command lines :
```
cd <path_to_powsybl_tutorials>/flow-transfert/
mvn package exec:java
```
