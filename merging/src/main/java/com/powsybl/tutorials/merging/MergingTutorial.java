/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.tutorials.merging;

import com.powsybl.cgmes.conversion.CgmesExport;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/**
 * @author Miora Ralambotiana <miora.ralambotiana at rte-france.com>
 */
public final class MergingTutorial {

    private static final String TMP_DIR = System.getProperty("java.io.tmpdir"); // put your own directory output path

    public static void main(String[] args) {
        File fileBe = new File(MergingTutorial.class.getResource("/MicroGridTestConfiguration_T4_BE_BB_Complete_v2.zip").getPath());
        Network n1 = Network.read(fileBe.toString());
        String n1Id = n1.getId();
        File fileNl = new File(MergingTutorial.class.getResource("/MicroGridTestConfiguration_T4_NL_BB_Complete_v2.zip").getPath());
        Network n2 = Network.read(fileNl.toString());
        String n2Id = n2.getId();

        Network mergedNetwork = Network.merge(n1, n2);

        LoadFlowResult result = LoadFlow.run(mergedNetwork, LoadFlowParameters.load());
        System.out.println(result.isOk());
        System.out.println(result.getMetrics());

        // Generate updated SSH files (one by IGM)
        exportNetwork(mergedNetwork.getSubnetwork(n1Id), Path.of(TMP_DIR), "MicroGridTestConfiguration_T4_BE_BB_SSH_v2_TEST", Set.of("SSH"));
        exportNetwork(mergedNetwork.getSubnetwork(n2Id), Path.of(TMP_DIR), "MicroGridTestConfiguraton_T4_NL_BB_SSH_v2_TEST", Set.of("SSH"));
        exportNetwork(mergedNetwork, Path.of(TMP_DIR), "MicroGridTestConfiguration_T4_BE_NL_BB_SV_v2_TEST", Set.of("SV"));
    }

    private static void exportNetwork(Network network, Path outputDir, String baseName, Set<String> profilesToExport) {
        Objects.requireNonNull(network);
        Properties exportParams = new Properties();
        exportParams.put(CgmesExport.PROFILES, String.join(",", profilesToExport));
        network.write("CGMES", exportParams, outputDir.resolve(baseName));
    }

    private MergingTutorial() {
    }
}
