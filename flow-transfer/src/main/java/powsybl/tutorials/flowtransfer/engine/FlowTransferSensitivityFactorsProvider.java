package powsybl.tutorials.flowtransfer.engine;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityFactorsProvider;
import com.powsybl.sensitivity.factors.BranchFlowPerInjectionIncrease;
import com.powsybl.sensitivity.factors.functions.BranchFlow;
import com.powsybl.sensitivity.factors.variables.InjectionIncrease;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FlowTransferSensitivityFactorsProvider implements SensitivityFactorsProvider {

    @Override
    public List<SensitivityFactor> getFactors(Network network) {
        Generator anyGenerator = findAnyConnectedGenerator(network);
        InjectionIncrease injectionIncrease = new InjectionIncrease(anyGenerator.getId(), anyGenerator.getNameOrId(), anyGenerator.getId());
        return network.getBranchStream().map(branch -> {
            BranchFlow branchFlow = new BranchFlow(branch.getId(), branch.getNameOrId(), branch.getId());
            return new BranchFlowPerInjectionIncrease(branchFlow, injectionIncrease);
        }).collect(Collectors.toList());
    }

    /**
     * Find a generator in the network that is connected and part of the main synchronous component.
     */
    private Generator findAnyConnectedGenerator(Network network) {
        return network.getGeneratorStream()
                .filter(this::isConnected)
                .filter(this::isInMainSynchronousComponent)
                .findAny().orElse(null);
    }

    /**
     * Check if a generator is part of the main synchronous component.
     */
    private boolean isInMainSynchronousComponent(Generator generator) {
        Bus connectionBus = generator.getTerminal().getBusBreakerView().getBus();
        return connectionBus != null && connectionBus.isInMainSynchronousComponent();
    }

    /**
     * Check if a generator is connected to the network.
     */
    private boolean isConnected(Generator generator) {
        return generator.getTerminal().isConnected();
    }
}
