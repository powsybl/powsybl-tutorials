/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package powsybl.tutorials.sensitivity;

import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.iidm.xml.NetworkXml;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.*;
import com.powsybl.sensitivity.converter.CsvSensitivityComputationResultExporter;
import com.powsybl.sensitivity.converter.JsonSensitivityComputationResultExporter;
import com.powsybl.sensitivity.converter.SensitivityComputationResultExporter;
import com.powsybl.sensitivity.factors.BranchFlowPerPSTAngle;
import com.powsybl.sensitivity.factors.functions.BranchFlow;
import com.powsybl.sensitivity.factors.variables.PhaseTapChangerAngle;
import com.powsybl.sensitivity.json.JsonSensitivityComputationParameters;
import com.powsybl.sensitivity.json.SensitivityFactorsJsonSerializer;
import com.rte_france.powsybl.hades2.sensitivity.Hades2SensitivityFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Agnes Leroy <agnes.leroy at rte-france.com>
 */
public final class SensitivityTutorial {
    private static final Logger LOGGER = LoggerFactory.getLogger(SensitivityTutorial.class);

    public static void main(String[] args) throws IOException {
        LOGGER.info("Starting the sensitivity tutorial execution");
    }

    private SensitivityTutorial() {
    }
}

