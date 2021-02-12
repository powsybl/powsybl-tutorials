/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package powsybl.tutorials.flowtransfer.engine;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FlowTransferAnalysisInput {

    private final List<String> outages;

    public FlowTransferAnalysisInput(List<String> outages) {
        this.outages = Objects.requireNonNull(outages);
    }

    /**
     * Get the list of outages that is simulated.
     */
    public List<String> getOutages() {
        return Collections.unmodifiableList(outages);
    }
}
