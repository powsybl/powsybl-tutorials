# Merging View and SV export of CGM

In this tutorial, we learn how to import IGM CGMES files, merge them, run a loadflow and export a SV file of the CGM composed of these IGM. The CGMES files are from two bordering countries: Belgium and Netherlands.

Note that the boundary files in CGMES format are stored in each CGMES archive.

# How to install the loadflow simulator  
In the tutorial, we use Hades2, a RTE load-flow tool. Please visit the [Hades2 documentation](https://rte-france.github.io/hades2/index.html) to learn how to install Hades2 on your computer. Note that Hades2 only works on Linux and on Windows for the moment.

# How to configure this tutorial
The configuration file is :
```
<path_to_powsybl_tutorials>/merging-view/src/main/resources/config.yml
```
In this tutorial, you have to configure the path to Hades2. Please visit the [loadflow feature](https://rte-france.github.io/hades2/features/loadflow.html) to configure it.

You also have to configure the path to a local computation directory (Hades2 temporary files will be stored here). Please visit [this page](https://www.powsybl.org/pages/documentation/user/configuration/computation-local.html) to learn how to do it.

# Running the tutorial
To run this tutorial, you just need to execute the following command lines:
```
cd <path_to_powsybl_tutorials>/merging-view/
mvn compile exec:java
```
