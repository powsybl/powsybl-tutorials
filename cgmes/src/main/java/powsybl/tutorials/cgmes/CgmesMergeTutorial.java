/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package powsybl.tutorials.cgmes;

import com.powsybl.commons.io.table.AsciiTableFormatterFactory;
import com.powsybl.commons.io.table.TableFormatterConfig;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.util.Networks;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.security.Security;
import com.powsybl.security.SecurityAnalysis;
import com.powsybl.security.SecurityAnalysisParameters;
import com.powsybl.security.SecurityAnalysisResult;
import com.rte_france.powsybl.hades2.Hades2Factory;
import com.rte_france.powsybl.hades2.Hades2SecurityAnalysisFactory;

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
                        + generator.getTerminal().getP() + " MW");
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

        // We are going to compute a load flow on this network with Hades2 simulator.
        // This line defined the way we want to compute : locally by default.
        // See the load-flow tutorial for more information.
        ComputationManager computationManager = LocalComputationManager.getDefault();
        LoadFlow loadFlow = new Hades2Factory().create(networkBe, computationManager, 0);
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters()
                .setVoltageInitMode(LoadFlowParameters.VoltageInitMode.DC_VALUES);
        LoadFlowResult result = loadFlow.run(VariantManagerConstants.INITIAL_VARIANT_ID, loadFlowParameters)
                .join();
        System.out.println(result.isOk());
        System.out.println(result.getMetrics());

        // This following function prints the active balance summary.
        Networks.printBalanceSummary("Balance: ", networkBe, new PrintWriter(System.out));

        // We are going to perform a security analysis on the merged network.
        SecurityAnalysis securityAnalysis = new Hades2SecurityAnalysisFactory().create(networkBe, computationManager, 0);
        SecurityAnalysisParameters securityAnalysisParameters = new SecurityAnalysisParameters(); // Default parameters.

        // A security analysis needs a contingencies provider in order to create a list of contingencies.
        // We are going to learn how to implement a contingencies provider.
        // This is the easy way (but not really the modern way).
        ContingenciesProvider contingenciesProvider = new ContingenciesProvider() {
            @Override
            public List<Contingency> getContingencies(Network network) {
                List<Contingency> contingencies = new ArrayList<>();
                for (Line line : network.getLines()) {
                    if (line.getTerminal1().getVoltageLevel().getNominalV() < 300) {
                        Contingency contingency = new Contingency(getName(line), new BranchContingency(line.getId()));
                        contingencies.add(contingency);
                    }
                }
                return contingencies;
            }
        };

        // These previous lines are equivalent to the following ones.
        ContingenciesProvider contingenciesProvider1 = new ContingenciesProvider() {
            @Override
            public List<Contingency> getContingencies(Network network) {
                return network.getLineStream()
                        .filter(l -> l.getTerminal1().getVoltageLevel().getNominalV() < 300)
                        .map(l -> new Contingency(getName(l), new BranchContingency(l.getId())))
                        .collect(Collectors.toList());
            }
        };

        // And finally, the best way to implement a contingencies provider is this one:
        // The list is composed of all lines which voltage is less than 300 kV.
        ContingenciesProvider contingenciesProvider2 = n -> n.getLineStream()
                .filter(l -> l.getTerminal1().getVoltageLevel().getNominalV() < 300)
                .map(l -> new Contingency(getName(l), new BranchContingency(l.getId())))
                .collect(Collectors.toList());

        System.out.println("Number of contingencies: " + contingenciesProvider2.getContingencies(networkBe).size());

        // We are going to run the security analysis on each contingency of the list.
        SecurityAnalysisResult securityAnalysisResult = securityAnalysis.run(VariantManagerConstants.INITIAL_VARIANT_ID,
                securityAnalysisParameters, contingenciesProvider2).join();

        // Let's analyse the results.
        // For each contingency, only the two windings transformer NL-TR2_1 is overloaded.
        // Current permanent limit at each side of the transformer is reached.
        Security.print(securityAnalysisResult,
                networkBe,
                new OutputStreamWriter(System.out),
                new AsciiTableFormatterFactory(),
                new Security.PostContingencyLimitViolationWriteConfig(null, TableFormatterConfig.load(), true, true));

    }

    // This function is needed to give a name to merged line.
    private static String getName(Line line) {
        if (line.isTieLine()) {
            TieLine tieLine = (TieLine) line;
            return tieLine.getHalf1().getName() + " + " + tieLine.getHalf2().getName();
        } else {
            return line.getName();
        }
    }

    private CgmesMergeTutorial() {
    }

}
