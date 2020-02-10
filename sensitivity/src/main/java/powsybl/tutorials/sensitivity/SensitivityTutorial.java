package powsybl.tutorials.sensitivity;

import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.iidm.xml.NetworkXml;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.*;
import com.powsybl.sensitivity.converter.JsonSensitivityComputationResultExporter;
import com.powsybl.sensitivity.converter.SensitivityComputationResultExporter;
import com.powsybl.sensitivity.factors.BranchFlowPerPSTAngle;
import com.powsybl.sensitivity.factors.functions.BranchFlow;
import com.powsybl.sensitivity.factors.variables.PhaseTapChangerAngle;
import com.rte_france.powsybl.hades2.sensitivity.Hades2SensitivityFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Agnes Leroy <agnes.leroy at rte-france.com>
 */
public final class SensitivityTutorial {
    private static final Logger LOGGER = LoggerFactory.getLogger(SensitivityTutorial.class);
    private static final String CONTINGENCY_VARIANT = "contingencyVariant";

    public static void main(String[] args) throws IOException {

        // 1. Import the network from a XML file
        // The network is described in the iTesla Internal Data Model format.
        File file = new File(SensitivityTutorial.class.getResource("/sensi_network_12_nodes.xml").getPath());
        Network network = Importers.loadNetwork(file.toString());
        // This network consists of 12 buses:
        //     - we are considering schematic exchanges between France, Belgium, Germany and the Netherlands
        //     - there are 3 buses per country
        //     - there is one phase-shift transformer in Belgium
        //     - there are 4 lines we would wish to monitor in this case:
        //         - Belgium - France
        //         - France - Germany
        //         - Germany - the Netherlands
        //         - the Netherlands - Belgium
        //     - we are monitoring the effect of the phase-shift transformer or of the generators' injections on
        //       each of these lines.
        // In this tutorial the power flow is done on active power only, but this can be changed in the config file
        // by setting dcMode = false in the hades2-default-parameters. Then, the power flow will handle both P and Q.

        // In order to compute a sensitivity analysis on this network,
        // the sensitivity engine used should be defined in the local config file.
        // Here we use the Hades2 sensitivity engine, which comes together with the
        // loadflow. The configuration is thus the same as the loadflow.
        // Check the loadflow tutorial for more information on the configuration.

        // 2. Run an initial loadflow in order to output an iidm file with the P values
        // This is an optional section: it is not necessary to launch the loadflow explicitly
        // for the sensitivity study with the option computeInitialLoadflow: true.
        LoadFlow.run(network, LoadFlowParameters.load());
        // Write the IIDM network in xml format into a file in the results directory.
        String resourcesPath = Paths.get(SensitivityTutorial.class.getResource("/").getPath(),
                "../../src/main/resources").toString();
        Path iidmPath = Paths.get(resourcesPath, "./network_after_loadflow.xml").toAbsolutePath();
        File iidmFile = new File(iidmPath.toString());
        // If the file doesn't exist, create it.
        if (!iidmFile.exists()) {
            boolean fileCreated = iidmFile.createNewFile();
            if (!fileCreated) {
                throw new IOException("Unable to create the iidm file");
            }
        }
        // Write the data into the file.
        try (FileOutputStream os = new FileOutputStream(iidmFile.toString())) {
            NetworkXml.write(network, os);
        }

        // 3. Create a list of factors to be studied in the sensitivity computation
        // A standard sensitivity computation input is composed of a list of sensitivity factors,
        // each one composed of a sensitivity variable (the variable of impact) and a sensitivity function
        // (the observed function). They correspond to partial derivatives measuring the sensitivity
        // of the observed function (for example the flow through a line) with respect to the
        // variable of impact (for example a phase-shift transformer (PST) tap position or an injection).
        // There are four types of factors available, check the Powsybl website for more information.
        // Here we study the effect of the Belgian phase shift transformer on the flow through each of
        // the lines connecting the countries. We thus define these lines as monitoredBranch,
        // and create one factor of type BranchFlowPerPSTAngle for each monitoredBranch.
        // TODO: read the CNEC and the RA data from a CRAC file instead of hard-coding their definition?

        // Start by defining the monitored lines.
        List<Line> monitoredLines = new ArrayList<>();
        monitoredLines.add(network.getLine("BBE2AA1  FFR3AA1  1"));
        monitoredLines.add(network.getLine("FFR2AA1  DDE3AA1  1"));
        monitoredLines.add(network.getLine("DDE2AA1  NNL3AA1  1"));
        monitoredLines.add(network.getLine("NNL2AA1  BBE3AA1  1"));

        // Print the initial flow through each of the monitored lines.
        // These are the values of the "function reference" in the
        // JSON result file.
        LOGGER.info("Initial active power through the four monitored lines");
        for (Line line : monitoredLines) {
            LOGGER.info("LINE {} - P: {} MW", line.getId(), line.getTerminal1().getP());
        }

        // Then build the factors themselves.
        // TODO: refactor the API to simplify this part - use a builder pattern?
        SensitivityFactorsProvider twtFactorsProvider = net -> {
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

        // 4. Run the sensitivity analysis
        // Create a ComputationManager in order to perform the computations.
        // TODO: make some refactoring like in the loadflow to avoid explicitly creating the computation manager
        ComputationManager computationManager = LocalComputationManager.getDefault();
        // Create a SensitivityComputation object.
        // TODO: refactor also to do it like the loadflow
        SensitivityComputation sensitivityComputation = new Hades2SensitivityFactory().create(network,
                computationManager, 1);
        // Run the computation.
        SensitivityComputationResults sensitivityTwtComputationResults = sensitivityComputation.run(twtFactorsProvider,
                VariantManagerConstants.INITIAL_VARIANT_ID, SensitivityComputationParameters.load()).join();

        // 5. Output the results
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
        sensitivityTwtComputationResults.getSensitivityValues().forEach(value ->
                LOGGER.info("Value: {} MW/°", value.getValue()));

        // Write the sensitivity results in a JSON file.
        // You can check the results in the sensitivity/src/main/resources/sensi_result.json file.
        // TODO: modify POWSYBL to fill the variable reference value in the results, at the moment it is NaN
        Path jsonTwtResultPath = Paths.get(resourcesPath, "./sensi_result.json").toAbsolutePath();
        File jsonTwtResultFile = new File(jsonTwtResultPath.toString());
        // If the file doesn't exist, create it
        if (!jsonTwtResultFile.exists()) {
            boolean fileCreated = jsonTwtResultFile.createNewFile();
            if (!fileCreated) {
                throw new IOException("Unable to create the result file");
            }
        }
        SensitivityComputationResultExporter exporter = new JsonSensitivityComputationResultExporter();
        try (FileOutputStream os = new FileOutputStream(jsonTwtResultFile.toString())) {
            exporter.export(sensitivityTwtComputationResults, new OutputStreamWriter(os));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // 7. Perform a systematic sensitivity analysis
        // A systematic sensitivity analysis consists of a series of sensitivity calculations
        // performed on a network given a list of contingencies.
        // At the moment the systematic study is performed by creating a network variant for each
        // contingency, and running a sensitivity computation on each variant.
        // Another possibility would be to use the systematic sensitivity feature of Hades2
        // in the same way as the security analysis feature: creating one variant on which all
        // the calculations are done successively, without re-loading the network each time, by
        // modifying the Jacobian matrix directly in the solver.
        // TODO: implement the API for the Hades2 systematic sensitivity studies

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

        // For each contingency, create a variant of the network and run a sensitivity analysis.
        contingenciesProvider.getContingencies(network).forEach(cont -> {
            // Create a new variant and set it as the WorkingVariant.
            network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID,
                    CONTINGENCY_VARIANT);
            network.getVariantManager().setWorkingVariant(CONTINGENCY_VARIANT);
            // Apply the contingency on the network in that variant.
            cont.toTask().modify(network, computationManager);

            // Run the sensitivity computation with respect to the PST tap position on that network.
            SensitivityComputationResults contingencyResults = sensitivityComputation.run(
                    twtFactorsProvider, CONTINGENCY_VARIANT,
                    SensitivityComputationParameters.load()).join();

            // Print the sensitivity values in the terminal.
            // The values of the four factors are expressed in MW/°, which
            // means that for a 1° phase change introduced by the PST, the flow
            // through a given monitored line is modified by the value of the
            // factor (in MW).
            LOGGER.info("Contingency {}:", cont.getId());
            contingencyResults.getSensitivityValues().forEach(value ->
                LOGGER.info("Value: {} MW/°", value.getValue()));

            // Remove the variant corresponding to the current contingency.
            network.getVariantManager().removeVariant(CONTINGENCY_VARIANT);
        });
    }

    private SensitivityTutorial() {
    }
}
