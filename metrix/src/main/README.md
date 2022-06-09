# Powsybl-METRIX tutorial
author:
- Mathilde Bongrain
  title: "**TP Metrix - 6 node network - Statement**"
---

**Objectives of the practical work:** On a very simple network (6 workstations), to appropriate
Metrix settings and outputs, and knowing how to explain the
results obtained.

## What do you need ?
- About 4 hours
- A UNIX environment
- git (to be able to clone the repository)
- ...and that's it.

## How to install metrix
In this chapter we will install Powsybl-METRIX on your environment. You can skip this part if Powsybl-METRIX is already set up.
1. **Clone the repository** : open a terminal in the directory of your choice, and enter the following command line :
   <code> git clone https://github.com/powsybl/powsybl-metrix.git </code>
2. **Install Metrix** : go in the cloned repository, and run the script `./install.sh`.
   When you are asked if you want a full or a metrix installation, select "full". When you are asked if you want to add Powsybl-metrix to the path, select "yes".

## 1- Network presentation

The network used for this lab consists of 6 stations, all connected
by two parallel lines with the same electrotechnical characteristics
(same resistance and same reactance for each line), as well as two
HVDC and a TD. It also features 4 groups and three loads.\
![image](images/reseau_6_noeuds_ss_HDVC.png)

The network is described in an "iidm" format (which is the native Powsybl network representation format).
It can be found in the folder `src/main/resources/3A/data/reseau_6noeuds.xiidm`.

in this TP,each station and line's name refers to cardinal points:
- NO: North West
- N: North
- NE: North East
- SE: South East
- S: South
- SO: South West

# 2- To get started:
Along with the network file, you can find a timeseries file at the path :
`src/main/resources/3A/data/ts/time-series-tp.csv`. It will contains time series to map to each demand and/or 
unavoidable production of the network.

Note : These two files (network file and timeseries file) are the same used in all exercises of the tutorial.

In mapping output you should have the following network:\
![image](images/result_mapping_ss_HDVC.png)


# 3- Load Flow mode: Understanding flows

The load flow Metrix allows the calculation of active-only flows on structures in N and N-1 based on network
information (topology, and electrotechnical characteristics), production and load timeseries and a list of contingencies.
It does not  optimize anything. To launch a load flow, it is necessary:
![image](images/mode_LF_fichiers.png)
The multi-situation contrains the network information and the timeseries. The Metrix configuration script is a script 
that allows to hold all parameters and options of the simulation (in particular the calculation mode, the cyhoixes of
modeling of the structures and the data tro be acquired in output). The contingencies script denotes the list of defects and the 
options related to them.




### Action 3A - Launch in simple LF mode

#### Goal:

We want to observe the flows on all the structures of our
network in the nominal case (N) and when line S_SO_S1 is removed (N-1).

#### In practice

- Create a Metrix configuration script in which we declare
  want results on all the works of the network (if necessary,
  see syntax below).

- Create a list of defects containing only the N-1 on the work
  S_SO_1 (if needed, see syntax below).

- Create a Metrix calculation that points to the defined multi-situation
  previously, the configuration script and the list of contingencies
  (see reminder of the previous section if necessary).

- Launch the calculation and note the flows on the structures on the
  different time steps.

