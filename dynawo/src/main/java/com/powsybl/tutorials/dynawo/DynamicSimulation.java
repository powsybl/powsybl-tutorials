/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.tutorials.dynawo;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dynamicsimulation.*;
import com.powsybl.dynamicsimulation.groovy.*;
import com.powsybl.dynawo.DynawoSimulationConfig;
import com.powsybl.dynawo.DynawoSimulationParameters;
import com.powsybl.dynawo.DynawoSimulationProvider;
import com.powsybl.dynawo.commons.ExportMode;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Alice Caron {@literal <alice.caron at rte-france.com>}
 */
public final class DynamicSimulation {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicSimulation.class);

    private static final Path CASE_FOLDER =
            Path.of(DynamicSimulation.class.getClassLoader().getResource("ieee14").getPath());

    private static final Path CONFIG_FOLDER =
            Path.of(DynamicSimulation.class.getClassLoader().getResource("config").getPath());

    private DynamicSimulation() {

    }

    public static void main(String[] args) throws IOException {
        // Need to set as first argument the arg to the user dynawo executable
        LOGGER.info("args[0]: {} (path to Dynawo executable)", args[0]);

        DynawoSimulationProvider provider = new DynawoSimulationProvider(new DynawoSimulationConfig(
                Path.of(args[0]), true)
        );
        DynamicSimulationParameters parameters = DynamicSimulationParameters.load();
        DynawoSimulationParameters dynawoSimulationParameters = DynawoSimulationParameters.load();
        parameters.addExtension(DynawoSimulationParameters.class, dynawoSimulationParameters);

        Network network = Network.read(CASE_FOLDER.resolve("IEEE14.iidm"));

        try (InputStream models = Files.newInputStream(CONFIG_FOLDER.resolve("models.par"));
             InputStream networkPar = Files.newInputStream(CONFIG_FOLDER.resolve("network.par"));
             InputStream solvers = Files.newInputStream(CONFIG_FOLDER.resolve("solvers.par"))) {
            dynawoSimulationParameters
                    .setModelsParameters(models)
                    .setNetworkParameters(networkPar, "8")
                    .setSolverParameters(solvers, "3")
                    .setSolverType(DynawoSimulationParameters.SolverType.SIM)
                    .setTimelineExportMode(ExportMode.XML);
        }

        ReportNode reportNode = ReportNode.newRootReportNode()
            .withAllResourceBundlesFromClasspath()
            .withMessageTemplate("tutorial.dynawo.root")
            .build();

        GroovyDynamicModelsSupplier dynamicModelsSupplier = new GroovyDynamicModelsSupplier(
                CASE_FOLDER.resolve("dynamicModels.groovy"),
                GroovyExtension.find(DynamicModelGroovyExtension.class, DynawoSimulationProvider.NAME));

        GroovyEventModelsSupplier eventModelsSupplier = new GroovyEventModelsSupplier(
                CASE_FOLDER.resolve("eventModels.groovy"),
                GroovyExtension.find(EventModelGroovyExtension.class, DynawoSimulationProvider.NAME));

        GroovyOutputVariablesSupplier outputVariablesSupplier = new GroovyOutputVariablesSupplier(
                CASE_FOLDER.resolve("outputVariables.groovy"),
                GroovyExtension.find(OutputVariableGroovyExtension.class, DynawoSimulationProvider.NAME));

        DynamicSimulationRunParameters runParameters = new DynamicSimulationRunParameters()
            .setParameters(parameters)
            .setEventModelsSupplier(eventModelsSupplier)
            .setOutputVariablesSupplier(outputVariablesSupplier)
            .setReportNode(reportNode);

        provider.run(network, VariantManagerConstants.INITIAL_VARIANT_ID, dynamicModelsSupplier, runParameters).join();

        StringWriter sw = new StringWriter();
        reportNode.print(sw);
        System.out.println(sw);
    }
}
