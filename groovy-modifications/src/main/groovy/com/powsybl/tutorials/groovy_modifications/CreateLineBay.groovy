package com.powsybl.tutorials.groovy_modifications

import com.powsybl.iidm.modification.topology.CreateBranchFeederBaysBuilder
import com.powsybl.iidm.network.extensions.ConnectablePosition
import com.powsybl.iidm.network.extensions.FourSubstationsNodeBreakerWithExtensionsFactory

// Load network
network = FourSubstationsNodeBreakerWithExtensionsFactory.create()

// Create line adder
lineAdder = network.newLine()
        .setId("newLine")
        .setVoltageLevel1("S1VL2")
        .setVoltageLevel2("S2VL1")
        .setR(5.0)
        .setX(50.0)
        .setG1(20.0)
        .setB1(30.0)
        .setG2(40.0)
        .setB2(50.0)

// Create network modification to add line
modification = new CreateBranchFeederBaysBuilder()
        .withBranchAdder(lineAdder)
        .withBusOrBusbarSectionId1("S1VL2_BBS2")
        .withBusOrBusbarSectionId2("S2VL1_BBS")
        .withDirection1(ConnectablePosition.Direction.BOTTOM)
        .withDirection2(ConnectablePosition.Direction.TOP)
        .withPositionOrder1(55)
        .withPositionOrder2(45)
        .build()

// Apply modification
modification.apply(network)

// Export network
saveNetwork("XIIDM", network, null, "/tmp/node_breaker_with_new_line.xiidm")