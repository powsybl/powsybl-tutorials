# CGMES, merge and security analysis tutorial
In this tutorial, we learn how to import two CGMES files. The CGMES files are from 2 bordering countries: Belgium and Netherlands. These files are imported as 2 different networks and then merged in a single network. The second part of this tutorial aims to compute a security analysis on this network.

Note that the boundary files in CGMES format are stored in each CGMES archive. Coming soon: the boundary files will be stored once and the path will be given by the `config.yml` file.

In order to perform a security analysis, we need a load flow.

# How to install the loadflow simulator  
In the tutorial, we use Hades2, a RTE load-flow tool. Please visit the [Hades2 documentation](https://rte-france.github.io/hades2/index.html) to learn how to install Hades2 on your computer. Note that Hades2 only works on Linux and on Windows for the moment.

# How to configure this tutorial
The configuration file is :
```
<path_to_powsybl_tutorials>/src/main/resources/config.yml
```
In the tutorial, you have to configure the path to Hades2. Please visit the [loadflow feature](https://rte-france.github.io/hades2/features/loadflow.html) to configure it.

In the tutorial, you also have to configure the path to a local computation directory (Hades2 temporary files will be stored here). Please visit [this page](https://www.powsybl.org/docs/configuration/modules/computation-local.html) to learn how to do it.

# Running the tutorial
You just need to execute the following command lines:
```
cd <path_to_powsybl_tutorials>/cgmes/
mvn compile exec:java
```
