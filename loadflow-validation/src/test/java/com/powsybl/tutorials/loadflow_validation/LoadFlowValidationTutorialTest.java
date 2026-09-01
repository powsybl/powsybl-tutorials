/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.tutorials.loadflow_validation;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.validation.ValidationConfig;
import com.powsybl.loadflow.validation.ValidationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class LoadFlowValidationTutorialTest {

    protected FileSystem fileSystem;
    protected Path tmpDir;

    @BeforeEach
    void setUp() throws IOException {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        tmpDir = Files.createDirectory(fileSystem.getPath("tmp"));
    }

    @ParameterizedTest
    @MethodSource("provideValidationElement")
    void test(ValidationType type, boolean isValidBefore, boolean isValidAfter) throws IOException {
        Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/network.xiidm"));
        ValidationConfig config = ValidationConfig.load();
        // check loadflow validation before loadflow run
        assertThat(type.check(network, config, tmpDir)).isEqualTo(isValidBefore);
        // loadflow validation
        LoadFlow.run(network, new LoadFlowParameters());
        // check loadflow validation after loadflow run
        assertThat(type.check(network, config, tmpDir)).isEqualTo(isValidAfter);
    }

    static Stream<Arguments> provideValidationElement() {
        // loadflow validation element | validation status before loadflow run |  validation status after loadflow run
        return Stream.of(
                Arguments.of(ValidationType.BUSES, false, false), // TODO
                Arguments.of(ValidationType.FLOWS, false, false), // TODO
                Arguments.of(ValidationType.GENERATORS, false, true),
                Arguments.of(ValidationType.SVCS, true, true),
                Arguments.of(ValidationType.TWTS, false, true),
                Arguments.of(ValidationType.TWTS3W, true, true)
        );
    }

    @Test
    void testValidationOfBuses() throws IOException {
        Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/network.xiidm"));
        assertThat(ValidationType.BUSES.check(network, ValidationConfig.load(), tmpDir)).isEqualTo(false);
        String result = """
                sim1 BUSES check
                id;characteristic;value
                VLGEN_0;incomingP;inv
                VLGEN_0;incomingQ;inv
                VLGEN_0;loadP;0.00000
                VLGEN_0;loadQ;0.00000
                VLHV1_0;incomingP;inv
                VLHV1_0;incomingQ;inv
                VLHV1_0;loadP;0.00000
                VLHV1_0;loadQ;0.00000
                VLHV2_0;incomingP;inv
                VLHV2_0;incomingQ;inv
                VLHV2_0;loadP;0.00000
                VLHV2_0;loadQ;0.00000
                VLLOAD_0;incomingP;inv
                VLLOAD_0;incomingQ;inv
                VLLOAD_0;loadP;inv
                VLLOAD_0;loadQ;inv
                """;
        // assertThat(tmpDir.resolve("buses.csv")).content().isEqualTo(result);
        // loadflow validation
        LoadFlow.run(network, new LoadFlowParameters());
        // check loadflow validation after loadflow run
        network.getBusView()
                .getBusStream()
                .sorted(Comparator.comparing(Bus::getId))
                .forEach(bus -> {
                    System.out.println("bus ID :: " + bus.getId());
                    System.out.println("    >>>> P :: " + bus.getP());
                    System.out.println("    >>>> Q :: " + bus.getQ());
                    double loadP = bus.getLoadStream()
                            .map(Load::getTerminal).mapToDouble(Terminal::getP).sum();
                    double loadQ = bus.getLoadStream()
                            .map(Load::getTerminal).mapToDouble(Terminal::getQ).sum();
                    System.out.println("    <<<< P :: " + loadP);
                    System.out.println("    <<<< Q :: " + loadQ);
                });
        assertThat(ValidationType.BUSES.check(network, ValidationConfig.load(), tmpDir)).isEqualTo(false);
        //[main] INFO com.powsybl.loadflow.validation.BusesValidation - Checking buses of network sim1
        //[main] WARN com.powsybl.loadflow.validation.BusesValidation - BUSES validation error: VLGEN_0 P -1.0630223528096394E-5 0.0
        //[main] WARN com.powsybl.loadflow.validation.BusesValidation - BUSES validation error: VLHV1_0 P -0.0040426552814096794 0.0
        //[main] WARN com.powsybl.loadflow.validation.BusesValidation - BUSES validation error: VLHV1_0 Q 7.022229112862988E-5 0.0
        //[main] WARN com.powsybl.loadflow.validation.BusesValidation - BUSES validation error: VLHV2_0 P 2.3449047148460522E-9 0.0
        //[main] WARN com.powsybl.loadflow.validation.BusesValidation - BUSES validation error: VLHV2_0 Q -1.1810016076196916E-8 0.0
        //[main] WARN com.powsybl.loadflow.validation.BusesValidation - BUSES validation error: VLLOAD_0 P -599.999999904206 600.0
        //[main] WARN com.powsybl.loadflow.validation.BusesValidation - BUSES validation error: VLLOAD_0 Q -199.999999996566 200.0
    }

    @Test
    void testValidationOfFlows() throws IOException {
        Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/network.xiidm"));
        assertThat(ValidationType.FLOWS.check(network, ValidationConfig.load(), tmpDir)).isEqualTo(false);
        // loadflow validation
        LoadFlow.run(network, new LoadFlowParameters());
        // check loadflow validation after loadflow run
        assertThat(ValidationType.FLOWS.check(network, ValidationConfig.load(), tmpDir)).isEqualTo(false); // TODO
    }

    @Test
    void testValidationOfGenerators() throws IOException {
        Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/network.xiidm"));
        assertThat(ValidationType.GENERATORS.check(network, ValidationConfig.load(), tmpDir)).isEqualTo(false);
        // loadflow validation
        LoadFlow.run(network, new LoadFlowParameters());
        // check loadflow validation after loadflow run
        assertThat(ValidationType.GENERATORS.check(network, ValidationConfig.load(), tmpDir)).isEqualTo(true);
    }

    @Test
    void testValidationOfSvc() throws IOException {
        Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/network.xiidm"));
        assertThat(ValidationType.SVCS.check(network, ValidationConfig.load(), tmpDir)).isEqualTo(true);
        // loadflow validation
        LoadFlow.run(network, new LoadFlowParameters());
        // check loadflow validation after loadflow run
        assertThat(ValidationType.SVCS.check(network, ValidationConfig.load(), tmpDir)).isEqualTo(true);
    }

    @Test
    void testValidationOfTWT() throws IOException {
        Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/network.xiidm"));
        assertThat(ValidationType.TWTS.check(network, ValidationConfig.load(), tmpDir)).isEqualTo(false);
        // loadflow validation
        LoadFlow.run(network, new LoadFlowParameters());
        // check loadflow validation after loadflow run
        assertThat(ValidationType.TWTS.check(network, ValidationConfig.load(), tmpDir)).isEqualTo(true);
    }

    @Test
    void testValidationOfTWT3W() throws IOException {
        Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/network.xiidm"));
        assertThat(ValidationType.TWTS3W.check(network, ValidationConfig.load(), tmpDir)).isEqualTo(true);
        // loadflow validation
        LoadFlow.run(network, new LoadFlowParameters());
        // check loadflow validation after loadflow run
        assertThat(ValidationType.TWTS3W.check(network, ValidationConfig.load(), tmpDir)).isEqualTo(true);
    }

}
