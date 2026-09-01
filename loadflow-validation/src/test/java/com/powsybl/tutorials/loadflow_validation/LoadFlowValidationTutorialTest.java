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
import com.powsybl.iidm.network.Network;
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
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class LoadFlowValidationTutorialTest {

    protected FileSystem fileSystem;
    protected Path tmpDir;

    @BeforeEach
    public void setUp() throws IOException {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        tmpDir = Files.createDirectory(fileSystem.getPath("tmp"));
    }

    @ParameterizedTest
    @MethodSource("provideValidationElement")
    public void test(ValidationType type, boolean isValidBefore, boolean isValidAfter) throws IOException {
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
    public void testValidationOfBuses() throws IOException {
        Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/network.xiidm"));
        assertThat(ValidationType.BUSES.check(network, ValidationConfig.load(), tmpDir)).isEqualTo(false);
        // loadflow validation
        LoadFlow.run(network, new LoadFlowParameters());
        // check loadflow validation after loadflow run
        assertThat(ValidationType.BUSES.check(network, ValidationConfig.load(), tmpDir)).isEqualTo(true);
    }

    @Test
    public void testValidationOfFlows() throws IOException {
        Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/network.xiidm"));
        assertThat(ValidationType.FLOWS.check(network, ValidationConfig.load(), tmpDir)).isEqualTo(false);
        // loadflow validation
        LoadFlow.run(network, new LoadFlowParameters());
        // check loadflow validation after loadflow run
        assertThat(ValidationType.FLOWS.check(network, ValidationConfig.load(), tmpDir)).isEqualTo(true);
    }

    @Test
    public void testValidationOfGenerators() throws IOException {
        Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/network.xiidm"));
        assertThat(ValidationType.GENERATORS.check(network, ValidationConfig.load(), tmpDir)).isEqualTo(false);
        // loadflow validation
        LoadFlow.run(network, new LoadFlowParameters());
        // check loadflow validation after loadflow run
        assertThat(ValidationType.GENERATORS.check(network, ValidationConfig.load(), tmpDir)).isEqualTo(true);
    }

    @Test
    public void testValidationOfSvc() throws IOException {
        Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/network.xiidm"));
        assertThat(ValidationType.SVCS.check(network, ValidationConfig.load(), tmpDir)).isEqualTo(true);
        // loadflow validation
        LoadFlow.run(network, new LoadFlowParameters());
        // check loadflow validation after loadflow run
        assertThat(ValidationType.SVCS.check(network, ValidationConfig.load(), tmpDir)).isEqualTo(true);
    }

    @Test
    public void testValidationOfTWT() throws IOException {
        Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/network.xiidm"));
        assertThat(ValidationType.TWTS.check(network, ValidationConfig.load(), tmpDir)).isEqualTo(false);
        // loadflow validation
        LoadFlow.run(network, new LoadFlowParameters());
        // check loadflow validation after loadflow run
        assertThat(ValidationType.TWTS.check(network, ValidationConfig.load(), tmpDir)).isEqualTo(true);
    }

    @Test
    public void testValidationOfTWT3W() throws IOException {
        Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/network.xiidm"));
        assertThat(ValidationType.TWTS3W.check(network, ValidationConfig.load(), tmpDir)).isEqualTo(true);
        // loadflow validation
        LoadFlow.run(network, new LoadFlowParameters());
        // check loadflow validation after loadflow run
        assertThat(ValidationType.TWTS3W.check(network, ValidationConfig.load(), tmpDir)).isEqualTo(true);
    }

}
