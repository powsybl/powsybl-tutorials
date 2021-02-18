/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package powsybl.tutorials.flowtransfer;

import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import powsybl.tutorials.flowtransfer.engine.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Anne Tilloy {@literal <anne.tilloy at rte-france.com>}
 */
public final class FlowTransferAnalysisTutorial {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowTransferAnalysisTutorial.class);

    public static void main(String[] args) {
        Network network = ExampleGenerator.network();
        FlowTransferAnalysisInput input = ExampleGenerator.input();

        FlowTransferAnalysis flowTransferAnalysis = new FlowTransferAnalysis();
        FlowTransferAnalysisResult result = flowTransferAnalysis.run(network, input);

        result.allFlowTransfers().forEach((outage, flowTransfer) -> {
            LOGGER.error("Analysing transfer flows for outage %s", outage);
            LOGGER.error("Flow before outage: %f MW", flowTransfer.getNFlow());
            flowTransfer.getFlowCollections().forEach((branch, flowCollection) -> {
                if (branch.equals(outage)) {
                    LOGGER.error("  %s, outage analysed", branch);
                } else {
                    LOGGER.error("  %s, init: %f MW, final: %f MW, flow transfer rate: %.5f",
                            branch, flowCollection.getInitialFlow(), flowCollection.getFinalFlow(), computeRate(flowTransfer.getNFlow(), flowCollection));
                }
            });
        });
    }

    private static double computeRate(double initialFlow, FlowCollection flowCollection) {
        return (flowCollection.getFinalFlow() - flowCollection.getInitialFlow()) / initialFlow;
    }

    private FlowTransferAnalysisTutorial() {
    }
}
