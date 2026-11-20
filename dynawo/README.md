# Dynamic simulation
This tutorial shows how to write Java code to perform a dynamic simulation with Dynawo.

# How to configure this tutorial
The configuration file is:
```
<path_to_powsybl_tutorials>/dynawo/src/main/resources/config.yml
```

# Running the tutorial
You need to download Dynawo model (see the documentation page for this tutorial) and configure the path to reach it to then run the command.
You need to execute the following command lines:
```
cd <path_to_powsybl_tutorials>/dynawo/
mvn clean compile exec:java \
  -Dexec.args="/path/to/dynawo/dynawo" \
  -Dpowsybl.config.dirs="$(pwd)/src/main/resources"
```

# Outputs
The outputs are stored in the `target/tutorial/` folder.
