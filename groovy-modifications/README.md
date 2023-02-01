# Tutorial on how to use network modifications methods in Groovy

In this tutorial, you will find two scripts explaining how to add a load and a line with the associated topology to a network.

The network element is added to the network and the switches are also created to connect the element 
to a given busbar section with a closed disconnector and a breaker. Open switches are also added to every parallel busbar sections.

Other similar functions are available to add every kind of injections and branches.

# CreateLoadBay.groovy

In this script, a node/breaker network is first loaded. Then a load is added on the busbar section BBS1 of the voltage level named S1VL2 and the network is exported.

Before adding the load, the voltage level S1VL2 is:

![S1VL2](.github/nb_network_s1vl2.png "S1VL2")

The script is adding the load between the transformer TWT (order position 10) and the VSC converter station VSC1 (order position 20). For that, the order position is put to 15 when creating the modification.
It is also possible to specify the direction of the load. Here it is not, so by default it is directed to the bottom.

The voltage level S1VL2 will look like that after applying the modification:

![S2VL1 with new load](.github/nb_network_with_load.png "S2VL1 with new load")

It is possible to create a builder with any kind of injection adder and thus create any type of injection.

# CreateLineBay.groovy

In this script, the same node/breaker network is loaded and a line is created between two voltage levels S1VL2 and S2VL1.

Before adding the line, the two voltage levels are:
![S1VL2](.github/nb_network_s1vl2.png "S1VL2")
![S2VL1](.github/nb_network_s2vl1.png "S2VL1")

The script is adding the line between the generator GH3 (order position 50) and the load LD2 (order position 60) on S1VL2 and on the right of the line LINE_S2S3 (order position 30) on S2VL1.
The order position for the new line in the script are taken to be between 50 and 60 on S1VL2 and higher than 30 on S2VL1.
On S1VL2, the new line is pointing to the bottom, which is specified in the modification via the parameter ConnectablePosition.BOTTOM.
On S2VL1, the direction of the new line is TOP.

The voltage levels will then look like that after adding the line:

![S1VL2 with new line](.github/nb_network_with_new_line_S1VL2.png "S1VL2 with new line")
![S2VL1 with new line](.github/nb_network_with_new_line_s2vl1.png "S2VL1 with new line")

The same method can be used to create a two-windings transformer on a network.