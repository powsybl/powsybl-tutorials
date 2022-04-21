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

# The loadflow simulator  
In the tutorial, we use [powsybl-open-loadflow](https://github.com/powsybl/powsybl-open-loadflow), the powsybl load-flow tool.

# How to configure this tutorial
To configure the loadflow, simply change the LoadFlowParameters passed on when launching the simulator with `LoadFlow.run`.
If you want to use another loadflow implementation, you can use a configuration file to select which implementation you want, as specified in the [loadflow documentation](https://www.powsybl.org/pages/documentation/simulation/powerflow/#configuration).
Or simply ensure that there is only one LoadFlowProvider in the classpath (remove the powsybl-open-loadflow artifact from the pom.xml).

# Running the tutorial
You just need to launch the `SensitivityTutorial::main` with your IDE or execute the following command lines:
```
cd <path_to_powsybl_tutorials>/sensitivity
mvn package exec:java
```
