---
layout: default
---

# How to manage topological views?
This example aims first to create a substation with a node/breaker topology model and to visit it through the bus/breaker and the bus views.
Then, we create the same substation with a bus/breaker topology model and we visit it through the bus/breaker and the bus views.
The corresponding code is fully available in the [topology](https://github.com/powsybl/powsybl-tutorials) tutorial.


# Building a network in node/breaker topology model

The substation of this tutorial has two voltage levels VL1 and VL2 described with a node/breaker topology model.
The first voltage level VL1 has 2 busbar sections BBS1 and BBS2, a generator GN, a load LD and a coupler BR3 between busbar sections BBS1 and BBS2.
The second voltage level VL2 has a single busbar section BBS3, a line LN and is connected to voltage level VL1 through transformer TR.
Below is a diagram of the substation:

![Node breaker topology](./img/topology/nodeBreakerTopology.svg){: width="50%" .center-image}

The node/breaker topology model is stored inside the voltage level as a graph, in which the vertices correspond to connectivity nodes and the edges correspond to switches (or internal connections).
The next diagram shows how to map the substation topology to a graph.

![Node breaker graph](./img/topology/nodeBreakerTopologyGraph.svg){: width="50%" .center-image}

- Each voltage level has its own topology graph. Voltage level VL1 has 8 connectivity nodes. Generator GN is connected to node 1, load LD to node 5, busbar sections BBS1 and BBS2 to node 3 and 4. 2, 6 and 7 are internal connectivity nodes.
- Voltage level VL2 has 3 nodes, line LN is connected to node 1, busbar section BBS3 to node 2.
- Transformer TR is connected to node 8 of voltage level 400 KV and node 3 of voltage level 225 KV.
- Plain edges represent closed switches.
- Dashed edges represent open switches.
- Green edges will disappear during the bus/breaker topology computation whereas pink edges (like 3<->4 in this case) will be retained, whatever their state open or closed.

## Bus/breaker view

The following diagram shows the computed bus/breaker topology.
Compared to the node/breaker topology, only the equipments (GN, LD, TR, LN) and the switches flagged as retained (BR3) remain in the network description.
The equipments are now connected through buses (B1, B2 and B3).

![Bus breaker graph](./img/topology/busBreakerTopology.svg){: width="50%" .center-image}

We can switch to a bus/breaker view on the substation voltage level VL1.
VL1 contains 2 buses in the bus/breaker view.
The first bus connects the nodes 1, 2, 3, 5 and 6, and consequently the generator GN and the load LD.
Note that the load LD does not belong to the connected component, because the switch BR2 is open.
The second bus connects nodes 4, 7 and 8. Note that VL1 contains only one switch in the bus/breaker view (BR3).
Here are the corresponding prints in the tutorial:

````
Bus: VL1_1
	 Generators: GN
Bus: VL1_4
	 T2W: TR
Bus: VL1_5
	 Loads: LD, connected component: null
Switch: BR3
Bus: VL2_1
	 T2W: TR
````

## Bus view

The following diagram shows the computed bus topology. Compared to bus/breaker topology, there is no switches anymore. There remains only equipments (GN, TR, LN) connected through buses. LD is not connected.

![Bus graph](./img/topology/busTopology.svg){: width="50%" .center-image}

We can switch to a bus view on substation voltage level VL1. VL1 contains 1 bus in the bus view. This bus connects all the equipments of voltage level VL1. Here are the corresponding prints in the tutorial:

````
Bus:VL1_1
	 Generators: GN
	 T2Ws: TR
Bus:VL2_1
	 T2Ws: TR
````

# Building a network in bus/breaker topology model

Note that creating a substation with a bus/breaker topology model is easier.
Instead of creating VL1 and VL3 with a node/breaker topology model, we can directly create them in a simpler bus/breaker topology model.
It can be very useful when data source only contains bus/branch data link in UCTE-DEF or in CIM-CGMES format sometimes.
Beware: in that case the node/breaker view status on voltage level VL1 and VL2 is N/A (not available).

Here are the corresponding prints in the tutorial. Note that the load LD is not printed in that case:

````
bus/breaker network in bus/breaker view:
Bus: B1
	 Generators: GN
Bus: B2
	 T2Ws: TR
Switches: BR3
Bus: B3
	 T2Ws: TR
bus/breaker network in bus view:
Bus:VL1_0
	 Generators: GN
	 T2Ws: TR
Bus:VL2_0
	 T2Ws: TR
````