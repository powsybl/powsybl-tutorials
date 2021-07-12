# Merging View and SV export of CGM

In this tutorial, we learn how to import IGM CGMES files, merge them, run a load flow and export updated SSH files and a merged SV file of the CGM composed of these IGM. The CGMES files are from two bordering countries: Belgium and Netherlands.

Note that the boundary files in CGMES format are stored in each CGMES archive.

# How to install the load flow simulator
In the tutorial, we use the OpenLoadFlow implementation. Please visit this [page](https://www.powsybl.org/pages/documentation/simulation/powerflow/openlf.html) for more information about it.

# How to configure this tutorial
The configuration file is:
```
<path_to_powsybl_tutorials>/merging-view/src/main/resources/config.yml
```
In this tutorial, we only need:
```
load-flow:
  default-impl-name: OpenLoadFlow
load-flow-default-parameters:
  voltageInitMode: DC_VALUES
```

# Running the tutorial
To run this tutorial, you just need to execute the following command lines:
```
cd <path_to_powsybl_tutorials>/merging-view/
mvn clean package exec:exec
```