/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package powsybl.tutorials.sensitivity;

import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.EmptyContingencyListProvider;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.SensitivityAnalysis;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityFactorsProvider;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import com.powsybl.sensitivity.SensitivityAnalysisResult;
import com.powsybl.sensitivity.converter.CsvSensitivityAnalysisResultExporter;
import com.powsybl.sensitivity.converter.JsonSensitivityAnalysisResultExporter;
import com.powsybl.sensitivity.converter.SensitivityAnalysisResultExporter;
import com.powsybl.sensitivity.factors.BranchFlowPerPSTAngle;
import com.powsybl.sensitivity.factors.functions.BranchFlow;
import com.powsybl.sensitivity.factors.variables.PhaseTapChangerAngle;
import com.powsybl.sensitivity.json.JsonSensitivityAnalysisParameters;
import com.powsybl.sensitivity.json.SensitivityFactorsJsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Agnes Leroy {@literal <agnes.leroy at rte-france.com>}
 */
public final class SensitivityTutorialComplete {
    private static final Logger LOGGER = LoggerFactory.getLogger(SensitivityTutorialComplete.class);

    public static void main(String[] args) throws IOException {

        // 1. Import the network from a XML file
        // The network is described in the iTesla Internal Data Model format.
        Path networkPath = Paths.get(SensitivityTutorialComplete.class.getResource("/sensi_network_12_nodes.xml").getPath());
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
        // TODO: refactor the API to simplify this part - use a builder pattern?
        SensitivityFactorsProvider factorsProvider = net -> {
            List<SensitivityFactor> factors = new ArrayList<>();
            monitoredLines.forEach(l -> {
                String monitoredBranchId = l.getId();
                String monitoredBranchName = l.getName();
                BranchFlow branchFlow = new BranchFlow(monitoredBranchId, monitoredBranchName, l.getId());
                String twtId = network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getId();
                factors.add(new BranchFlowPerPSTAngle(branchFlow,
                        new PhaseTapChangerAngle(twtId, twtId, twtId)));
            });
            return factors;
        };

        // 3. Run the sensitivity analysis
        // Run the analysis that will be performed on network working variant with default sensitivity analysis parameters
        // Default implementation defined in the platform configuration will be used.
        SensitivityAnalysisResult sensiResults = SensitivityAnalysis.run(network, factorsProvider, new EmptyContingencyListProvider());

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
        sensiResults.getSensitivityValues().forEach(value ->
                LOGGER.info("Value: {} MW/°", value.getValue()));

        // Write the sensitivity results in a JSON file.
        // You can check the results in the sensitivity/src/main/resources/sensi_result.json file.
        // TODO: modify POWSYBL to fill the variable reference value in the results, at the moment it is NaN
        Path jsonSensiResultPath = Paths.get(SensitivityTutorialComplete.class.getResource("/sensi_result.json").getPath());
        File jsonSensiResultFile = new File(jsonSensiResultPath.toString());
        // If the file doesn't exist, create it
        if (!jsonSensiResultFile.exists()) {
            boolean fileCreated = jsonSensiResultFile.createNewFile();
            if (!fileCreated) {
                throw new IOException("Unable to create the result file");
            }
        }
        SensitivityAnalysisResultExporter jsonExporter = new JsonSensitivityAnalysisResultExporter();
        try (FileOutputStream os = new FileOutputStream(jsonSensiResultFile.toString())) {
            jsonExporter.export(sensiResults, new OutputStreamWriter(os));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // 5. Perform a systematic sensitivity analysis
        // A systematic sensitivity analysis consists of a series of sensitivity calculations
        // performed on a network given a list of contingencies and sensitivity factors.
        // Here we use the systematic sensitivity feature of Hades2, creating one variant on which all
        // the calculations are done successively, without re-loading the network each time, by
        // modifying the Jacobian matrix directly in the solver.

        // First, implement a contingencies provider.
        // Here the list of contingencies is composed of the lines that are not monitored
        // in the sensitivity analysis.
        ContingenciesProvider contingenciesProvider = n -> n.getLineStream()
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
                .map(l -> new Contingency(l.getId(), new BranchContingency(l.getId())))
                .collect(Collectors.toList());
        // This makes a total of 11 contingencies
        LOGGER.info("Number of contingencies: {}", contingenciesProvider.getContingencies(network).size());

        // Now, read factors from a JSON file (this is the second example of how to create factors, actually
        // we created the same twice)
        Path factorsFile = Paths.get(SensitivityTutorialComplete.class.getResource("/factors.json").getPath());
        SensitivityFactorsProvider jsonFactorsProvider = net -> {
            try (InputStream is = Files.newInputStream(factorsFile)) {
                return SensitivityFactorsJsonSerializer.read(new InputStreamReader(is));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };

        // Run the sensitivity computation with respect to the PST tap position on that network.

        Path parametersFile = Paths.get(SensitivityTutorialComplete.class.getResource("/sensi_parameters.json").getPath());
        SensitivityAnalysisParameters params = SensitivityAnalysisParameters.load();
        JsonSensitivityAnalysisParameters.update(params, parametersFile);

        SensitivityAnalysisResult systematicSensiResults = SensitivityAnalysis.run(network, jsonFactorsProvider, contingenciesProvider, params);

        // Export the results to a CSV file
        Path csvResultPath = Paths.get(SensitivityTutorialComplete.class.getResource("/sensi_syst_result.json").getPath());
        File csvResultFile = new File(csvResultPath.toString());
        // If the file doesn't exist, create it
        if (!csvResultFile.exists()) {
            boolean fileCreated = csvResultFile.createNewFile();
            if (!fileCreated) {
                throw new IOException("Unable to create the systematic sensi result file");
            }
        }
        SensitivityAnalysisResultExporter csvExporter = new CsvSensitivityAnalysisResultExporter();
        try (FileOutputStream os = new FileOutputStream(csvResultFile.toString())) {
            csvExporter.export(systematicSensiResults, new OutputStreamWriter(os));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private SensitivityTutorialComplete() {
    }
}
