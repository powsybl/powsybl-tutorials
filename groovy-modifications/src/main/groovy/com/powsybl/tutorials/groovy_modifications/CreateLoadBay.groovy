package com.powsybl.tutorials.groovy_modifications

import com.powsybl.iidm.modification.topology.CreateFeederBayBuilder
import com.powsybl.iidm.network.extensions.FourSubstationsNodeBreakerWithExtensionsFactory

// Load network
network = FourSubstationsNodeBreakerWithExtensionsFactory.create()

// Create load adder
loadAdder = network.getVoltageLevel("S1VL2").newLoad().setId("newLoad").setP0(100).setQ0(50)

// Create network modification to add load
modification = new CreateFeederBayBuilder()
        .withInjectionAdder(loadAdder)
        .withBusOrBusbarSectionId("S1VL2_BBS1")
        .withInjectionPositionOrder(15)
        .build()

// Apply modification
modification.apply(network)

// Export network
saveNetwork("XIIDM", network, null, "/tmp/node_breaker_with_new_load.xiidm")

