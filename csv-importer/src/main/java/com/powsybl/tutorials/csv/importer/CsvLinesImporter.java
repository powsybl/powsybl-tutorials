/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.tutorials.csv.importer;

import com.google.auto.service.AutoService;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Importer;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.NetworkFactory;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.VoltageLevel;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
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
        try (InputStream inputStream = dataSource.newInputStream(null, EXTENSION);
             CsvReader<NamedCsvRecord> csvReader = CsvReader.builder()
                 .fieldSeparator(",")
                 .quoteCharacter('"')
                 .ofNamedCsvRecord(inputStream)) {
            csvReader.forEach(rec -> {
                String id = rec.getField("LineId");
                LOGGER.info("import lineID {}", id);
                Substation s1 = getSubstation(rec.getField("SubstationId1"), network, Country.FR);
                Substation s2 = getSubstation(rec.getField("SubstationId2"), network, Country.BE);
                VoltageLevel vl1 = getVoltageLevel(rec.getField("VoltageLevelId1"), network, s1, 220, TopologyKind.BUS_BREAKER);
                VoltageLevel vl2 = getVoltageLevel(rec.getField("VoltageLevelId2"), network, s2, 220, TopologyKind.BUS_BREAKER);
                Bus nhv1 = getBus(vl1, rec.getField("BusId1"));
                Bus nhv2 = getBus(vl2, rec.getField("BusId2"));
                network.newLine()
                    .setId(id)
                    .setVoltageLevel1(vl1.getId())
                    .setVoltageLevel2(vl2.getId())
                    .setBus1(nhv1.getId())
                    .setConnectableBus1(nhv1.getId())
                    .setBus2(nhv2.getId())
                    .setConnectableBus2(nhv2.getId())
                    .setR(Double.parseDouble(rec.getField("R")))
                    .setX(Double.parseDouble(rec.getField("X")))
                    .setG1(Double.parseDouble(rec.getField("G1")))
                    .setB1(Double.parseDouble(rec.getField("B1")))
                    .setG2(Double.parseDouble(rec.getField("G2")))
                    .setB2(Double.parseDouble(rec.getField("B2")))
                    .add();
            });
            LOGGER.debug("{} import done", EXTENSION);

        } catch (IOException ioException) {
            throw new UncheckedIOException(ioException);
        }
        return network;
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
