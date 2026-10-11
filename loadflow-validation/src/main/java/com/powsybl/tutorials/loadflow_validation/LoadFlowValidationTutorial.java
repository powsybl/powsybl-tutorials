/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.tutorials.loadflow_validation;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.validation.ValidationConfig;
import com.powsybl.loadflow.validation.ValidationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

public final class LoadFlowValidationTutorial {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadFlowValidationTutorial.class);

    public static void main(String[] args) throws IOException {
        LOGGER.info("Starting the load flow validation tutorial execution");
        final String networkFileName = "network.xml";
        Network network;
        try (InputStream is = LoadFlowValidationTutorial.class.getClassLoader().getResourceAsStream(networkFileName)) {
            network = Network.read(networkFileName, is);
        } catch (IOException e) {
            throw new PowsyblException("Could not load network from file [" + networkFileName + "]", e);
        }
        // loadflow run
        LoadFlow.run(network, new LoadFlowParameters());
        // loadflow validation
        ValidationConfig config = ValidationConfig.load();
        EnumSet<ValidationType> types = EnumSet.of(ValidationType.BUSES, ValidationType.FLOWS,
                ValidationType.GENERATORS, ValidationType.SVCS, ValidationType.TWTS);
        boolean allValid = true;
        Path outputFolder = Path.of("validation-results");
        Files.createDirectories(outputFolder);
        for (ValidationType type : types) {
            boolean valid = type.check(network, config, outputFolder);
            LOGGER.info("Validation ({}) : {}", type, valid);
            allValid &= valid;
        }
        LOGGER.info("Validation: {}", allValid);
    }

    private LoadFlowValidationTutorial() {
    }
}
