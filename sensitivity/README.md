# Sensitivity tutorial
This tutorial aims to compute a sensitivity analysis on a small fictive network. 
The network consists of 12 buses:\
    - we are considering schematic exchanges between France, Belgium, Germany and the Netherlands\
    - there are 3 buses per country\
    - there is one phase-shift transformer in Belgium\
    - there are 4 lines we would wish to monitor in this case:\
        - Belgium - France\
        - France - Germany\
        - Germany - the Netherlands\
        - the Netherlands - Belgium\
    - we are monitoring the effect of the phase-shift transformer or of the generators' injections on each of these lines.\
We first load the network and compute a loadflow. This is actually optional, but
makes it possible to print the values of active power at the different nodes of the network. 
Then, we run one sensitivity analysis, and write the results in a json file.
Finally, we define contingencies on each of the lines that are not monitored,
and compute a sensitivity analysis for each of them. The factors' values are printed
in the terminal.

# How to install the loadflow simulator  
In the tutorial, we use Hades2, a RTE load-flow tool. 
Please visit the [Hades2 documentation](https://rte-france.github.io/hades2/index.html) to learn how to install Hades2 on your computer. 
Note that Hades2 only works on Linux and Windows at the moment.

# How to configure this tutorial
The configuration file is :
```
<path_to_powsybl_tutorials>/sensitivity/complete/src/main/resources/config.yml
```
In order to compute a sensitivity analysis on this network,
the sensitivity engine used should be defined in the local config file.
Here we use the Hades2 sensitivity engine, which comes together with the
loadflow. The configuration is thus the same as the loadflow.
Please check the loadflow tutorial for more information on the configuration,
as well as the [loadflow feature page](https://rte-france.github.io/hades2/features/loadflow.html) of the website.

You also have to configure the path to a local computation directory (Hades2 temporary files will be stored here). 
Please visit [this page](https://www.powsybl.org/pages/documentation/user/configuration/computation-local.html) to learn how to do it.

The calculations are done in DC mode, but this can be changed in the 
configuration file by setting dcMode = false in the hades2-default-parameters.


# Running the tutorial
You just need to execute the following command lines:
```
cd <path_to_powsybl_tutorials>/sensitivity/complete
mvn package exec:java
```
