/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.tutorials.sa;

import com.powsybl.action.*;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.iidm.network.*;
import com.powsybl.security.SecurityAnalysis;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.security.SecurityAnalysisRunParameters;
import com.powsybl.security.condition.TrueCondition;
import com.powsybl.security.limitreduction.LimitReduction;
import com.powsybl.security.strategy.ConditionalActions;
import com.powsybl.security.strategy.OperatorStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;

/**
 * @author Samir Romdhani {@literal <samir.romdhani_externe at rte-france.com>}
 */
public final class SecurityAnalysisTutorials {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityAnalysisTutorials.class);

    private SecurityAnalysisTutorials() {
    }

    public static void main(String[] args) {
        // Network and Contingency
        log(runSecurityAnalysisUseCase1());
        // Network, Contingency, Operator Strategies and Actions
        log(runSecurityAnalysisUseCase2());
        // Network, Contingency, and Limit Reductions
        log(runSecurityAnalysisUseCase3());
    }

    public static SecurityAnalysisResult runSecurityAnalysisUseCase1() {
        Network network = Network.read("network.xml", SecurityAnalysisTutorials.class.getResourceAsStream("/network.xiidm"));
        LOGGER.info(":: SecurityAnalysis :: network and contingency");

        network.getLine("NHV1_NHV2_1")
                .getOrCreateSelectedOperationalLimitsGroup1("DEFAULT")
                .newCurrentLimits()
                .setPermanentLimit(460).add();
        network.getLine("NHV1_NHV2_1").setSelectedOperationalLimitsGroup1("DEFAULT");

        Contingency contingency = Contingency.line("NHV1_NHV2_2");
        return SecurityAnalysis.run(network, List.of(contingency)).getResult();
    }

    public static SecurityAnalysisResult runSecurityAnalysisUseCase2() {
        Network network = Network.read("network.xml", SecurityAnalysisTutorials.class.getResourceAsStream("/network.xiidm"));
        LOGGER.info(":: SecurityAnalysis :: network, contingency, operator strategies and actions");

        network.getLine("NHV1_NHV2_1")
                .getOrCreateSelectedOperationalLimitsGroup1("DEFAULT")
                .newCurrentLimits()
                .setPermanentLimit(460).add();
        network.getLine("NHV1_NHV2_1").setSelectedOperationalLimitsGroup1("DEFAULT");

        Contingency contingency = Contingency.line("NHV1_NHV2_2");
        LoadAction loadAction = new LoadActionBuilder()
                .withId("loadActionId")
                .withLoadId("LOAD")
                .withActivePowerValue(300)
                .withRelativeValue(false)
                .build();
        GeneratorAction generatorAction = new GeneratorActionBuilder()
                .withId("generatorActionId")
                .withGeneratorId("GEN")
                .withActivePowerValue(300)
                .withActivePowerRelativeValue(false)
                .build();

        OperatorStrategy operatorStrategy = new OperatorStrategy("id1", ContingencyContext.specificContingency(contingency.getId()),
                List.of(new ConditionalActions("stage1", new TrueCondition(), List.of(loadAction.getId(), generatorAction.getId()))));

        SecurityAnalysisRunParameters parameters = new SecurityAnalysisRunParameters();
        parameters.addOperatorStrategy(operatorStrategy);
        parameters.addAction(loadAction);
        parameters.addAction(generatorAction);
        return SecurityAnalysis.run(network, List.of(contingency), parameters).getResult();
    }

    public static SecurityAnalysisResult runSecurityAnalysisUseCase3() {
        Network network = Network.read("network.xml", SecurityAnalysisTutorials.class.getResourceAsStream("/network.xiidm"));
        LOGGER.info(":: SecurityAnalysis :: network, contingency and limit reduction");

        network.getLine("NHV1_NHV2_1")
                .getOrCreateSelectedOperationalLimitsGroup1("DEFAULT")
                .newCurrentLimits()
                .setPermanentLimit(460).add();
        network.getLine("NHV1_NHV2_1").setSelectedOperationalLimitsGroup1("DEFAULT");

        Contingency contingency = Contingency.line("NHV1_NHV2_2");
        SecurityAnalysisRunParameters parameters = new SecurityAnalysisRunParameters();
        LimitReduction limitReduction = LimitReduction.builder(LimitType.CURRENT, 0.9)
                .withMonitoringOnly(false)
                .withContingencyContext(ContingencyContext.all())
                .build();
        parameters.addLimitReduction(limitReduction);
        return SecurityAnalysis.run(network, List.of(contingency), parameters).getResult();
    }

    private static void log(SecurityAnalysisResult result) {
        LOGGER.info("\t Pre contingency results");
        result.getPreContingencyLimitViolationsResult().getLimitViolations()
                .forEach(value -> {
                    LOGGER.info("\t\t Value: {} MW/°", value.getValue());
                    LOGGER.info("\t\t Limit: {} MW/°", value.getLimit());
                });
        LOGGER.info("\t Post contingency results");
        result.getPostContingencyResults().forEach(postContingencyResult -> {
            LOGGER.info("\t\t Contingency : {}", postContingencyResult.getContingency().getId());
            postContingencyResult.getLimitViolationsResult().getLimitViolations()
                    .forEach(value -> {
                        LOGGER.info("\t\t\t Violation Value: {} MW/°", value.getValue());
                        LOGGER.info("\t\t\t Violation Limit: {} MW/°", value.getLimit());
                    });
        });
        LOGGER.info("\t Operator strategy results");
        result.getOperatorStrategyResults().forEach(operatorStrategyResult -> {
            LOGGER.info("\t\t OperatorStrategy : {}", operatorStrategyResult.getOperatorStrategy().getId());
            operatorStrategyResult.getLimitViolationsResult().getLimitViolations()
                    .forEach(value -> {
                        LOGGER.info("\t\t\t Violation Value: {} MW/°", value.getValue());
                        LOGGER.info("\t\t\t Violation Limit: {} MW/°", value.getLimit());
                    });
        });
    }
}
