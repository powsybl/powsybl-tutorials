/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package powsybl.tutorials.flowtransfer.engine;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FlowCollection {
    private final double initialFlow;
    private final double finalFlow;

    public FlowCollection(double initialFlow, double finalFlow) {
        this.initialFlow = initialFlow;
        this.finalFlow = finalFlow;
    }

    /**
     * Get the initial active power in MW.
     */
    public double getInitialFlow() {
        return initialFlow;
    }

    /**
     * Get the final active power in MW.
     */
    public double getFinalFlow() {
        return finalFlow;
    }
}
