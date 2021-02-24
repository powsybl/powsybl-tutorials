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
    private static final Logger LOG = LoggerFactory.getLogger(LoadflowTutorialComplete.class);

    public static void main(String[] args) {
        // Load network file
        final String iidmFileName = "eurostag-tutorial1-lf.xml";
        final InputStream is = LoadflowTutorialComplete.class.getClassLoader().getResourceAsStream(iidmFileName);
        Network network = Importers.loadNetwork(iidmFileName, is);

        // 1. Scan network topo: print substations and lines
        for (Substation substation : network.getSubstations()) {
            LOG.info("Substation " + substation.getNameOrId());
            LOG.info("Voltage levels :");
            for (VoltageLevel voltageLevel : substation.getVoltageLevels()) {
                LOG.info(" > " + voltageLevel.getNominalV());
            }
            LOG.info("Two windings transformers :");
            for (TwoWindingsTransformer twoWindingsTransfo : substation.getTwoWindingsTransformers()) {
                LOG.info(" > " + twoWindingsTransfo.getNameOrId());
            }
            LOG.info("Three windings transformers :");
            for (ThreeWindingsTransformer threeWindingsTransfo : substation.getThreeWindingsTransformers()) {
                LOG.info(" > " + threeWindingsTransfo.getNameOrId());
            }
        }

        printLines(network);
        // 2. Prepare loadflow calculation
        final String variantId = "loadflowVariant";
        network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, variantId);
        network.getVariantManager().setWorkingVariant(variantId);

        // 3. Run loadflow calculation
        final LoadFlowParameters loadflowParams = LoadFlowParameters.load();
        LoadFlow.run(network, loadflowParams);

        // 4. Output results
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
            LOG.info("Angle difference   : " + (angle - oldAngle));
            LOG.info("Tension difference : " + (v - oldV));
        }

        // Part 2: Apply contingency and perform loadflow computation again
        // Cut a line and create a new variant from resulting network
        network.getLine("NHV1_NHV2_1").getTerminal1().disconnect();
        network.getLine("NHV1_NHV2_1").getTerminal2().disconnect();

        final String contingencyVariantId = "contingencyLoadflowVariant";
        network.getVariantManager().cloneVariant(variantId, contingencyVariantId);
        network.getVariantManager().setWorkingVariant(contingencyVariantId);
        LoadFlow.run(network);

        // Show that lines are cut
        printLines(network);

        // Visit network and display info about sources and loads
        final DefaultTopologyVisitor visitor = new DefaultTopologyVisitor() {
            @Override
            public void visitGenerator(Generator generator) {
                LOG.info("Generator : " + generator.getNameOrId() + " [" + generator.getTerminal().getP() + " MW]");
            }

            @Override
            public void visitLoad(Load load) {
                LOG.info("Load : " + load.getNameOrId() + " [" + load.getTerminal().getP() + " MW]");
            }
        };
        // Run visitor on each voltage level
        for (VoltageLevel voltageLevel : network.getVoltageLevels()) {
            voltageLevel.visitEquipments(visitor);
        }
    }

    /**
     * Print info on all lines in provided network
     *
     * @param network any network
     */
    private static void printLines(Network network) {
        for (Line line : network.getLines()) {
            LOG.info("Line : " + line.getNameOrId());
            LOG.info(" > Terminal 1 power : " + line.getTerminal1().getP());
            LOG.info(" > Terminal 2 power : " + line.getTerminal2().getP());
        }
    }

    private LoadflowTutorialComplete() {
    }

}
