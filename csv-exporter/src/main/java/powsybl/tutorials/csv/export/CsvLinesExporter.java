/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package powsybl.tutorials.csv.export;

import com.google.auto.service.AutoService;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.io.table.*;
import com.powsybl.iidm.export.Exporter;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

/**
 * @author Miora Ralambotiana <miora.ralambotiana at rte-france.com>
 */
@AutoService(Exporter.class)
public class CsvLinesExporter implements Exporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvLinesExporter.class);

    private static final String EXTENSION = "csv";
    private static final char CSV_SEPARATOR = ';';

    @Override
    public String getFormat() {
        return "CSV";
    }

    @Override
    public String getComment() {
        return "CSV exporter";
    }

    @Override
    public void export(Network network, Properties parameters, DataSource dataSource) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(dataSource);
        try {
            long startTime = System.currentTimeMillis();

            TableFormatterFactory factory = new CsvTableFormatterFactory();
            TableFormatterConfig tfc = new TableFormatterConfig(Locale.getDefault(), CSV_SEPARATOR, "N/A", true, false);

            try (Writer writer = new OutputStreamWriter(dataSource.newOutputStream(null, EXTENSION, false));
                 TableFormatter formatter = factory.create(writer, "", tfc,
                    new Column("LineId"),
                    new Column("SubstationId1"),
                    new Column("SubstationId2"),
                    new Column("VoltageLevelId1"),
                    new Column("VoltageLevelId2"),
                    new Column("BusId1"),
                    new Column("BusId2"),
                    new Column("R"),
                    new Column("X"),
                    new Column("G1"),
                    new Column("B1"),
                    new Column("G2"),
                    new Column("B2"))) {

                for (Line line : network.getLines()) {
                    VoltageLevel vl1 = line.getTerminal1().getVoltageLevel();
                    VoltageLevel vl2 = line.getTerminal2().getVoltageLevel();

                    Bus bus1 = line.getTerminal1().getBusBreakerView().getBus();
                    String bus1Id = (bus1 != null) ? bus1.getId() : "";

                    Bus bus2 = line.getTerminal2().getBusBreakerView().getBus();
                    String bus2Id = (bus2 != null) ? bus2.getId() : "";

                    LOGGER.debug("export lineID {} ", line.getId());
                    formatter.writeCell(line.getId())
                            .writeCell(vl1.getSubstation().getId())
                            .writeCell(vl2.getSubstation().getId())
                            .writeCell(vl1.getId())
                            .writeCell(vl2.getId())
                            .writeCell(bus1Id)
                            .writeCell(bus2Id)
                            .writeCell(line.getR())
                            .writeCell(line.getX())
                            .writeCell(line.getG1())
                            .writeCell(line.getB1())
                            .writeCell(line.getG2())
                            .writeCell(line.getB2());
                }
                LOGGER.info("CSV export done in {} ms", System.currentTimeMillis() - startTime);
            }
        } catch (IOException e) {
            LOGGER.error(e.toString(), e);
            throw new UncheckedIOException(e);
        }

    }

}
