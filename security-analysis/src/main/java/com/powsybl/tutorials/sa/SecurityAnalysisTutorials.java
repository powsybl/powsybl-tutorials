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
import com.powsybl.security.monitor.StateMonitor;
import com.powsybl.security.strategy.ConditionalActions;
import com.powsybl.security.strategy.OperatorStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import static com.powsybl.contingency.ContingencyContextType.SPECIFIC;

/**
 * @author Samir Romdhani {@literal <samir.romdhani_externe at rte-france.com>}
 */
public final class SecurityAnalysisTutorials {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityAnalysisTutorials.class);

    private SecurityAnalysisTutorials() {
    }

    public static void main(String[] args) {
        // Network and contingency
        log(runSecurityAnalysisWithContingency());
        // Network, contingency, operator strategies and actions
        log(runSecurityAnalysisWithOperatorStrategyAndActions());
        // Network, contingency and limit reduction
        log(runSecurityAnalysisWithLimitReduction());
        // Network, contingency and state monitor
        log(runSecurityAnalysisWithStateMonitor());
    }

    public static SecurityAnalysisResult runSecurityAnalysisWithContingency() {
        Network network = Network.read("network.xml", SecurityAnalysisTutorials.class.getResourceAsStream("/network.xiidm"));
        LOGGER.info(":: SecurityAnalysis :: network and contingency");

        addLimitInLine1(network);
        Contingency contingency = Contingency.line("NHV1_NHV2_2");
        return SecurityAnalysis.run(network, List.of(contingency)).getResult();
    }

    public static SecurityAnalysisResult runSecurityAnalysisWithOperatorStrategyAndActions() {
        Network network = Network.read("network.xml", SecurityAnalysisTutorials.class.getResourceAsStream("/network.xiidm"));
        LOGGER.info(":: SecurityAnalysis :: network, contingency, operator strategies and actions");

        addLimitInLine1(network);
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

    public static SecurityAnalysisResult runSecurityAnalysisWithLimitReduction() {
        Network network = Network.read("network.xml", SecurityAnalysisTutorials.class.getResourceAsStream("/network.xiidm"));
        LOGGER.info(":: SecurityAnalysis :: network, contingency and limit reduction");

        addLimitInLine1(network);
        Contingency contingency = Contingency.line("NHV1_NHV2_2");

        SecurityAnalysisRunParameters parameters = new SecurityAnalysisRunParameters();
        LimitReduction limitReduction = LimitReduction.builder(LimitType.CURRENT, 0.9)
                .withMonitoringOnly(false)
                .withContingencyContext(ContingencyContext.all())
                .build();
        parameters.addLimitReduction(limitReduction);
        return SecurityAnalysis.run(network, List.of(contingency), parameters).getResult();
    }

    public static SecurityAnalysisResult runSecurityAnalysisWithStateMonitor() {
        Network network = Network.read("network.xml", SecurityAnalysisTutorials.class.getResourceAsStream("/network.xiidm"));
        LOGGER.info(":: SecurityAnalysis :: network, contingency and state Monitor");
        Contingency contingency = Contingency.line("NHV1_NHV2_2");
        SecurityAnalysisRunParameters parameters = new SecurityAnalysisRunParameters();
        StateMonitor stateMonitor = new StateMonitor(new ContingencyContext(contingency.getId(), SPECIFIC),
                Set.of("NHV1_NHV2_1"), // <= branch id
                Set.of("VLGEN", "VLHV1", "VLHV2", "VLLOAD"), // <= Voltage Levels id
                Set.of());
        parameters.addMonitor(stateMonitor);
        return SecurityAnalysis.run(network, List.of(contingency), parameters).getResult();
    }

    private static void log(SecurityAnalysisResult result) {
        LOGGER.info("\t Pre contingency results");
        result.getPreContingencyResult().getLimitViolationsResult().getLimitViolations()
                .forEach(limitViolation -> {
                    LOGGER.info("\t\t Value: {} MW/°", limitViolation.getValue());
                    LOGGER.info("\t\t Limit: {} MW/°", limitViolation.getLimit());
                });
        result.getPreContingencyResult().getNetworkResult().getBranchResults()
                .forEach(branchResult -> LOGGER.info("\t\t branchResult: {}", branchResult.toString()));
        result.getPreContingencyResult().getNetworkResult().getBusResults()
                .forEach(busResult -> LOGGER.info("\t\t busResult: {}", busResult.toString()));
        result.getPreContingencyResult().getNetworkResult().getThreeWindingsTransformerResults()
                .forEach(transformerResult -> LOGGER.info("\t\t TWT Result: {}", transformerResult.toString()));

        LOGGER.info("\t Post contingency results");
        result.getPostContingencyResults().forEach(postContingencyResult -> {
            LOGGER.info("\t\t Contingency : {}", postContingencyResult.getContingency().getId());
            postContingencyResult.getLimitViolationsResult().getLimitViolations()
                    .forEach(value -> {
                        LOGGER.info("\t\t\t Violation Value: {} MW/°", value.getValue());
                        LOGGER.info("\t\t\t Violation Limit: {} MW/°", value.getLimit());
                    });
            postContingencyResult.getNetworkResult().getBranchResults()
                    .forEach(branchResult -> LOGGER.info("\t\t branchResult: {}", branchResult.toString()));
            postContingencyResult.getNetworkResult().getBusResults()
                    .forEach(busResult -> LOGGER.info("\t\t busResult: {}", busResult.toString()));
            postContingencyResult.getNetworkResult().getThreeWindingsTransformerResults()
                    .forEach(transformerResult -> LOGGER.info("\t\t TWT Result: {}", transformerResult.toString()));
        });

        LOGGER.info("\t Operator strategy results");
        result.getOperatorStrategyResults().forEach(operatorStrategyResult -> {
            LOGGER.info("\t\t OperatorStrategy : {}", operatorStrategyResult.getOperatorStrategy().getId());
            operatorStrategyResult.getLimitViolationsResult().getLimitViolations()
                    .forEach(value -> {
                        LOGGER.info("\t\t\t Violation Value: {} MW/°", value.getValue());
                        LOGGER.info("\t\t\t Violation Limit: {} MW/°", value.getLimit());
                    });
            operatorStrategyResult.getNetworkResult().getBranchResults()
                    .forEach(branchResult -> LOGGER.info("\t\t branchResult: {}", branchResult.toString()));
            operatorStrategyResult.getNetworkResult().getBusResults()
                    .forEach(busResult -> LOGGER.info("\t\t busResult: {}", busResult.toString()));
            operatorStrategyResult.getNetworkResult().getThreeWindingsTransformerResults()
                    .forEach(transformerResult -> LOGGER.info("\t\t TWT Result: {}", transformerResult.toString()));
        });
    }

    private static void addLimitInLine1(Network network) {
        network.getLine("NHV1_NHV2_1")
                .getOrCreateSelectedOperationalLimitsGroup1("DEFAULT")
                .newCurrentLimits()
                .setPermanentLimit(460).add();
        network.getLine("NHV1_NHV2_1").setSelectedOperationalLimitsGroup1("DEFAULT");
    }
}
