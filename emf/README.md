# European Merging Function tutorial

In this tutorial, we will learn how to import multiple IGMs in CIM-CGMES format, merge them and compute a load flow on the resulting CGM. Then we will learn how to balance the AC net positions of the merged control areas and export the results in a SV file. For full documentation, please visit [this page](TODO).

# How to install the loadflow simulator  
In the tutorial, we use the OpenLoadFlow implementation. Please visit this page to learn more about [OpenLoadFlow](https://powsybl.readthedocs.io/projects/powsybl-open-loadflow/en/latest/).

# How to configure this tutorial
The configuration file is:
```
<path_to_powsybl_tutorials>/emf/src/main/resources/config.yml
```

# Running the tutorial
You just need to execute the following command lines:
```
cd <path_to_powsybl_tutorials>/emf
mvn clean package exec:java
```