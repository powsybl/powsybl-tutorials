/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.tutorials.sensitivity;

import com.powsybl.commons.json.JsonUtil;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.security.json.SecurityAnalysisJsonModule;
import com.powsybl.sensitivity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Agnes Leroy <agnes.leroy at rte-france.com>
 * @author Anne Tilloy <anne.tilloy at rte-france.com>
 */
public final class SensitivityTutorial {
    private static final Logger LOGGER = LoggerFactory.getLogger(SensitivityTutorial.class);

    public static void main(String[] args) throws IOException {

        // 1. Import the network from a XML file
        // The network is described in iidm (the iTesla Internal Data Model format).
        Network network = Importers.loadNetwork("sensi_network_12_nodes.xml",
                SensitivityTutorial.class.getResourceAsStream("/sensi_network_12_nodes.xml"));

        // In this tutorial the sensitivity is done on active and reactive powers, but this can be changed in
        // the LoadFlowParameters by using setDc(true). Then, the power flow will handle active power only.

        // 2. Create a list of factors to be studied in the sensitivity computation
        // First, create the sensitivity factors in Java directly
        // Start by defining the monitored lines.
        List<Line> monitoredLines = List.of(network.getLine("BBE2AA1  FFR3AA1  1"),
                                            network.getLine("FFR2AA1  DDE3AA1  1"),
                                            network.getLine("DDE2AA1  NNL3AA1  1"),
                                            network.getLine("NNL2AA1  BBE3AA1  1"));

        // Print the initial flow through each of the monitored lines.
        // These are the values of the "function reference" in the
        // JSON result file.
        LoadFlowParameters parameters = new LoadFlowParameters();
        LoadFlow.run(network, parameters);
        LOGGER.info("Initial active power through the four monitored lines");
        for (Line line : monitoredLines) {
            LOGGER.info("LINE {} - P: {} MW", line.getId(), line.getTerminal1().getP());
        }

        // Then build the factors themselves.
        List<SensitivityFactor> factors =  monitoredLines.stream()
                .map(l -> new SensitivityFactor(SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, l.getId(),
                                                SensitivityVariableType.TRANSFORMER_PHASE, "BBE2AA1  BBE3AA1  1",
                                                false, ContingencyContext.all())).collect(Collectors.toList());

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
        // Here we see that each line will undergo a variation of 28MW for a phase change of 1°
        // introduced by the PST (and -28MW for a -1° change).
        // The four monitored lines are affected identically, because in this very simple
        // network all lines have the same reactance.
        LOGGER.info("Initial sensitivity results");
        results.getPreContingencyValues().forEach(value ->
                LOGGER.info("Value: {} MW/°", value.getValue()));

        // 5. Perform a systematic sensitivity analysis
        // A systematic sensitivity analysis consists of a series of sensitivity calculations
        // performed on a network given a list of contingencies and sensitivity factors.
        // Here we use the systematic sensitivity feature of powsybl-open-loadflow, creating one variant on which all
        // the calculations are done successively, without re-loading the network each time, by
        // modifying the Jacobian matrix directly in the solver.

        // Here the list of contingencies is composed of the lines that are not monitored
        // in the sensitivity analysis.
        List<Contingency> contingencies = network.getLineStream()
            .filter(l -> monitoredLines.stream().noneMatch(l::equals))
            .map(l -> Contingency.branch(l.getId()))
            .collect(Collectors.toList());
        // This makes a total of 11 contingencies
        LOGGER.info("Number of contingencies: {}", contingencies.size());

        // Run the sensitivity computation with respect to the PST tap position on that network.
        SensitivityAnalysisResult results2 = SensitivityAnalysis.run(network, factors, contingencies);

        // Write the sensitivity results in a JSON temporary file. You can check the results in that file or specify your own file.
        File jsonResultsFile = File.createTempFile("sensitivity_results_", ".json");
        JsonUtil.createObjectMapper()
                .registerModule(new SecurityAnalysisJsonModule())
                .writerWithDefaultPrettyPrinter()
                .writeValue(jsonResultsFile, results2);
        LOGGER.info("Results written in file {}", jsonResultsFile);
    }

    private SensitivityTutorial() {
    }
}
