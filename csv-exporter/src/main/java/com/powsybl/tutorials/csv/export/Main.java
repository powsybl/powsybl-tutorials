/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.tutorials.csv.export;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;

import java.nio.file.Paths;

/**
 * @author Miora Ralambotiana <miora.ralambotiana at rte-france.com>
 *
 * This class is only needed to run the tutorial easily.
 */
public final class Main {

    public static void main(String[] args) {
        Network network = EurostagTutorialExample1Factory.create();
        network.write("CSV", null, Paths.get(System.getProperty("java.io.tmpdir"), "test.csv"));
    }

    private Main() {
    }

}
