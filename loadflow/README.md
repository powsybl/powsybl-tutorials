# Loadflow tutorial
This tutorial aims to compute loadflows on a small fictive network. We first load the network and we compute a loadflow. Secondly, we make a contingency on a line and compute again a loadflow.  


# How to install the loadflow simulator  
In the tutorial, we use the OpenLoadFlow implementation. Please visit this page to learn more about [OpenLoadFlow](https://www.powsybl.org/pages/documentation/simulation/powerflow/openlf.html).

# How to configure this tutorial
The configuration file is:
```
<path_to_powsybl_tutorials>/loadflow/complete/src/main/resources/config.yml
```

# Running the tutorial
You just need to execute the following command lines:
```
cd <path_to_powsybl_tutorials>/loadflow/complete
mvn clean package exec:exec
```
