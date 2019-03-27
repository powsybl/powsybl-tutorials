/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.tutorials.csv.importer;

import com.powsybl.iidm.import_.Importers;

/**
 * @author Miora Ralambotiana <miora.ralambotiana at rte-france.com>
 */
public final class Main {

    public static void main(String[] args) {
        Importers.loadNetwork(Main.class.getResource("/eurostag-tutorial-example1.csv").getPath());
    }

    private Main() {
    }

}
