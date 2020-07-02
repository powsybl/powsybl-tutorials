# Loadflow tutorial
This tutorial aims to compute loadflows on a small fictive network. We first load the network and we compute a loadflow. Secondly, we make a contingency on a line and compute again a loadflow.  


# How to install the loadflow simulator  
In the tutorial, we use Hades2, a RTE load-flow tool. Please visit the [Hades2 documentation](https://rte-france.github.io/hades2/index.html) to learn how to install Hades2 on your computer. Note that Hades2 only works on Linux and on Windows for the moment.

# How to configure this tutorial
The configuration file is :
```
<path_to_powsybl_tutorials>/loadflow/complete/src/main/resources/config.yml
```
In the tutorial, you have to configure the path to Hades2. Please visit the [loadflow feature](https://rte-france.github.io/hades2/features/loadflow.html) to configure it.

In the tutorial, you also have to configure the path to a local computation directory (Hades2 temporary files will be stored here). Please visit [this page](https://www.powsybl.org/docs/configuration/modules/computation-local.html) to learn how to do it.

# Running the tutorial
You just need to execute the following command lines :
```
cd <path_to_powsybl_tutorials>/loadflow/complete
mvn package exec:java
```
