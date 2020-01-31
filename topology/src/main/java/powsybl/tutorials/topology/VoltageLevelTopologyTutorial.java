/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package powsybl.tutorials.topology;

import com.powsybl.iidm.network.*;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.Iterator;

public final class VoltageLevelTopologyTutorial {

    public static void main(String[] args) throws IOException {


        // The following code shows how to create the substation with a node/breaker topology model.
        Network network = NetworkFactory.create("network", "test")
                .setCaseDate(DateTime.parse("2018-05-11T12:00:55+01:00"))
                .setForecastDistance(60);

        Substation substation = network.newSubstation()
                .setId("substation")
                .add();

        VoltageLevel vl1 = substation.newVoltageLevel()
                .setId("VL1")
                .setNominalV(132.0)
                .setTopologyKind(TopologyKind.NODE_BREAKER)
                .add();
        vl1.getNodeBreakerView().setNodeCount(9); // Bizarre...
        // Create busbar sections BBS1 and BBS2
        vl1.getNodeBreakerView().newBusbarSection()
                .setId("BBS1")
                .setNode(3)
                .add();
        vl1.getNodeBreakerView().newBusbarSection()
                .setId("BBS2")
                .setNode(4)
                .add();
        // Create generator GN
        vl1.newGenerator()
                .setId("GN")
                .setNode(1)
                .setMinP(0)
                .setMaxP(100)
                .setTargetP(80)
                .setTargetQ(50)
                .setVoltageRegulatorOn(false)
                .add();
        // Create load LD
        vl1.newLoad()
                .setId("LD")
                .setNode(5)
                .setP0(40)
                .setQ0(0)
                .add();
        // Connect generator GN by creating breaker BR1 and disconnectors DI1 and DI2
        vl1.getNodeBreakerView().newBreaker()
                .setId("BR1")
                .setOpen(false)
                .setNode1(1)
                .setNode2(2)
                .add();
/*        vl1.getNodeBreakerView().newDisconnector()
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
                .add();*/
        // Connect load LD through BR2.
        vl1.getNodeBreakerView().newBreaker()
                .setId("BR2")
                .setOpen(false)
                .setNode1(5)
                .setNode2(6)
                .add();
        // Create busbar coupler BR3
        vl1.getNodeBreakerView().newBreaker()
                .setId("BR3")
                .setOpen(false)
                .setRetained(true) // Retain this breaker in the bus/breaker topology !
                .setNode1(3)
                .setNode2(4)
                .add();

        VoltageLevel vl2 = substation.newVoltageLevel()
                .setId("VL2")
                .setNominalV(220.0)
                .setTopologyKind(TopologyKind.NODE_BREAKER)
                .add();
        vl2.getNodeBreakerView().setNodeCount(4); // Bizarre...
        // Create busbar section BBS3
        vl2.getNodeBreakerView().newBusbarSection()
                .setId("BBS3")
                .setNode(2)
                .add();
        vl2.getNodeBreakerView().newBreaker()
                .setId("BR6")
                .setNode1(1)
                .setNode2(2)
                .add();
        // Create load LD2 (instead of LN, it is faster)
        vl2.newLoad()
                .setId("LD2")
                .setNode(1)
                .setP0(40)
                .setQ0(0)
                .add();

        // Create transformer TR
        vl1.getNodeBreakerView().newBreaker()
                .setId("BR4")
                .setOpen(false)
                .setNode1(7)
                .setNode2(8)
                .add();
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

        // The following code shows how to get buses and breakers of the bus/breaker view in voltage level VL1.
        // VL1 contains 2 buses in the bus/breaker view
        Iterator<Bus> itB = vl1.getBusBreakerView().getBuses().iterator();
        // First bus connects nodes 1, 2, 3, 5, 6
        Bus b1 = itB.next();
        // ... and consequently generator GN and load LD
        System.out.println(b1.getGenerators().iterator().next().getId());
        System.out.println(b1.getLoads().iterator().next().getId());
        // second bus connects nodes 4, 7, 8
        Bus b2 = itB.next();
        System.out.println(b2.getTwoWindingsTransformers().iterator().next().getId());
        // VL1 contains 1 switch in the bus/breaker view
        Iterator<Switch> itS = vl1.getBusBreakerView().getSwitches().iterator();
        while (itS.hasNext()) {
            System.out.println(itS.next().getId());
        }
    }

    private VoltageLevelTopologyTutorial() {
    }

}
