/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package powsybl.tutorials.flowtransfer.engine;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FlowTransferAnalysisResult {

    private final Map<String, FlowTransfer> flowTransfersMap;

    public FlowTransferAnalysisResult(Map<String, FlowTransfer> flowTransfersMap) {
        this.flowTransfersMap = flowTransfersMap;
    }

    /**
     * For each outage, a flow transfer is computed.
     * As several outages are simulated, it gets all the flow transfers.
     */
    public Map<String, FlowTransfer> allFlowTransfers() {
        return Collections.unmodifiableMap(flowTransfersMap);
    }

    /**
     * Get the flow transfer for a specific outage.
     */
    public FlowTransfer flowTransfer(String outage) {
        Objects.requireNonNull(outage);
        return flowTransfersMap.get(outage);
    }
}
