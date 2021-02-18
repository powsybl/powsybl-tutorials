/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package powsybl.tutorials.flowtransfer.engine;

import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityAnalysis;
import com.powsybl.sensitivity.SensitivityAnalysisResult;
import com.powsybl.sensitivity.SensitivityFactorsProvider;
import com.powsybl.sensitivity.SensitivityValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FlowTransferAnalysis {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowTransferAnalysis.class);

    private Map<String, Double> nFlowsByBranch;

    private Map<String, FlowTransfer> flowTransfers;

    public FlowTransferAnalysisResult run(Network network, FlowTransferAnalysisInput input) {
        SensitivityFactorsProvider sensitivityFactorsProvider = new FlowTransferSensitivityFactorsProvider();
        ContingenciesProvider contingenciesProvider = new FlowTransferContingenciesProvider(input);
        SensitivityAnalysisResult sensitivityAnalysisResult = SensitivityAnalysis.run(network, sensitivityFactorsProvider, contingenciesProvider);
        return buildResult(sensitivityAnalysisResult);
    }

    /**
     * Check if a generator is part of the main synchronous component.
     */
    private FlowTransferAnalysisResult buildResult(SensitivityAnalysisResult sensitivityAnalysisResult) {
        // From the sensitivity analysis result, we get the sensitivity values.
        // Please refer to https://www.powsybl.org/pages/documentation/simulation/sensitivity/ for more details.
        nFlowsByBranch = flowsOnBranch(sensitivityAnalysisResult.getSensitivityValues());

        // From the sensitivity analysis result, we get the sensitivity values after each contingency.
        flowTransfers = sensitivityAnalysisResult.getSensitivityValuesContingencies().entrySet()
                .stream()
                .map(entry -> buildFlowTransfer(entry.getKey(), flowsOnBranch(entry.getValue())))
                .collect(Collectors.toMap(FlowTransfer::getOutage, Function.identity()));
        return new FlowTransferAnalysisResult(flowTransfers);
    }

    private FlowTransfer buildFlowTransfer(String outage, Map<String, Double> outageFlowsByBranch) {
        Double nFlow = nFlowsByBranch.get(outage);
        if (nFlow == null) {
            LOGGER.error("The initial flow on the outage is not defined.");
        }
        Map<String, FlowCollection> flowCollections = new TreeMap<>();
        nFlowsByBranch.forEach((branchId, nFlowBranchValue) -> {
            Double outageFlowBranchValue = outageFlowsByBranch.get(branchId);
            if (outageFlowBranchValue == null) {
                LOGGER.error("The final flow on branch %s is not defined.", branchId);
            }
            flowCollections.put(branchId, new FlowCollection(nFlowBranchValue, outageFlowBranchValue));
        });
        return new FlowTransfer(outage, nFlow, flowCollections);
    }

    private Map<String, Double> flowsOnBranch(Collection<SensitivityValue> sensitivityValues) {
        // From the sensitivity values, we get only the reference value of the function: here the flow on each branch
        // before any contingency.
        return sensitivityValues.stream()
                .collect(Collectors.toMap(sensitivityValue -> sensitivityValue.getFactor().getFunction().getId(), SensitivityValue::getFunctionReference));
    }
}
