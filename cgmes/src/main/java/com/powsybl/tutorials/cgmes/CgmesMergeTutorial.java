/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.tutorials.cgmes;

import com.powsybl.commons.io.table.AsciiTableFormatterFactory;
import com.powsybl.commons.io.table.TableFormatterConfig;
import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.util.Networks;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.security.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Anne Tilloy <anne.tilloy at rte-france.com>
 */
public final class CgmesMergeTutorial {

    public static void main(String[] args) throws IOException {

        // These lines load two networks from files in CGMES format.
        // The two networks are stored in an array list.
        List<Network> networks = new ArrayList<>();
        // A micro grid from Elia TSO (Belgium).
        File fileBe = new File(CgmesMergeTutorial.class.getResource("/MicroGridTestConfiguration_T4_BE_BB_Complete_v2.zip").getPath());
        Network networkBe = Importers.loadNetwork(fileBe.toString());
        System.out.println("ID of BE network: " + networkBe.getId());
        System.out.println("BE substation count :" + networkBe.getSubstationCount());
        networks.add(networkBe);

        // These lines print the number of buses of the "networkBe" network.
        for (Component component : networkBe.getBusView().getConnectedComponents()) {
            System.out.println(networkBe.getName() + " :" + component.getNum() + " " + component.getSize() + " buses");
        }

        // A micro grid from Tennet TSO (Netherlands).
        File fileNl = new File(CgmesMergeTutorial.class.getResource("/MicroGridTestConfiguration_T4_NL_BB_Complete_v2.zip").getPath());
        Network networkNl = Importers.loadNetwork(fileNl.toString());
        System.out.println("ID of NL network: " + networkNl.getId());
        System.out.println("NL substation count: " + networkNl.getSubstationCount());
        networks.add(networkNl);

        // Note that the boundary files are also imported (they are located in each archive).

        // These lines print the energy sources and the loads of a network through
        // a visitor.
        TopologyVisitor visitor = new DefaultTopologyVisitor() {
            @Override
            public void visitGenerator(Generator generator) {
                System.out.println("Generator: " + generator.getName() + ": "
                        + generator.getTerminal().getP() + " MW (max. " + generator.getMaxP() + " MW)");
            }

            @Override
            public void visitLoad(Load load) {
                System.out.println("Load: " + load.getName() + ": "
                        + load.getTerminal().getP() + " MW");
            }
        };
        // Let's apply it to our two networks.
        for (Network n : networks) {
            System.out.println("Network: " + n.getName());
            for (VoltageLevel vl : n.getVoltageLevels()) {
                vl.visitEquipments(visitor);
            }
        }

        // We are going to merge the two previous networks.
        // Note that the second network is merged in the first one.
        networkBe.merge(networkNl);
        for (VoltageLevel vl : networkBe.getVoltageLevels()) {
            vl.visitEquipments(visitor);
        }

        // We check if the number of buses have increased.
        for (Component component : networkBe.getBusView().getConnectedComponents()) {
            System.out.println(networkBe.getName() + " :" + component.getNum() + " " + component.getSize() + " buses");
        }

        networkBe.getGenerator("3a3b27be-b18b-4385-b557-6735d733baf0").setVoltageRegulatorOn(false);

        // We are going to compute a load flow on this network. The load-flow engine used
        // is defined in the configuration file.
        // See the load-flow tutorial for more information.
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        LoadFlow.run(networkBe, loadFlowParameters);

        // This following function prints the active balance summary.
        Networks.printBalanceSummary("Balance: ", networkBe, new PrintWriter(System.out));

        // We are going to perform a security analysis on the merged network.

        // A security analysis needs a list of contingencies.
        // The list is composed of all lines which voltage is less than 300 kV.
        List<Contingency> contingencies = networkBe.getLineStream()
                .filter(l -> l.getTerminal1().getVoltageLevel().getNominalV() < 300)
                .map(l -> new Contingency(l.getName(), new BranchContingency(l.getId())))
                .collect(Collectors.toList());

        System.out.println("Number of contingencies: " + contingencies.size());

        // We are going to run the security analysis on each contingency of the list.
        SecurityAnalysisParameters securityAnalysisParameters = new SecurityAnalysisParameters()
                .setLoadFlowParameters(loadFlowParameters);
        SecurityAnalysisReport securityAnalysisReport = SecurityAnalysis.run(networkBe,  contingencies, securityAnalysisParameters);
        SecurityAnalysisResult securityAnalysisResult = securityAnalysisReport.getResult();

        // Let's analyse the results.
        // For each contingency, only the two windings transformer NL-TR2_1 is overloaded.
        // Current permanent limit at each side of the transformer is reached.
        Security.print(securityAnalysisResult,
                networkBe,
                new OutputStreamWriter(System.out),
                new AsciiTableFormatterFactory(),
                new Security.PostContingencyLimitViolationWriteConfig(null, TableFormatterConfig.load(), true, true));
    }

    private CgmesMergeTutorial() {
    }

}
