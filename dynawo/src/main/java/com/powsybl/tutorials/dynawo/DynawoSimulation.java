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
import java.nio.file.Path;
import java.util.Objects;

public final class DynawoSimulation {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynawoSimulation.class);

    private static final String TMP_DIR = System.getProperty("java.io.tmpdir"); // put your own directory output path

    private DynawoSimulation() {

    }

    public static void main(String[] args) throws IOException {
        // Need to set as first argument the parg to the user dynawo executable
        LOGGER.info("args[0]: {} (path to Dynawo executable)", args[0]);

        DynawoSimulationProvider provider = new DynawoSimulationProvider(new DynawoSimulationConfig(
                Path.of(args[0]), true)
        );
        DynamicSimulationParameters parameters = new DynamicSimulationParameters()
                .setDebugDir(TMP_DIR + "/tutorials_dynamic_simulation/")
                .setStartTime(0)
                .setStopTime(100);
        DynawoSimulationParameters dynawoSimulationParameters = DynawoSimulationParameters.load();
        parameters.addExtension(DynawoSimulationParameters.class, dynawoSimulationParameters);

        String path = DynawoSimulation.class.getClassLoader().getResource("IEEE14.iidm").getPath();
        Network network = Network.newReader(path).read();

        GroovyDynamicModelsSupplier dynamicModelsSupplier = new GroovyDynamicModelsSupplier(
                getResourceAsStream("/ieee14/dynamicModels.groovy"),
                GroovyExtension.find(DynamicModelGroovyExtension.class, DynawoSimulationProvider.NAME));

        GroovyEventModelsSupplier eventModelsSupplier = new GroovyEventModelsSupplier(
                getResourceAsStream("/ieee14/eventModels.groovy"),
                GroovyExtension.find(EventModelGroovyExtension.class, DynawoSimulationProvider.NAME));

        GroovyOutputVariablesSupplier outputVariablesSupplier = new GroovyOutputVariablesSupplier(
                getResourceAsStream("/ieee14/outputVariables.groovy"),
                GroovyExtension.find(OutputVariableGroovyExtension.class, DynawoSimulationProvider.NAME));

        dynawoSimulationParameters.setModelsParameters(getResourceAsStream("/config/models.par"))
                .setNetworkParameters(getResourceAsStream("/config/network.par"), "8")
                .setSolverParameters(getResourceAsStream("/config/solvers.par"), "3")
                .setSolverType(DynawoSimulationParameters.SolverType.SIM)
//                .setSolverParameters(getResourceAsStream("/ieee14/solvers.par"), "2")
//                .setSolverType(DynawoSimulationParameters.SolverType.IDA)
                .setTimelineExportMode(ExportMode.XML);

        ReportNode reportNode = ReportNode.newRootReportNode()
            .withAllResourceBundlesFromClasspath()
            .withMessageTemplate("tutorial.dynawo.root")
            .build();

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

    protected static InputStream getResourceAsStream(String name) {
        return Objects.requireNonNull(DynawoSimulation.class.getResourceAsStream(name));
    }
}
