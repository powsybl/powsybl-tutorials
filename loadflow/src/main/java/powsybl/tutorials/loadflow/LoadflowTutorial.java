/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package powsybl.tutorials.loadflow;

import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.rte_france.powsybl.hades2.Hades2Factory;
import java.io.File;

/**
 * @author Anne Tilloy <anne.tilloy at rte-france.com>
 */
public final class LoadflowTutorial {

    public static void main(String[] args) {

        // This line imports the network from a XML file. The network is described in the
        // iTesla Internal Data Model format.
        File file = new File(LoadflowTutorial.class.getResource("/eurostag-tutorial1-lf.xml").getPath());
        Network network = Importers.loadNetwork(file.toString());

        // Let's scan the network.
        // This network is composed of two substations. Each substations have two voltage
        // levels and a two windings transformer.
        for (Substation substation : network.getSubstations()) {
            System.out.println("Substation " + substation.getName());
            System.out.println("Voltage levels: " + substation.getVoltageLevels());
            System.out.println("Two windings transformers: "
                    + substation.getTwoWindingsTransformers());
            System.out.println("Three windings transformers: "
                    + substation.getThreeWindingsTransformers());
        }
        // There are two lines in the network.
        for (Line l : network.getLines()) {
            System.out.println("Line: " + l.getName());
            System.out.println("Line: " + l.getTerminal1().getP());
            System.out.println("Line: " + l.getTerminal2().getP());
        }

        // These lines print the energy sources and the loads of the network through
        // a visitor.
        TopologyVisitor visitor = new DefaultTopologyVisitor() {
            @Override
            public void visitGenerator(Generator generator) {
                System.out.println("Generator " + generator.getName() + ": "
                        + generator.getTerminal().getP() + " MW");
            }
            @Override
            public void visitLoad(Load load) {
                System.out.println("Load " + load.getName() + ": "
                        + load.getTerminal().getP() + " MW");
            }
        };

        for (VoltageLevel vl : network.getVoltageLevels()) {
            vl.visitEquipments(visitor);
        }

        // We are going to compute a load flow on this network with Hades2 simulator.
        // This line defined the way we want to compute : locally by default.
        ComputationManager computationManager = LocalComputationManager.getDefault();
        // This line configures the load flow computation : with the simulator Hades2 and with
        // the computation manager defined before.
        LoadFlow loadflow = new Hades2Factory().create(network, computationManager, 0);

        // These are the parameters of the load flow. Here angles are set to zero and
        // tensions are set to one per unit.
        LoadFlowParameters loadflowParameters = new LoadFlowParameters()
                .setVoltageInitMode(LoadFlowParameters.VoltageInitMode.UNIFORM_VALUES);
        // The computation results will be stored in a variant of the network.
        // A variant of a network gathers all multi state variables (tensions, angles,
        // active and reactive powers, tap changer positions, hvdc converter modes,
        // switch positions, etc.)
        network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID,
                "loadflowVariant");
        // The computation starts here. Do not miss the join at the end.
        LoadFlowResult result = loadflow.run("loadflowVariant", loadflowParameters)
                .join();
        System.out.println(result.isOk());


        // Note that tensions and angles have been computed previously and are stored
        // in the initial variant. The following lines just compared the results.
        double angle;
        double v;
        double angleInitial;
        double vInitial;
        for (Bus bus : network.getBusView().getBuses()) {
            network.getVariantManager().setWorkingVariant(VariantManagerConstants.INITIAL_VARIANT_ID);
            angleInitial = bus.getAngle();
            vInitial = bus.getV();
            network.getVariantManager().setWorkingVariant("loadflowVariant");
            angle = bus.getAngle();
            v = bus.getV();
            System.out.println("Angle difference: " + (angle - angleInitial));
            System.out.println("Tension difference: " + (v - vInitial));
        }

        // The line "NHV1_NHV2_1" is disconnected through the following lines.
        network.getLine("NHV1_NHV2_1").getTerminal1().disconnect();
        network.getLine("NHV1_NHV2_1").getTerminal2().disconnect();

        // We are going to compute a load flow again.
        // We create a new variant of the network in order to store the results.
        network.getVariantManager().cloneVariant("loadflowVariant",
                "contingencyLoadflowVariant");
        LoadFlowResult result2 = loadflow.run("contingencyLoadflowVariant",
                loadflowParameters).join();
        System.out.println(result2.isOk());

        // Let's analyse the results.
        network.getVariantManager().setWorkingVariant("contingencyLoadflowVariant");
        for (Line l : network.getLines()) {
            System.out.println("Line: " + l.getName());
            System.out.println("Line: " + l.getTerminal1().getP());
            System.out.println("Line: " + l.getTerminal2().getP());
        }
        // The power now flows only on line NHV1_NHV2_2.
        for (VoltageLevel vl : network.getVoltageLevels()) {
            vl.visitEquipments(visitor);
        }

    }

    private LoadflowTutorial() {
    }

}
