/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package powsybl.tutorials.loadflow;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;

import java.io.File;

/**
 * @author Agnes Leroy <agnes.leroy at rte-france.com>
 */
public final class LoadflowTutorial {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadflowTutorial.class);

    public static void main(String[] args) {
        LOGGER.info("Starting the load flow tutorial execution now");
    }

    private LoadflowTutorial() {
    }
}

