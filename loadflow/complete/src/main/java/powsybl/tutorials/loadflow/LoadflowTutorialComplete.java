package powsybl.tutorials.loadflow;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;


/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * @author Anne Tilloy <anne.tilloy at rte-france.com>
 */
public final class LoadflowTutorialComplete {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadflowTutorialComplete.class);

    public static void main(String[] args) {
        // We first import the network from a XML file. The network is described in the
        // iTesla Internal Data Model format.
        final String iidmFileName = "eurostag-tutorial1-lf.xml";
        final InputStream is = LoadflowTutorialComplete.class.getClassLoader().getResourceAsStream(iidmFileName);
        Network network = Importers.loadNetwork(iidmFileName, is);

        // Let's scan the network.
        // In this tutorial it is composed of two substations. Each substation has two voltage
        // levels and one two-windings transformer.
        for (Substation substation : network.getSubstations()) {
            LOGGER.info("Substation " + substation.getNameOrId());
            LOGGER.info("Voltage levels:");
            for (VoltageLevel voltageLevel : substation.getVoltageLevels()) {
                LOGGER.info(" > " + voltageLevel.getNominalV());
            }
            LOGGER.info("Two windings transformers:");
            for (TwoWindingsTransformer twoWindingsTransfo : substation.getTwoWindingsTransformers()) {
                LOGGER.info(" > " + twoWindingsTransfo.getNameOrId());
            }
            LOGGER.info("Three windings transformers:");
            for (ThreeWindingsTransformer threeWindingsTransfo : substation.getThreeWindingsTransformers()) {
                LOGGER.info(" > " + threeWindingsTransfo.getNameOrId());
            }
        }
        // There are two lines in the network.
        printLines(network);
        // We now define a visitor and use it to print the energy sources
        // and the loads of the network. Visitors are usually used to access
        // the network equipments efficiently, and modify their properties
        // for instance. Here we just print some data about the
        // Generators and Loads.
        final TopologyVisitor visitor = new DefaultTopologyVisitor() {
            @Override
            public void visitGenerator(Generator generator) {
                LOGGER.info("Generator: " + generator.getNameOrId() + " [" + generator.getTerminal().getP() + " MW]");
            }

            @Override
            public void visitLoad(Load load) {
                LOGGER.info("Load: " + load.getNameOrId() + " [" + load.getTerminal().getP() + " MW]");
            }
        };
        for (VoltageLevel voltageLevel : network.getVoltageLevels()) {
            voltageLevel.visitEquipments(visitor);
        }
        // We are going to compute a load flow on this network.
        // The load flow engine used should be defined in the config file.
        // When executing this tutorial with Maven, a custom config.yml is used
        // from the resources folder (check the pom.xml file).

        // Note that by default the load flow is computed from the initial variant of the network
        // and the computation results will be stored in it. Here we prefer to create a new variant.
        // A variant of a network gathers all multi state variables (voltages, angles,
        // active and reactive powers, tap changer positions, hvdc converter modes,
        // switch positions, etc.). For an example of a load-flow computation that does not
        // rely on several variants, please check the cgmes tutorial.
        final String variantId = "loadflowVariant";
        network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, variantId);
        network.getVariantManager().setWorkingVariant(variantId);

        // Below are the parameters of the load flow. Here angles are set to zero and
        // voltages are set to one per unit.
        LoadFlowParameters loadflowParams = new LoadFlowParameters()
                    .setVoltageInitMode(LoadFlowParameters.VoltageInitMode.UNIFORM_VALUES);
        LoadFlow.run(network, loadflowParams);
        displayLoadflowResults(network, variantId);

        // Apply contingency and perform loadflow computation again
        // Disconnect a line and create a new variant from resulting network
        network.getLine("NHV1_NHV2_1").getTerminal1().disconnect();
        network.getLine("NHV1_NHV2_1").getTerminal2().disconnect();

        // We are going to compute a load flow again.
        // We create a new variant of the network in order to store the results.
        final String contingencyVariantId = "contingencyLoadflowVariant";
        network.getVariantManager().cloneVariant(variantId, contingencyVariantId);
        network.getVariantManager().setWorkingVariant(contingencyVariantId);
        // This time, load parameters from config.yml file
        loadflowParams = LoadFlowParameters.load();
        LoadFlow.run(network, loadflowParams);
        LoadFlow.run(network);

        // Show that lines are cut
        printLines(network);
        // Run visitor on each voltage level
        for (VoltageLevel voltageLevel : network.getVoltageLevels()) {
            voltageLevel.visitEquipments(visitor);
        }
    }

    /**
     * Display loadflow results :for each bus of provided network, show angle and tension difference
     * between loadflow result and initial state
     *
     * @param network   a network
     * @param variantId variant of the network in which loadflow was calculated
     */
    private static void displayLoadflowResults(final Network network, final String variantId) {
        double angle;
        double v;
        double oldAngle;
        double oldV;
        for (Bus bus : network.getBusView().getBuses()) {
            network.getVariantManager().setWorkingVariant(VariantManagerConstants.INITIAL_VARIANT_ID);
            oldAngle = bus.getAngle();
            oldV = bus.getV();
            network.getVariantManager().setWorkingVariant(variantId);
            angle = bus.getAngle();
            v = bus.getV();
            LOGGER.info("Angle difference  : " + (angle - oldAngle));
            LOGGER.info("Tension difference: " + (v - oldV));
        }
    }

    /**
     * Print info on all lines in provided network
     *
     * @param network any network
     */
    private static void printLines(Network network) {
        for (Line line : network.getLines()) {
            LOGGER.info("Line: " + line.getNameOrId());
            LOGGER.info(" > Terminal 1 power: " + line.getTerminal1().getP());
            LOGGER.info(" > Terminal 2 power: " + line.getTerminal2().getP());
        }
    }

    private LoadflowTutorialComplete() {
    }

}