[//]: # ()
[//]: # (#### Syntax help:)

[//]: # ()
[//]: # (Results on all network works:[link)

[//]: # (wiki]&#40;https://wikicvg.rte-france.com/xwiki/bin/view/imaGrid/4.+Configure+and+launch+Metrix#HOuvragessurveillE9setouvragesavecrE9sultats&#41;)

[//]: # ()
[//]: # (Create a defect list:[link)

[//]: # (wiki]&#40;https://wikicvg.rte-france.com/xwiki/bin/view/imaGrid/4.+Configure+and+launch+Metrix#HListed27incidents&#41;)

### Action 3A - Launch in single LF mode - Fix

#### Scripts:

Metrix configuration script: (conf.groovy)

    for (l in network. branches) {
       branch(l.id) {
         baseCaseFlowResults true
         maxThreatFlowResults true
       }
    }

Contingencies script: (contingencies.groovy)

    contingency('S_SO_1') { equipments 'S_SO_1'}

#### Results and Analysis:

You must obtain the following result maps: flow map
in N on the left and in N-1 on the right.

![image](images/result_LF_simple.png)

More specifically, on the S_SO_2 structure in N and N-1 on the various
no time, we get:\

| Ts    | FLOW_S\_SO_2 | MAX_THREAT_1\_FLOW_S\_SO_1 | MAX_THREAT_1\_FLOW_S\_SO_2 |
|-------|--------------|----------------------------|----------------------------|
| T01   | -290.5       | -484.2                     | S_SO_1                     |
| T02   | -290.5       | -484.2                     | S_SO_1                     |
| T03   | -290.5       | -484.2                     | S_SO_1                     |


In N, as the groups and the consumptions are south, the
network is less impedant and we see that the flows pass through it
mostly. There is also flow on the TD, and to the north. The
flows are the same on the 3 time steps because the only difference
is that production moves from group SO_G2 to group S0_G1 which are on
the same post.

### Action 3B - Monitor S_SO_2

#### Goal:

We want to monitor certain structures (here S_SO_2), i.e.
observe flows and threshold overruns only on
the supervised structure. This reduces the amount of results to
analyze.

#### In practice

- Modify the Metrix configuration script to monitor the structure
  S_SO_2 in N and on incidents.

- Declare as threshold in N and N-k the timeseries 'thresholdN' provided in
  the set of entry timeseries (see syntax).

- No longer ask for results on other works to reduce
  reading the results later.

- Launch the calculation and analyze the new results.

#### Syntax help:

Declare a threshold:[link
wiki](https://wikicvg.rte-france.com/xwiki/bin/view/imaGrid/4.+Configure+and+launch+Metrix#HOuvragessurveillE9setouvragesavecrE9sultats)
Description of Metrix results: [link
wiki](https://wikicvg.rte-france.com/xwiki/bin/view/imaGrid/4.+Configure+and+launch+Metrix#HSortiesdeMetrix)

### Action 3B - Monitor S_SO_2 - Fix

#### Scripts:

Metrix setup script:

    branch('S_SO_2') {
       baseCaseFlowResults true // results in N
       maxThreatFlowResults true // results on N-k
       branchRatingsBaseCase 'thresholdN' // threshold in N
       branchRatingsOnContingency 'thresholdN' // threshold in N-k
    }

#### Results and Analysis:

You need to get new columns in the output file
OVERLOAD\_ BASECASE (which represent the difference between the flow in N
and N-1 on the work) and OVERLOAD_OUTAGES (which represent the
difference between the flow in N-1 and the threshold).

| Ts    | OVERLOAD_BASECASE | OVERLOAD_OUTAGES | 
|-------|-------------------|------------------|
| T01   | 0                 | 84.2             | 
| T02   | 0                 | 84.2             | 
| T03   | 190.5              | 384.2            | 


There is no constraint in N on the first two time steps then
that there are on the third. This is due to the change in the value of the
threshold from 400 to 100 MW in the 'thresholdN' timeseries. There are
overruns on all time steps in N-1.

## OPF mode without redispatching: Optimize RTE's manual actions

### Action 4A - Use topological countermeasures to solve constraints

#### Goal:

We want to see if RTE's actions are sufficient to
solve the constraints previously studied. For this, we propose
in a first to offer the use of topological parades. In our
case, which topological parades (opening of line or passage to two
nodes in a post) could be effective?

#### In practice:

- In the \"Metrix simulation\" object, define the following 4 parries
  on the S_SO_1 contingency (see syntax):

    - opening of the circuit breaker SS1_SS1_DJ_OMN (this goes to two nodes
      post S)

    - opening of circuit breaker SOO1_SOO1_DJ_OMN (this goes to two
      nodes the post of SO)

    - opening of the two circuit breakers above

    - opening of the S_SO_2 line

- Configure in the Metrix script the launch below and observe
  the activations of parries at the different time steps as well as
  the evolution of the constraints (cf description of the Metrix results) .

#### Syntax help:

Definition of a contingency response:[link
wiki](https://wikicvg.rte-france.com/xwiki/bin/view/imaGrid/4.+Configure+and+launch+Metrix#HFichierdeparadestopologiques)
Description of Metrix results: [link
wiki](https://wikicvg.rte-france.com/xwiki/bin/view/imaGrid/4.+Configure+and+launch+Metrix#HSortiesdeMetrix)

### Action 4A - Use topological countermeasures to solve constraints - Fix

#### Scripts:

Parry files

    NB;4;
    S_SO_1;1;SS1_SS1_DJ_OMN;
    S_SO_1;1;SOO1_SOO1_DJ_OMN;
    S_SO_1;2;SS1_SS1_DJ_OMN;SOO1_SOO1_DJ_OMN;
    S_SO_1;1;S_SO_2;

Metrix configuration file:

    parameters {computationType OPF_WITHOUT_REDISPATCHING}

    branch('S_SO_2') {
       baseCaseFlowResults true // results in N
       maxThreatFlowResults true // results on N-k
       branchRatingsBaseCase 'thresholdN' // threshold in N
       branchRatingsOnContingency 'thresholdN' // threshold in N-k
    }

#### Results and Analysis:

On the first time step, Metrix moves the SO substation to two nodes, this
which \"extends the path\" passing through the constrained structure and allows
remove the constraint completely.

On time steps 2 and 3, this parry does not work because the
production is on the other node of the SO post. Metrix then chooses the
parade which consists in opening the work in constraint. He had not
retained this parry on the first time step because it was declared in
below in the lTranslation types
Text translation
Source text
5,000 / 5,000
Translation results
list of parries: with identical effectiveness, the parries
are chosen in the order of the list.

On the 3rd time step, the constraint in N remains unchanged.

### Action 4B - Use a phase-shifting transformer

#### Goal:

The goal here is to see if the use of the phase-shifting transformer allows
alone to solve the constraints. The phase shifts of the TDs are by
contingencies set to their value in the multi-situation (choice of socket),
you can ask Metrix to optimize them preventively and on certain
incidents. What sign of phase shift would relieve the
N and N-K constraints identified? Positive which slows down the flow between NO
and NE or negative which accentuates the flow between NO and NE

#### In practice:

In the Metrix configuration file:

- authorize the TD NE_NO_1 to move preventively and on the contingency
  S_SO_1 (see syntax).

- Launch the calculation without parades

- Observe the actions on the TD and the evolution of the constraints (cf
  description of Metrix results).

For the rest of the tutorial, we will no longer use the TD, remember to
remove from the Metrix configuration for the next steps.

#### Syntax help:

Authorization of a TD to move in preventive and / or curative: [link
wiki](https://wikicvg.rte-france.com/xwiki/bin/view/imaGrid/4.+Configure+and+launch+Metrix#HTransfo-dE9phaseurs)
Description of Metrix results: [link
wiki](https://wikicvg.rte-france.com/xwiki/bin/view/imaGrid/4.+Configure+and+launch+Metrix#HSortiesdeMetrix)

### Action 4B - Use phase-shifting transformer - Fix

#### Scripts:

Metrix configuration file:

    parameters {
        computationType OPF_WITHOUT_REDISPATCHING
    }

    branch('S_SO_2') {
       baseCaseFlowResults true // results in N
       maxThreatFlowResults true // results on N-k
       branchRatingsBaseCase 'thresholdN' // threshold in N
       branchRatingsOnContingency 'thresholdN' // threshold in N-k
    }

    phaseShifter('NE_NO_1') {
      controlType OPTIMIZED_ANGLE_CONTROL
      onContingency 'S_SO_1'
    }

#### Results and Analysis:

By launching the OPF_WITHOUT_REDISPATCHING calculation without parade, we observe
that Metrix plays on the phase shift of the TD in curative on all the steps of
time and preventively on the third time step. Constraints
are fully lifted:\

Result T01 T02 T03
  --------------- ------- ------- -------
PST_CUR_NE_NO_1\_S_SO_1 -0.32 -0.32 -1.01
PST_NE_NO_1 // -0.75
PST_CUR_TAP_NE_NO_1\_S_SO_1 1 1 1
PST_TAP_NE_NO_1 // 1
OVERLOAD_BASECASE 0 0 0
OVERLOAD_OUTAGES 0 0 0

## OPF mode: Optimize all actions

### Action 5A - Configure adjustable groups in preventive

#### Goal:

The goal here is to take care of the residual stresses after the
\"free\" actions that constitute the parries (it is assumed that the TD
is not available/does not exist). We recall that in part 4A,
the parades had not been able to resolve the constraints as a preventive measure. We
therefore proposes to see if the use of groups as a preventive measure makes it possible to
resolve these constraints. At what power of the groups can we
to expect? preventive or curative?

#### In practice:

- Define in the Metrix configuration file that all groups
  can move in preventive with costs upwards of 100 and at
  decreasing by 1 (see syntax)

- Change simulation mode to \"OPF\", resume parries, and
  start the calculation

- Observe the actions taken by Metrix (see description of the results
  Metrix) whose cost of redispatching

NB: the SE_G group has a Pmax of 600MW

#### Syntax help:

Configure groups:[link
wiki](https://wikicvg.rte-france.com/xwiki/bin/view/imaGrid/4.+Configure+and+launch+Metrix#HGE9nE9rateurs)
Description of Metrix results: [link
wiki](https://wikicvg.rte-france.com/xwiki/bin/view/imaGrid/4.+Configure+and+launch+Metrix#HSortiesdeMetrix)

### Action 5A - Configure adjustable groups in preventive - Fix

#### Scripts:

Metrix configuration file:

    parameters {computationType OPF}

    branch('S_SO_2') {
       baseCaseFlowResults true // results in N
       maxThreatFlowResults true // results on N-k
       branchRatingsBaseCase 'thresholdN' // threshold in N
       branchRatingsOnContingency 'thresholdN' // threshold in N-k
    }

    for (g in ['SO_G1','SE_G','SO_G2','N_G']) {
     generator(g) {
       redispatchingDownCosts 1
       redispatchingUpCosts 100
     }
    }

#### Results and Analysis:

By launching the OPF calculation with the previous parades and the groups in
preventive, we can clearly see that Metrix used the parries in priority
on the curative and makes it possible to solve the constraints in N-k. For the
deviations in N observed at the 4C action, he is forced to use the
preventive groups (because parries cannot act in N). There are
so redispatchinTranslation types
Text translation
Source text
5,000 / 5,000
Translation results
g on the 3rd time step; Metrix chooses to
lower the unit to SO which delivers on the structure under stress and
mount the one to SE which is on the same post as the load. He rides it
up to its Pmax but must then pick up the group on the station
N. The cost of redispatching is equal to the volumes called multiplied by
their costs.

In the end, there are no constraints left (this is always the case in mode
OPF) and the flow on the monitored structure is reduced to exactly 100MW
in N on the 3rd time step.

Result T01 T02 T03
  ------------------- ----- ----- ---------
OVERLOAD_BASECASE 0 0 0
OVERLOAD_OUTAGES 0 0 0
GEN_COST 0 0 67695.2
GEN_SO_G1 // -670.2
GEN_SE_G // 600
GEN_N\_G // 70.2

### Action 5B - Configure adjustable groups in healing

#### Goal:

The goal here is to see how curative redispatching operates in
relation to preventive redispatching. In order to see their use, it
is necessary to remove parries that have zero cost and are
therefore priority over any costly parry.

#### In practice:

- Configure adjustable groups in healing

- Specify that groups can also act on the incident
  'S_SO_1' (see syntax)

- Remove the use of parades, and launch the calculation

- Observe the actions taken by Metrix on the groups in preventive
  and curative, as well as the cost of redispatching

#### Syntax help:

Configure groups:[link
wiki](https://wikicvg.rte-france.com/xwiki/bin/view/imaGrid/4.+Configure+and+launch+Metrix#HGE9nE9rateurs)

### Action 5B - Configure Adjustable Groups in Healing - Fix

#### Scripts:

Metrix configuration file:

    parameters {computationType OPF}

    branch('S_SO_2') {
       baseCaseFlowResults true // results in N
       maxThreatFlowResults true // results on N-k
       branchRatingsBaseCase 'thresholdN' // threshold in N
       branchRatingsOnContingency 'thresholdN' // threshold in N-k
    }

    for (g in ['SO_G1','SE_G','SO_G2','N_G']) {
     generator(g) {
       redispatchingDownCosts 1
       redispatchingUpCosts 100
       onContingency 'S_SO_1'
     }
    }

#### Results and Analysis:

Keeping the parries, adding healing adjustments does not modify
the results. In fact, the parades make it possible to remove the constraints
at almost zero cost compared to group adjustments, they are therefore
implemented on a priority basis.

If we remove the parades, Metrix also performs redispatching
curative on the first two time steps. On the 3rd, it does not
of curative adjustment because preventive adjustment already makes it possible to prevent
Incident constraints.

Result T01 T02 T03
  ----------------------- -------- -------- ---------
GEN_COST 0 0 67688.1
GEN_SO_G1 // -670.2
GEN_SE_G // 600
GEN_N\_G // 70.2
GEN_CUR_SO_G1_S\_SO_1 / -168.5 -266.7
GEN_CUR_SO_G2_S\_SO_1 -168.5 / /
GEN_CUR_SE_G\_S_SO_1 168.5 168.5 /
GEN_CUR_N\_G_S\_SO_1 // 266.7

### Action 5C - Remove essential group from healing

#### Goal:

The goal here is to see how Metrix will react when you remove a
essential group of redispatching in curative.

#### In practice:

- resume the same configuration as the previous action

- remove group N_G from adjustable groups

- resume topological parades

- Observe the actions taken by Metrix on the groups

### Action 5C - Remove essential group from healing - Correction

#### Scripts:

Metrix configuration file:

    parameters {computationType OPF}

    branch('S_SO_2') {
       baseCaseFlowResults true // results in N
       maxThreatFlowResults true // results on N-k
       branchRatingsBaseCase 'thresholdN' // threshold in N
       branchRatingsOnContingency 'thresholdN' // threshold in N-k
    }

    for (g in ['SO_G1','SE_G','SO_G2']) {
     generator(g) {
       redispatchingDownCosts 1
       redispatchingUpCosts 100
       onContingency 'S_SO_1'
     }
    }

Parries file to take into account.

#### Results and Analysis:

Once SE_G's group is raised to Pmax, Metrix has no other group
available to compensate for the decrease in the group of the SO post. He cuts
then 35MW of consumption at SE.

Result T01 T02 T03
  ------------------ ------------------ -------- ------ ----
FLOW_S\_S0_2 -290.5 -290.5 -100
LOAD_COST 0 0 386416.7
LOAD_SE_L1 // 35.1
GEN_SO_G2 // -635.1
GEN_SE_G // 600
TOPOLOGY_S\_SO_1 SOO1_SOO1_DJ_OMN S_SO_2 S_SO_2

### Action 5D - Authorize consumption in advance
Translation types
Text translation
Source text
5,000 / 5,000
Translation results
windy

#### Goal:

The goal here is to see how Metrix will solve the constraints
when preventive load shedding is authorized on SO and only the
groups SE (Pmax)600) and SO?

#### In practice:

- repeat the same configuration as the previous action (still in
  removing the N_G group from the adjustable groups, and without parades)

- authorize consumptions 'SO_L' to move preventively (see
  syntax)

- Observe the actions taken by Metrix on the groups and the
  consumption

#### Syntax help:

Configure consumptions:[link
wiki](https://wikicvg.rte-france.com/xwiki/bin/view/imaGrid/4.+Configure+and+launch+Metrix#HConsummations)

### Action 5D - Authorize preventive consumption - Correction

#### Scripts:

Metrix configuration file:

    parameters {computationType OPF}

    branch('S_SO_2') {
       baseCaseFlowResults true // results in N
       maxThreatFlowResults true // results on N-k
       branchRatingsBaseCase 'thresholdN' // threshold in N
       branchRatingsOnContingency 'thresholdN' // threshold in N-k
    }

    for (g in ['SO_G1','SE_G','SO_G2','N_G']) {
     generator(g) {
       redispatchingDownCosts 1
       redispatchingUpCosts 100
       onContingency 'S_SO_1'
     }
     
     load('SO_L') {
    preventiveSheddingPercentage 100
    }

    }

No parade file.

#### Results and Analysis:

In this case, an error code 1 is observed on the third time step.\
Indeed, we saw just before that Metrix had to resort to load shedding
to SE to find a solution, but this load shedding is not authorized here
explicitly but another one that is not useful. With the means
allowed, Metrix cannot find a solution that respects the thresholds of the
works, it returns an error.\
This example illustrates the importance of the configuration of the means
of action. It must be a good balance between too many means
of actions (if too many are authorized, the calculation times lengthen and
understanding of the results is difficult) and not enough (there is a risk
then not to have a solution to the problem).

### Action 5E - Authorize curative consumption

#### Goal:

The goal here is to see how Metrix will solve the constraints
when faced with several options. Will he prefer the action of
the SE_L1 consumption in curative and preventive or the action of the groups
SO_G1, SO_G1, SE_G and N_G?

#### In practice:

- authorize groups SO_G1, SO_G1, SE_G and N_G

- do not allow parries

- authorize consumption 'SE_L1' to move in preventive and curative mode
  (see syntax)

- Observe the actions taken by Metrix on the groups and the
  consumption

#### Syntax help:

Configure consumptions:[link
wiki](https://wikicvg.rte-france.com/xwiki/bin/view/imaGrid/4.+Configure+and+launch+Metrix#HConsummations)

### Action 5E - Authorize curative consumption - Correction

#### Scripts:

Metrix configuration file:

    parameters {computationType OPF}

    branch('S_SO_2') {
       baseCaseFlowResults true // results in N
       maxThreatFlowResults true // results on N-k
       branchRatingsBaseCase 'thresholdN' // threshold in N
       branchRatingsOnContingency 'thresholdN' // threshold in N-k
    }

    for (g in ['SO_G1','SE_G','SO_G2','N_G']) {
     generator(g) {
       redispatchingDownCosts 1
       redispatchingUpCosts 100
       onContingency 'S_SO_1'
     }
     
    load('SE_L1') {
    curativeSheddingPercentage 100
    curativeSheddingCost 10
    onContingency 'S_SO_1'
    }

    }

No parade file.

#### Results and Analysis:

As a reminder, before this modification, without the parades, Metrix had to
make healing group adjustments on the first two steps of
time: he lowered the group to SW and raised the group to SE. The
results by allowing the consumption of SE to move with a cost of
10 euros/MW are:

Result T01 T02 T03
  --------- -------- -------- --------
GEN_CUR_SO_G1_S\_SO_1 / -168.5 -133.3
GEN_CUR_SO_G2_S\_SO_1 -168.5 / /
LOAD_CUR_SE_L1_S\_SO_1 -168.5 -168/5 -133.3
GEN_SO_G1 // -670.2
GEN_SE_G // 600
GEN_N\_G // 70.2

We see that Metrix then prefers to lower the consumption to SE rather
that mount the SE group on the first two time steps in
curative. Indeed, we have fixed the cost of the groups at 100 euros
against 10 euros for the drop in consumption.

### Action 5F - Configure thresholds before maneuver

#### Goal:

The aim here is to see how the definition of a threshold before maneuver
can modify the actions chosen by Metrix.

#### In practice:

- add a threshold of 480 before healing on S_SO_2

- authorize groups SO_G1, SO_G1, SE_G and N_G in preventive and
  curative

- do not allow parries

- do not define a preventive or curative consumption parade

-   Compare the actions taken by Metrix in relation to the 5E action.

#### Syntax help:

Configure monitored sections:[link
wiki](https://wikicvg.rte-france.com/xwiki/bin/view/imaGrid/4.+Configure+and+launch+Metrix#HSectionssurveillE9es)

### Action 5F - Configure the thresholds before maneuver - Correction

#### Scripts:

Metrix configuration file:

    parameters {
      computationType OPF
      preCurativeResults true
    }

    branch('S_SO_2') {
       baseCaseFlowResults true // results in N
       maxThreatFlowResults true // results on N-k
       branchRatingsBaseCase 'thresholdN' // threshold in N
       branchRatingsOnContingency 'thresholdN' // threshold in N-k
       branchRatingsBeforeCurative 480 //threshold before curative
    }


    for (g in ['SO_G1','SE_G','SO_G2','N_G']) {
     generator(g) {
       redispatchingDownCosts 1
       redispatchingUpCosts 100
       onContingency 'S_SO_1'
     }
    }

No parade file.

#### Results and Analysis:

Activating the threshold before maneuver without defining it leads to the appearance
a new column \"MAX_TMP_THREAT_FLOW_S\_SO_2\" which contains the
worst flow after incident and before manoeuvre. It is here 484 MW on the
first two time steps.

When we define a maximum threshold of 480 MW before operation, we see that
on the first two time steps, although there was no constraint
in N or after parade, Metrix does preventive redispatching. In effect,
the flow before operation was 484MW for a threshold of 480 MW. metrix
therefore performs 8 MW of preventive redispatching to respect this threshold.

Result T01 T02 T03
  ----------------------------- ------------------ --- ----- ---------
MAX_TMP_THREAT_FLOW_S\_SO_2 -480 -480 -166.65
GEN_COST 852.6 857.2 67695.2
GEN_SO_G1 / -8.5 -670.2
GEN_SO_G2 -8.4 / /
GEN_SE_G 8.4 8.4 600
GEN_N\_G // 70.2
TOPOLOGY_S\_SO_1 SOO1_SOO1_DJ_OMN S_SO_2 S_SO_2
