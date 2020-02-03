/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package powsybl.tutorials.topology;

import com.powsybl.iidm.network.*;

import java.io.IOException;

public final class VoltageLevelTopologyTutorial {

    public static void main(String[] args) throws IOException {

        // First, we create the network described in the user story "topology" in bus/breaker topology model.
        Network network1 = VoltageLevelTopologyTutorial.createBusBreakerNetwork();

        // We visit the network through its bus/breaker view.
        System.out.println(network1.getId() + " in bus/breaker view: ");
        for (VoltageLevel vl : network1.getVoltageLevels()) {
            for (Bus bus : vl.getBusBreakerView().getBuses()) {
                System.out.println("Bus: " + bus.getId());
                for (Generator gen : bus.getGenerators()) {
                    System.out.println("\t Generators: " + gen.getId());
                }
                for (Load load : bus.getLoads()) {
                    System.out.println("\t Loads: " + load.getId() +
                            ", connected component: " + bus.getConnectedComponent());
                }
                for (TwoWindingsTransformer twt : bus.getTwoWindingsTransformers()) {
                    System.out.println("\t T2W: " + twt.getId());
                }
            }
            for (Switch sw : vl.getBusBreakerView().getSwitches()) {
                System.out.println("Switch: " + sw.getId());
            }
        }

        // Now, we visit the network through its bus view.
        System.out.println(network1.getId() + " in bus view: ");
        for (VoltageLevel vl : network1.getVoltageLevels()) {
            for (Bus bus : vl.getBusView().getBuses()) {
                System.out.println("Bus:" + bus.getId());
                for (Generator gen : bus.getGenerators()) {
                    System.out.println("\t Generators: " + gen.getId());
                }
                for (Load load : bus.getLoads()) {
                    System.out.println("\t Loads: " + load.getId());
                }
                for (TwoWindingsTransformer twt : bus.getTwoWindingsTransformers()) {
                    System.out.println("\t T2Ws: " + twt.getId());
                }
            }
        }

        // First, we create the network described in the user story "topology" in node/breaker topology model.
        Network network2 = VoltageLevelTopologyTutorial.createNodeBreakerNetwork();

        // We visit the network through its bus/breaker view.
        System.out.println(network2.getId() + " in bus/breaker view: ");
        for (VoltageLevel vl : network2.getVoltageLevels()) {
            for (Bus bus : vl.getBusBreakerView().getBuses()) {
                System.out.println("Bus: " + bus.getId());
                for (Generator gen : bus.getGenerators()) {
                    System.out.println("\t Generators: " + gen.getId());
                }
                for (Load load : bus.getLoads()) {
                    System.out.println("\t Loads: " + load.getId() +
                            ", connected component: " + bus.getConnectedComponent());
                }
                for (TwoWindingsTransformer twt : bus.getTwoWindingsTransformers()) {
                    System.out.println("\t T2Ws: " + twt.getId());
                }
            }
            for (Switch sw : vl.getBusBreakerView().getSwitches()) {
                System.out.println("Switches: " + sw.getId());
            }
        }

        // Now, we visit the network through its bus view.
        System.out.println(network2.getId() + " in bus view: ");
        for (VoltageLevel vl : network2.getVoltageLevels()) {
            for (Bus bus : vl.getBusView().getBuses()) {
                System.out.println("Bus:" + bus.getId());
                for (Generator gen : bus.getGenerators()) {
                    System.out.println("\t Generators: " + gen.getId());
                }
                for (Load load : bus.getLoads()) {
                    System.out.println("\t Loads: " + load.getId());
                }
                for (TwoWindingsTransformer twt : bus.getTwoWindingsTransformers()) {
                    System.out.println("\t T2Ws: " + twt.getId());
                }
            }
        }
    }


    private static Network createBusBreakerNetwork() {

        Network network = Network.create("bus/breaker network", "test");

        // The following code shows how to create the substation with a bus/breaker topology model.
        Substation  substation = network.newSubstation()
                .setId("susbtation")
                .add();

        // We create a voltage level vl1 which nominal voltage is 132.
        VoltageLevel vl1 = substation.newVoltageLevel()
                .setId("VL1")
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .setNominalV(132)
                .add();

        // Voltage level vl1 has two buses.
        vl1.getBusBreakerView().newBus()
                .setId("B1")
                .add();
        vl1.getBusBreakerView().newBus()
                .setId("B2")
                .add();

        // We create a breaker between the two buses.
        vl1.getBusBreakerView().newSwitch()
                .setId("BR3")
                .setBus1("B1")
                .setBus2("B2")
                .add();

        // We create a generator connected through bus B1.
        vl1.newGenerator()
                .setId("GN")
                .setBus("B1")
                .setMinP(0)
                .setMaxP(100)
                .setTargetP(80)
                .setTargetQ(50)
                .setVoltageRegulatorOn(false)
                .add();

        // We create a load disconnected but which can be connected through B1.
        vl1.newLoad()
                .setId("LD")
                .setConnectableBus("B1")
                .setP0(40)
                .setQ0(0)
                .add();

        // We create a voltage level vl2 which nominal voltage is 220 kV.
        VoltageLevel vl2 = substation.newVoltageLevel()
                .setId("VL2")
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .setNominalV(220)
                .add();

        // Vl2 has only one bus B3.
        vl2.getBusBreakerView().newBus()
                .setId("B3")
                .add();

        // We create a dangling line connected through B3.
        vl2.newDanglingLine()
                .setId("LN")
                .setBus("B3")
                .setR(0)
                .setX(0)
                .setB(0)
                .setG(0)
                .setP0(40)
                .setQ0(0)
                .add();

        // We create w two windings transformer that connects vl1 though B2 and vl2 through B3.
        substation.newTwoWindingsTransformer()
                .setId("TR")
                .setVoltageLevel1(vl1.getId())
                .setBus1("B2")
                .setVoltageLevel2(vl2.getId())
                .setBus2("B3")
                .setR(0.5)
                .setX(10)
                .setB(0)
                .setG(0)
                .setRatedU1(vl1.getNominalV())
                .setRatedU2(vl2.getNominalV())
                .add();

        return network;
    }

