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
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.validation.ValidationConfig;
import com.powsybl.loadflow.validation.ValidationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class LoadFlowValidationTutorialTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadFlowValidationTutorialTest.class);

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
                Arguments.of(ValidationType.BUSES, false, true),
                Arguments.of(ValidationType.FLOWS, false, true),
                Arguments.of(ValidationType.GENERATORS, false, true),
                Arguments.of(ValidationType.SVCS, true, true),
                Arguments.of(ValidationType.TWTS, false, true),
                Arguments.of(ValidationType.TWTS3W, true, true)
        );
    }

    @Test
    void testValidationOfBuses() throws IOException {
        Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/network.xiidm"));
        assertThat(ValidationType.BUSES.check(network, ValidationConfig.load(), tmpDir)).isFalse();
        String result = """
                sim1 BUSES check
                id;characteristic;value
                VLGEN_0;incomingP;NOT_CALCULATED
                VLGEN_0;incomingQ;NOT_CALCULATED
                VLGEN_0;loadP;0.00000
                VLGEN_0;loadQ;0.00000
                VLHV1_0;incomingP;NOT_CALCULATED
                VLHV1_0;incomingQ;NOT_CALCULATED
                VLHV1_0;loadP;0.00000
                VLHV1_0;loadQ;0.00000
                VLHV2_0;incomingP;NOT_CALCULATED
                VLHV2_0;incomingQ;NOT_CALCULATED
                VLHV2_0;loadP;0.00000
                VLHV2_0;loadQ;0.00000
                VLLOAD_0;incomingP;NOT_CALCULATED
                VLLOAD_0;incomingQ;NOT_CALCULATED
                VLLOAD_0;loadP;NOT_CALCULATED
                VLLOAD_0;loadQ;NOT_CALCULATED
                """;
        assertThat(tmpDir.resolve("buses.csv")).content().isEqualTo(result);
        Generator generator = network.getGenerator("GEN");
        TwoWindingsTransformer transformer = network.getTwoWindingsTransformer("NGEN_NHV1");
        LOGGER.info("Validation before LoadFlow");
        LOGGER.info("GEN P: {} MW", generator.getTerminal().getP());
        LOGGER.info("Transformer P: {} MW", transformer.getTerminal1().getP());
        LOGGER.info("SUM : {}", generator.getTerminal().getP() + transformer.getTerminal1().getP());
        // loadflow validation
        LoadFlow.run(network, new LoadFlowParameters());
        // check loadflow validation after loadflow run
        assertThat(ValidationType.BUSES.check(network, ValidationConfig.load(), tmpDir)).isTrue();
        LOGGER.info("Validation after LoadFlow");
        LOGGER.info("GEN P: {} MW", generator.getTerminal().getP());
        LOGGER.info("Transformer P: {} MW", transformer.getTerminal1().getP());
        LOGGER.info("SUM : {}", generator.getTerminal().getP() + transformer.getTerminal1().getP());
        assertThat(tmpDir.resolve("buses.csv")).content().isEqualTo("""
                sim1 BUSES check
                id;characteristic;value
                VLGEN_0;incomingP;-1.06302e-05
                VLGEN_0;incomingQ;0.00000
                VLGEN_0;loadP;0.00000
                VLGEN_0;loadQ;0.00000
                VLHV1_0;incomingP;-0.00404266
                VLHV1_0;incomingQ;7.02223e-05
                VLHV1_0;loadP;0.00000
                VLHV1_0;loadQ;0.00000
                VLHV2_0;incomingP;2.34490e-09
                VLHV2_0;incomingQ;-1.18100e-08
                VLHV2_0;loadP;0.00000
                VLHV2_0;loadQ;0.00000
                VLLOAD_0;incomingP;-600.000
                VLLOAD_0;incomingQ;-200.000
                VLLOAD_0;loadP;600.000
                VLLOAD_0;loadQ;200.000
                """);
    }

    @Test
    void testValidationOfFlows() throws IOException {
        Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/network.xiidm"));
        assertThat(ValidationType.FLOWS.check(network, ValidationConfig.load(), tmpDir)).isFalse();
        // loadflow validation
        LoadFlow.run(network, new LoadFlowParameters());
        // check loadflow validation after loadflow run
        assertThat(ValidationType.FLOWS.check(network, ValidationConfig.load(), tmpDir)).isTrue();
    }

    @Test
    void testValidationOfGenerators() throws IOException {
        Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/network.xiidm"));
        assertThat(ValidationType.GENERATORS.check(network, ValidationConfig.load(), tmpDir)).isFalse();
        // loadflow validation
        LoadFlow.run(network, new LoadFlowParameters());
        // check loadflow validation after loadflow run
        assertThat(ValidationType.GENERATORS.check(network, ValidationConfig.load(), tmpDir)).isTrue();
    }

    @Test
    void testValidationOfSvc() throws IOException {
        Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/network.xiidm"));
        assertThat(ValidationType.SVCS.check(network, ValidationConfig.load(), tmpDir)).isTrue();
        // loadflow validation
        LoadFlow.run(network, new LoadFlowParameters());
        // check loadflow validation after loadflow run
        assertThat(ValidationType.SVCS.check(network, ValidationConfig.load(), tmpDir)).isTrue();
    }

    @Test
    void testValidationOfTWT() throws IOException {
        Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/network.xiidm"));
        assertThat(ValidationType.TWTS.check(network, ValidationConfig.load(), tmpDir)).isFalse();
        // loadflow validation
        LoadFlow.run(network, new LoadFlowParameters());
        // check loadflow validation after loadflow run
        assertThat(ValidationType.TWTS.check(network, ValidationConfig.load(), tmpDir)).isTrue();
    }

    @Test
    void testValidationOfTWT3W() throws IOException {
        Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/network.xiidm"));
        assertThat(ValidationType.TWTS3W.check(network, ValidationConfig.load(), tmpDir)).isTrue();
        // loadflow validation
        LoadFlow.run(network, new LoadFlowParameters());
        // check loadflow validation after loadflow run
        assertThat(ValidationType.TWTS3W.check(network, ValidationConfig.load(), tmpDir)).isTrue();
    }

}
