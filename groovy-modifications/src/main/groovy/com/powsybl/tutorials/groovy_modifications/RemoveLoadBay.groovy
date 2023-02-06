import com.powsybl.iidm.modification.topology.RemoveFeederBayBuilder
import com.powsybl.iidm.network.extensions.FourSubstationsNodeBreakerWithExtensionsFactory

// Load network
network = FourSubstationsNodeBreakerWithExtensionsFactory.create()

// Create network modification to add line
modification = new RemoveFeederBayBuilder()
        .withConnectableId('LD2')
        .build()

// Apply modification
modification.apply(network)

// Export network
saveNetwork("XIIDM", network, null, "/tmp/node_breaker_without_ld2.xiidm")