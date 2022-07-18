/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.tutorials.csv.import_;

import com.csvreader.CsvReader;
import com.google.auto.service.AutoService;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.iidm.import_.Importer;
import com.powsybl.iidm.network.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * @author Miora Ralambotiana <miora.ralambotiana at rte-france.com>
 */
@AutoService(Importer.class)
public class CsvLinesImporter implements Importer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvLinesImporter.class);

    private static final String EXTENSION = "csv";

    @Override
    public String getFormat() {
        return "CSV";
    }

    @Override
    public String getComment() {
        return "CSV Importer";
    }

    @Override
    public boolean exists(ReadOnlyDataSource dataSource) {
        try {
            return dataSource.exists(null, EXTENSION);
        } catch (IOException e) {
            LOGGER.error(e.toString(), e);
            return false;
        }
    }

    @Override
    public Network importData(ReadOnlyDataSource dataSource, NetworkFactory networkFactory, Properties parameters) {
        Network network = networkFactory.createNetwork("Network_2Lines_Example", EXTENSION);
        LOGGER.debug("Start import from file {}", dataSource.getBaseName());
        try {
            CsvReader reader = new CsvReader(dataSource.newInputStream(null, EXTENSION), StandardCharsets.UTF_8);
            try {
                reader.readHeaders();
                while (reader.readRecord()) {
                    String id = reader.get("LineId");
                    LOGGER.info("import lineID {}", id);
                    Substation s1 = getSubstation(reader.get("SubstationId1"), network, Country.FR);
                    Substation s2 = getSubstation(reader.get("SubstationId2"), network, Country.BE);
                    VoltageLevel vl1 = getVoltageLevel(reader.get("VoltageLevelId1"), network, s1, 220, TopologyKind.BUS_BREAKER);
                    VoltageLevel vl2 = getVoltageLevel(reader.get("VoltageLevelId2"), network, s2, 220, TopologyKind.BUS_BREAKER);
                    Bus nhv1 = getBus(vl1, reader.get("BusId1"));
                    Bus nhv2 = getBus(vl2, reader.get("BusId2"));
                    network.newLine()
                            .setId(id)
                            .setVoltageLevel1(vl1.getId())
                            .setVoltageLevel2(vl2.getId())
                            .setBus1(nhv1.getId())
                            .setConnectableBus1(nhv1.getId())
                            .setBus2(nhv2.getId())
                            .setConnectableBus2(nhv2.getId())
                            .setR(Double.valueOf(reader.get("R")))
                            .setX(Double.valueOf(reader.get("X")))
                            .setG1(Double.valueOf(reader.get("G1")))
                            .setB1(Double.valueOf(reader.get("B1")))
                            .setG2(Double.valueOf(reader.get("G2")))
                            .setB2(Double.valueOf(reader.get("B2")))
                            .add();
                }
            } finally {
                reader.close();
            }
            LOGGER.debug("{} import done", EXTENSION);
            return network;

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Substation getSubstation(String id, Network network, Country country) {
        Substation substation = network.getSubstation(id);
        if (substation == null) {
            substation = network.newSubstation()
                    .setId(id)
                    .setCountry(country)
                    .add();
        }
        return substation;
    }

    private static VoltageLevel getVoltageLevel(String id, Network network, Substation s, double nominalVoltage, TopologyKind topologyKind) {
        VoltageLevel voltageLevel = network.getVoltageLevel(id);
        if (voltageLevel == null) {
            voltageLevel = s.newVoltageLevel()
                    .setId(id)
                    .setNominalV(nominalVoltage)
                    .setTopologyKind(topologyKind)
                    .add();
        }
        return voltageLevel;
    }

    private static Bus getBus(VoltageLevel vl, String id) {
        Bus bus = vl.getBusBreakerView().getBus(id);
        if (bus == null) {
            bus = vl.getBusBreakerView().newBus()
                    .setId(id)
                    .add();
        }
        return bus;
    }
}
