/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package powsybl.tutorials.cgmes;

import com.powsybl.commons.io.table.AsciiTableFormatter;
import com.powsybl.commons.io.table.Column;
import com.powsybl.commons.io.table.TableFormatterConfig;
import com.powsybl.iidm.export.Exporters;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Mathieu Bague <mathieu.bague@rte-france.com>
 */
public final class Merge {

    private Merge() {

    }

    public static void main(String[] args) throws IOException {
        Network cgm1 = merge(false);
        Exporters.export("XIIDM", cgm1, new Properties(), Paths.get("/tmp/cgm-tieline.xml"));
        Network cgm2 = merge(true);
        Exporters.export("XIIDM", cgm2, new Properties(), Paths.get("/tmp/cgm-xnodes.xml"));

        // compareTieLines(cgm1, cgm2);

        printBuses(cgm1, cgm2);
        printFlows(cgm1, cgm2);
    }

    private static Network merge(boolean withXnodes) {
        Network n1 = Importers.loadNetwork(Merge.class.getResource("/MicroGridTestConfiguration_T4_BE_BB_Complete_v2.zip").getPath());
        // Exporters.export("XIIDM", n1, new Properties(), Paths.get("/tmp/igm-be.xml"));

        Network n2 = Importers.loadNetwork(Merge.class.getResource("/MicroGridTestConfiguration_T4_NL_BB_Complete_v2.zip").getPath());
        // Exporters.export("XIIDM", n2, new Properties(), Paths.get("/tmp/igm-nl.xml"));

        Network cgm;
        if (withXnodes) {
            cgm = merge(n1, n2);
        } else {
            cgm = Network.create("cgm", "test");
            cgm.merge(n1, n2);
        }

        // Run a loadflow
        LoadFlow.find("OpenLoadFlow").run(cgm);
        // Exporters.export("XIIDM", cgm, new Properties(), Paths.get("/tmp/cgm-xnodes.xml"));

        return cgm;
    }

    private static void printBuses(Network cgm1, Network cgm2) throws IOException {
        try (AsciiTableFormatter formatter = new AsciiTableFormatter(new OutputStreamWriter(System.out), "Buses", TableFormatterConfig.load(),
                new Column("ID"), new Column("V1"), new Column("Angle1"), new Column("V2"), new Column("Angle2"), new Column("deltaV"), new Column("deltaAngle"))) {
            for (Bus bus : cgm1.getBusView().getBuses()) {
                if (!bus.getId().startsWith("TN_Border")) {
                    Bus bus2 = cgm2.getBusView().getBus(bus.getId());

                    double v1 = bus.getV();
                    double angle1 = bus.getAngle();
                    double v2 = bus2.getV();
                    double angle2 = bus2.getAngle();

                    formatter.writeCell(bus.getId())
                            .writeCell(v1).writeCell(angle1)
                            .writeCell(v2).writeCell(angle2)
                            .writeCell(Math.abs(v1 - v2)).writeCell(Math.abs(angle1 - angle2));
                }
            }
        }
    }

    private static void printFlows(Network cgm1, Network cgm2) throws IOException {
        try (AsciiTableFormatter formatter = new AsciiTableFormatter(new OutputStreamWriter(System.out), "Flows", TableFormatterConfig.load(),
                new Column("ID"), new Column("P1 (1)"), new Column("Q1 (1)"), new Column("P2 (1)"), new Column("Q2 (1)"),
                new Column("P1 (2)"), new Column("Q1 (2)"), new Column("P2 (2)"), new Column("Q2 (2)"),
                new Column("deltaP1"), new Column("deltaQ1"), new Column("deltaP2"), new Column("deltaQ2")
                )) {
            for (Branch branch : cgm1.getBranches()) {
                double p1_1 = branch.getTerminal1().getP();
                double q1_1 = branch.getTerminal1().getQ();
                double p2_1 = branch.getTerminal2().getP();
                double q2_1 = branch.getTerminal2().getQ();
                double p1_2;
                double q1_2;
                double p2_2;
                double q2_2;

                if (branch instanceof TieLine) {
                    String[] lineIds = branch.getId().split(" \\+ ");
                    Line line1 = cgm2.getLine(lineIds[0]);
                    Line line2 = cgm2.getLine(lineIds[1]);
                    // Swap lines if the name of the tieLine has been reordered
                    if (!line1.getTerminal1().getVoltageLevel().getId().equals(branch.getTerminal1().getVoltageLevel().getId())) {
                        Line tmp = line1;
                        line1 = line2;
                        line2 = tmp;
                    }

                    p1_2 = line1.getTerminal1().getP();
                    q1_2 = line1.getTerminal1().getQ();
                    p2_2 = line2.getTerminal1().getP();
                    q2_2 = line2.getTerminal1().getQ();
                } else {
                    Branch branch2 = cgm2.getBranch(branch.getId());
                    p1_2 = branch2.getTerminal1().getP();
                    q1_2 = branch2.getTerminal1().getQ();
                    p2_2 = branch2.getTerminal2().getP();
                    q2_2 = branch2.getTerminal2().getQ();
                }

                formatter.writeCell(branch.getId())
                        .writeCell(p1_1).writeCell(q1_1).writeCell(p2_1).writeCell(q2_1)
                        .writeCell(p1_2).writeCell(q1_2).writeCell(p2_2).writeCell(q2_2)
                        .writeCell(Math.abs(p1_1 - p1_2)).writeCell(Math.abs(q1_1 - q1_2)).writeCell(Math.abs(p2_1 - p2_2)).writeCell(Math.abs(q2_1 - q2_2));
            }
        }
    }

