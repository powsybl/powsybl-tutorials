/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.tutorials.dynawo;

import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.EventModelsSupplier;
import com.powsybl.dynamicsimulation.groovy.DynamicModelGroovyExtension;
import com.powsybl.dynamicsimulation.groovy.GroovyDynamicModelsSupplier;
import com.powsybl.dynamicsimulation.groovy.GroovyExtension;
import com.powsybl.dynawo.DynawoSimulationParameters;
import com.powsybl.dynawo.DynawoSimulationProvider;
import com.powsybl.dynawo.algorithms.DynawoAlgorithmsConfig;
import com.powsybl.dynawo.models.events.EventDisconnectionBuilder;
import com.powsybl.dynawo.security.DynawoSecurityAnalysisProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.security.dynamic.DynamicSecurityAnalysisParameters;
import com.powsybl.security.dynamic.DynamicSecurityAnalysisRunParameters;
import com.powsybl.security.json.SecurityAnalysisResultSerializer;
import com.powsybl.security.results.PostContingencyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * @author Alice Caron {@literal <alice.caron at rte-france.com>}
 */
public final class DynawoSecurityAnalysis {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynawoSecurityAnalysis.class);

    private static final Path CASE_FOLDER =
        Path.of(DynawoSecurityAnalysis.class.getClassLoader().getResource("ieee14").getPath());

    private static final Path CONFIG_FOLDER =
            Path.of(DynawoSecurityAnalysis.class.getClassLoader().getResource("config").getPath());

    private DynawoSecurityAnalysis() {
    }

    public static void main(String[] args) throws Exception {
        // Need to set as first argument the arg to the user dynawo executable
        LOGGER.info("args[0]: {} (path to Dynawo algorithms executable)", args[0]);

        DynawoSecurityAnalysisProvider provider = new DynawoSecurityAnalysisProvider(new DynawoAlgorithmsConfig(
                Path.of(args[0]), true)
        );

        Network network = Network.read(CASE_FOLDER.resolve("IEEE14.iidm"));

        // Dynamic models supplier (within a groovy file) to get the list of dynamic models associated to the elements (load, generator...) within the network.
        // Uses for instance: LoadAlphaBeta, GeneratorSynchronous, exposed by the "Dynawo" provider.
        GroovyDynamicModelsSupplier dynamicModelsSupplier = new GroovyDynamicModelsSupplier(
            CASE_FOLDER.resolve("dynamicModels.groovy"),
            GroovyExtension.find(DynamicModelGroovyExtension.class, DynawoSimulationProvider.NAME));

        // List of contingencies to simulate, giving the network state after each contingency.
        // Here: loss of a line, then loss of a generator. The ids come from the IIDM network.
        List<Contingency> contingencies = List.of(
            Contingency.line("_BUS____1-BUS____5-1_AC", "_BUS____5_VL"),
            Contingency.generator("_GEN____2_SM"));

        // A ContingenciesProvider transforms the network into a list of contingencies; here the list is static.
        ContingenciesProvider contingenciesProvider = n -> contingencies;

        // In addition to contingencies, we can inject dated events (triggers, short circuits…) replayed
        // in EACH simulation. Here, we disconnect a line at t = 10 s. Use EventModelsSupplier.empty()
        // if you don't have any events.
        EventModelsSupplier eventModelsSupplier = (n, reportNode) -> List.of(
            EventDisconnectionBuilder.of(n, reportNode)
                .staticId("_BUS____9-BUS___10-1_AC")
                .startTime(10)
                .build());

        // Configure Dynawo parameter sets (.par), the solver, and especially the criteria file (.crt):
        DynawoSimulationParameters dynawoSimulationParameters = new DynawoSimulationParameters();
        try (InputStream models = Files.newInputStream(CONFIG_FOLDER.resolve("models.par"));
             InputStream networkPar = Files.newInputStream(CONFIG_FOLDER.resolve("network.par"));
             InputStream solvers = Files.newInputStream(CONFIG_FOLDER.resolve("solvers.par"))) {
            dynawoSimulationParameters
                .setModelsParameters(models)
                .setNetworkParameters(networkPar, "8")
                .setSolverParameters(solvers, "3")
                .setSolverType(DynawoSimulationParameters.SolverType.SIM)
                .setCriteriaFilePath(CONFIG_FOLDER.resolve("criteria.crt"));
        }

        // Configure dynamic security analysis parameters:
        //  - a temporal simulation window [startTime, stopTime] (here 0 -> 100 s)
        //  - a contingency delay: the instant (in s) at which the contingency is applied in each simulation.
        DynamicSecurityAnalysisParameters parameters = new DynamicSecurityAnalysisParameters()
            .setDynamicSimulationParameters(new DynamicSimulationParameters(0, 100))
            .setDynamicContingenciesParameters(
                new DynamicSecurityAnalysisParameters.ContingenciesParameters(10));
        parameters.getDynamicSimulationParameters()
            .addExtension(DynawoSimulationParameters.class, dynawoSimulationParameters);

        ComputationManager computationManager = LocalComputationManager.getDefault();
        DynamicSecurityAnalysisRunParameters runParameters = new DynamicSecurityAnalysisRunParameters()
            .setDynamicSecurityAnalysisParameters(parameters)
            .setEventModelsSupplier(eventModelsSupplier)
            .setComputationManager(computationManager);

        // Run the analysis
        SecurityAnalysisResult result = provider.run(
                network,
                VariantManagerConstants.INITIAL_VARIANT_ID,
                dynamicModelsSupplier,
                contingenciesProvider,
                runParameters).join().getResult();

        printResult(result);

        StringWriter json = new StringWriter();
        SecurityAnalysisResultSerializer.write(result, json);
        System.out.println("\n===== Full result (JSON) =====");
        System.out.println(json);

        computationManager.close();
    }

    /** Pretty print the pre-contingency status and each contingency with its potential limit violations. */
    private static void printResult(SecurityAnalysisResult result) {
        System.out.println("\n===== Analysis Summary =====");

        // State "N" (before any contingency): reference operating point.
        System.out.println("Pre-contingency: status = " + result.getPreContingencyResult().getStatus());
        result.getPreContingencyResult().getLimitViolationsResult().getLimitViolations()
            .forEach(v -> System.out.println("   violation : " + formatViolation(v)));

        // State "N-1" (per contingency): status CONVERGED / FAILED / … + violated criteria.
        for (PostContingencyResult pcr : result.getPostContingencyResults()) {
            System.out.println("Contingency '" + pcr.getContingency().getId() + "': status = " + pcr.getStatus());
            pcr.getLimitViolationsResult().getLimitViolations()
                .forEach(v -> System.out.println("   violation : " + formatViolation(v)));
        }
    }

    private static String formatViolation(com.powsybl.contingency.violations.LimitViolation v) {
        return v.getLimitType() + " on '" + v.getSubjectId() + "'"
            + " : value=" + v.getValue() + ", limit=" + v.getLimit()
            + (v.getLimitName() != null ? " (" + v.getLimitName() + ")" : "");
    }
}
