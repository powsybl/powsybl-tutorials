# Loadflow tutorial
This tutorial aims to compute loadflows on a small fictive network. We first load the network and we compute a loadflow. Secondly, we make a contingency on a line and compute again a loadflow.  


# How to install the loadflow simulator  
In the tutorial, we use the OpenLoadFlow implementation. Please visit this page to learn more about [OpenLoadFlow](https://powsybl.readthedocs.io/projects/powsybl-open-loadflow/en/latest/).

# How to configure this tutorial
The configuration file is:
```
<path_to_powsybl_tutorials>/loadflow/src/main/resources/config.yml
```

# Running the tutorial
You just need to execute the following command lines:
```
cd <path_to_powsybl_tutorials>/loadflow/
mvn clean package exec:exec@run
```