    private static void compareTieLines(Network cgm1, Network cgm2) throws IOException {
        List<TieLine> tieLines = cgm1.getLineStream().filter(Line::isTieLine).map(TieLine.class::cast).collect(Collectors.toList());
        for (TieLine tieLine : tieLines) {
            String[] lineIds = tieLine.getId().split(" \\+ ");
            Line line1 = cgm2.getLine(lineIds[0]);
            Line line2 = cgm2.getLine(lineIds[1]);

            try (AsciiTableFormatter formatter = new AsciiTableFormatter(new OutputStreamWriter(System.out), "Characteristics " + tieLine.getId(), TableFormatterConfig.load(),
                    new Column("Characteristic"), new Column("CGM1"), new Column("CGM2"))) {
                formatter.writeCell("r").writeCell(tieLine.getR()).writeCell(line1.getR() + line2.getR());
                formatter.writeCell("x").writeCell(tieLine.getX()).writeCell(line1.getX() + line2.getX());
                formatter.writeCell("g1").writeCell(tieLine.getG1()).writeCell(line1.getG1() + line1.getG2());
                formatter.writeCell("g2").writeCell(tieLine.getG2()).writeCell(line2.getG1() + line2.getG2());
                formatter.writeCell("b1").writeCell(tieLine.getB1()).writeCell(line1.getB1() + line1.getB2());
                formatter.writeCell("b2").writeCell(tieLine.getB2()).writeCell(line2.getB1() + line2.getB2());
            }
        }
    }

    private static Network merge(Network... others) {
        Network cgm = Network.create("cgm", "test");

        // Find matching danglingLines (by Xnode)
        Map<String, List<DanglingLine> > danglingLinesByXnode = new HashMap<>();
        for (Network igm : others) {
            igm.getDanglingLineStream().forEach(dl -> danglingLinesByXnode.computeIfAbsent(dl.getUcteXnodeCode(), k -> new ArrayList<>()).add(dl));
        }

        // Prepare the merge of the matching dangling lines
        List<Runnable> endTasks = new ArrayList<>();
        for (Map.Entry<String, List<DanglingLine>> entry : danglingLinesByXnode.entrySet()) {
            String xnode = entry.getKey();
            List<DanglingLine> danglingLines = entry.getValue();

            switch (danglingLines.size()) {
                case 1:  // The danglingLine is dangling, just skip
                    break;

                case 2:  // merge the two dangling lines
                    endTasks.addAll(merge(cgm, xnode, danglingLines.get(0), danglingLines.get(1)));
                    break;

                default:
                    throw new AssertionError(danglingLines.size() + " danglingLines are connected to the Xnode " + xnode);
            }
        }

        // Merge the networks
        cgm.merge(others);
        endTasks.forEach(Runnable::run);

        // TODO(mathbagu): Triple stores, properties...

        return cgm;
    }

    private static List<Runnable> merge(Network cgm, String xnode, DanglingLine dl1, DanglingLine dl2) {
        VoltageLevel vl1 = dl1.getTerminal().getVoltageLevel();

        // Step 1: create a substation, a voltage level and a bus for the XNode
        Substation s = cgm.newSubstation()
                .setId(xnode)
                .add();
        VoltageLevel vl = s.newVoltageLevel()
                .setId(xnode + "_VL")
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .setNominalV(vl1.getNominalV())  // FIXME(mathbagu): should assert that both VL have the same nominal voltage
                .add();
        vl.getBusBreakerView().newBus()
                .setId(xnode + "_BUS")
                .add();

        // Step 2: create the two lineAdder objects to replace the dangling lines
        Runnable runnable1 = createLineAdder(cgm, xnode, dl1);
        Runnable runnable2 = createLineAdder(cgm, xnode, dl2);

        // Step 3: remove the dangling lines
        dl1.remove();
        dl2.remove();

        return Arrays.asList(runnable1, runnable2);
    }

    private static Runnable createLineAdder(Network cgm, String xnode, DanglingLine dl) {
        Terminal terminal = dl.getTerminal();
        VoltageLevel voltageLevel = terminal.getVoltageLevel();

        LineAdder adder = cgm.newLine()
                .setId(dl.getId())
                .setName(dl.getName())
                .setR(dl.getR())
                .setX(dl.getX())
                .setG1(dl.getG())
                .setG2(0)
                .setB1(dl.getB())
                .setB2(0);

        // Connect terminal1 of the merged line to the terminal of the dangling line
        adder.setVoltageLevel1(voltageLevel.getId());
        if (voltageLevel.getTopologyKind() == TopologyKind.NODE_BREAKER) {
            adder.setNode1(terminal.getNodeBreakerView().getNode());
        } else {
            Optional.ofNullable(terminal.getBusBreakerView().getBus()).map(b -> adder.setBus1(b.getId()));
            Optional.ofNullable(terminal.getBusBreakerView().getConnectableBus()).map(b -> adder.setConnectableBus1(b.getId()));
        }

        // Connect terminal2 of the merged line to the Xnode
        adder.setVoltageLevel2(xnode + "_VL")
                .setBus2(terminal.isConnected() ? xnode + "_BUS" : null)
                .setConnectableBus2(xnode + "_BUS");

        // TODO(mathbagu): properties, currentLimits and extensions
        // TODO(mathbagu): what about P,Q ?
        return adder::add;
    }

}