    private static Network createNodeBreakerNetwork() {

        Network network = Network.create("node/breaker network", "test");

        // The following code shows how to create the substation with a node/breaker topology model.
        Substation substation = network.newSubstation()
                .setId("substation")
                .add();

        // The substation contains 2 voltage levels: vl1 which nominal voltage is 132 kV and vl2
        // which nominal voltage is 220 kV.
        VoltageLevel vl1 = substation.newVoltageLevel()
                .setId("VL1")
                .setNominalV(132)
                .setTopologyKind(TopologyKind.NODE_BREAKER)
                .add();
        vl1.getNodeBreakerView().setNodeCount(9);

        // We create 2 busbar sections BBS1 and BBS2.
        vl1.getNodeBreakerView().newBusbarSection()
                .setId("BBS1")
                .setNode(3)
                .add();
        vl1.getNodeBreakerView().newBusbarSection()
                .setId("BBS2")
                .setNode(4)
                .add();

        // We create a generator GN.
        vl1.newGenerator()
                .setId("GN")
                .setNode(1)
                .setMinP(0)
                .setMaxP(100)
                .setTargetP(80)
                .setTargetQ(50)
                .setVoltageRegulatorOn(false)
                .add();
        // We connect the generator GN by creating breaker BR1 and disconnectors DI1 and DI2.
        vl1.getNodeBreakerView().newBreaker()
                .setId("BR1")
                .setOpen(false)
                .setNode1(1)
                .setNode2(2)
                .add();
        vl1.getNodeBreakerView().newDisconnector()
                .setId("DI1")
                .setOpen(false)
                .setNode1(2)
                .setNode2(3)
                .add();
        vl1.getNodeBreakerView().newDisconnector()
                .setId("DI2")
                .setOpen(true)
                .setNode1(2)
                .setNode2(4)
                .add();

        // We create a load LD.
        vl1.newLoad()
                .setId("LD")
                .setNode(5)
                .setP0(40)
                .setQ0(0)
                .add();
        // We connect load LD through BR2.
        vl1.getNodeBreakerView().newBreaker()
                .setId("BR2")
                .setOpen(true)
                .setNode1(5)
                .setNode2(6)
                .add();
        vl1.getNodeBreakerView().newDisconnector()
                .setId("DI5")
                .setOpen(false)
                .setNode1(3)
                .setNode2(6)
                .add();
        vl1.getNodeBreakerView().newDisconnector()
                .setId("DI6")
                .setOpen(true)
                .setNode1(4)
                .setNode2(6)
                .add();

        vl1.getNodeBreakerView().newBreaker()
                .setId("BR4")
                .setOpen(false)
                .setNode1(7)
                .setNode2(8)
                .add();
        vl1.getNodeBreakerView().newDisconnector()
                .setId("DI3")
                .setOpen(true)
                .setNode1(3)
                .setNode2(7)
                .add();
        vl1.getNodeBreakerView().newDisconnector()
                .setId("DI4")
                .setOpen(false)
                .setNode1(4)
                .setNode2(7)
                .add();

        // We create a busbar coupler BR3.
        vl1.getNodeBreakerView().newBreaker()
                .setId("BR3")
                .setOpen(false)
                .setRetained(true) // Retain this breaker in the bus/breaker topology !
                .setNode1(3)
                .setNode2(4)
                .add();

        // We create a second voltage level vl2 which nominal voltage is 220 kV.
        VoltageLevel vl2 = substation.newVoltageLevel()
                .setId("VL2")
                .setNominalV(220)
                .setTopologyKind(TopologyKind.NODE_BREAKER)
                .add();
        vl2.getNodeBreakerView().setNodeCount(4);

        // We create a busbar section BBS3.
        vl2.getNodeBreakerView().newBusbarSection()
                .setId("BBS3")
                .setNode(2)
                .add();

        // We create a dangling line LN.
        vl2.newDanglingLine()
                .setId("LN")
                .setNode(1)
                .setP0(40)
                .setQ0(0)
                .setR(0)
                .setX(0)
                .setB(0)
                .setG(0)
                .add();
        vl2.getNodeBreakerView().newBreaker()
                .setId("BR6")
                .setOpen(false)
                .setNode1(1)
                .setNode2(2)
                .add();

        // We create transformer TR between voltage level vl1 and voltage level vl2.
        vl2.getNodeBreakerView().newBreaker()
                .setId("BR5")
                .setOpen(false)
                .setNode1(2)
                .setNode2(3)
                .add();
        substation.newTwoWindingsTransformer()
                .setId("TR")
                .setVoltageLevel1(vl1.getId())
                .setNode1(8)
                .setVoltageLevel2(vl2.getId())
                .setNode2(3)
                .setR(0.5)
                .setX(10)
                .setB(0)
                .setG(0)
                .setRatedU1(vl1.getNominalV())
                .setRatedU2(vl2.getNominalV())
                .add();

        return network;

    }

    private VoltageLevelTopologyTutorial() {
    }

}
