/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package powsybl.tutorials.flowtransfer.engine;

import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FlowTransferContingenciesProvider implements ContingenciesProvider {

    private final FlowTransferAnalysisInput flowTransferAnalysisInput;

    public FlowTransferContingenciesProvider(FlowTransferAnalysisInput flowTransferAnalysisInput) {
        this.flowTransferAnalysisInput = flowTransferAnalysisInput;
    }

    @Override
    public List<Contingency> getContingencies(Network network) {
        return flowTransferAnalysisInput.getOutages().stream()
                .map(BranchContingency::new)
                .map(brc -> new Contingency(brc.getId(), brc))
                .collect(Collectors.toList());
    }
}
