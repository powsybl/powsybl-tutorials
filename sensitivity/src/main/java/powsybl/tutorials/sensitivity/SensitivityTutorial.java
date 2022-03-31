/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package powsybl.tutorials.sensitivity;

import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Agnes Leroy {@literal <agnes.leroy at rte-france.com>}
 */
public final class SensitivityTutorial {
    private static final Logger LOGGER = LoggerFactory.getLogger(SensitivityTutorial.class);

    public static void main(String[] args) throws IOException {

        // 1. Import the network from a XML file
        // The network is described in the iTesla Internal Data Model format.
        Path networkPath = Paths.get(SensitivityTutorial.class.getResource("/sensi_network_12_nodes.xml").getPath());
        Network network = Importers.loadNetwork(networkPath.toString());
        // In this tutorial the sensitivity is done on active power only, but this can be changed in the config file
        // by setting dcMode = false in the hades2-default-parameters. Then, the power flow will handle both P and Q.

        // 2. Create a list of factors to be studied in the sensitivity computation
        // First, create the sensitivity factors in Java directly
        // Start by defining the monitored lines.
        List<Line> monitoredLines = new ArrayList<>();
        monitoredLines.add(network.getLine("BBE2AA1  FFR3AA1  1"));
        monitoredLines.add(network.getLine("FFR2AA1  DDE3AA1  1"));
        monitoredLines.add(network.getLine("DDE2AA1  NNL3AA1  1"));
        monitoredLines.add(network.getLine("NNL2AA1  BBE3AA1  1"));

        // Print the initial flow through each of the monitored lines.
        // These are the values of the "function reference" in the
        // JSON result file.
        LoadFlow.run(network, LoadFlowParameters.load());
        LOGGER.info("Initial active power through the four monitored lines");
        for (Line line : monitoredLines) {
            LOGGER.info("LINE {} - P: {} MW", line.getId(), line.getTerminal1().getP());
        }

        // Then build the factors themselves.
        List<SensitivityFactor> factors = new ArrayList<>();
        monitoredLines.forEach(l -> {
            String monitoredBranchId = l.getId();
            String monitoredBranchName = l.getName();
            String twtId = network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getId();
            factors.add(new SensitivityFactor(SensitivityFunctionType.BRANCH_ACTIVE_POWER, monitoredBranchId,
                    SensitivityVariableType.TRANSFORMER_PHASE, twtId, false, ContingencyContext.all()));
        });

        // 3. Run the sensitivity analysis
        // Run the analysis that will be performed on network working variant with default sensitivity analysis parameters
        // Default implementation defined in the platform configuration will be used.
        SensitivityAnalysisResult results = SensitivityAnalysis.run(network, factors, Collections.emptyList());

        // 4. Output the results
        // Print the sensitivity values in the terminal.
        // The values of the four factors are expressed in MW/°, which
        // means that for a 1° phase change introduced by the PST, the flow
        // through a given monitored line is modified by the value of the
        // factor (in MW).
        // Here we see that each line will undergo a variation of 25MW for a phase change of 1°
        // introduced by the PST (and -25MW for a -1° change).
        // The four monitored lines are affected identically, because in this very simple
        // network all lines have the same reactance.
        LOGGER.info("Initial sensitivity results");
        results.getPreContingencyValues().forEach(value ->
                LOGGER.info("Value: {} MW/°", value.getValue()));

        // 5. Perform a systematic sensitivity analysis
        // A systematic sensitivity analysis consists of a series of sensitivity calculations
        // performed on a network given a list of contingencies and sensitivity factors.
        // Here we use the systematic sensitivity feature of Hades2, creating one variant on which all
        // the calculations are done successively, without re-loading the network each time, by
        // modifying the Jacobian matrix directly in the solver.

        // Here the list of contingencies is composed of the lines that are not monitored
        // in the sensitivity analysis.
        List<Contingency> contingencies = network.getLineStream()
            .filter(l -> {
                final boolean[] isContingency = {true};
                monitoredLines.forEach(monitoredLine -> {
                    if (l.equals(monitoredLine)) {
                        isContingency[0] = false;
                        return;
                    }
                });
                return isContingency[0];
            })
            .map(l -> Contingency.branch(l.getId()))
            .collect(Collectors.toList());
        // This makes a total of 11 contingencies
        LOGGER.info("Number of contingencies: {}", contingencies.size());

        // Run the sensitivity computation with respect to the PST tap position on that network.
        SensitivityAnalysisResult results2 = SensitivityAnalysis.run(network, factors, contingencies);
    }

    private SensitivityTutorial() {
    }
}
