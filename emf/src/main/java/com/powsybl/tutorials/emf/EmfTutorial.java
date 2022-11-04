/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.tutorials.emf;

import com.powsybl.balances_adjustment.balance_computation.*;
import com.powsybl.balances_adjustment.util.NetworkArea;
import com.powsybl.balances_adjustment.util.NetworkAreaFactory;
import com.powsybl.balances_adjustment.util.NetworkAreaUtil;
import com.powsybl.cgmes.conversion.export.CgmesExportContext;
import com.powsybl.cgmes.conversion.export.StateVariablesExport;
import com.powsybl.cgmes.extensions.*;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.exceptions.UncheckedXmlStreamException;
import com.powsybl.commons.xml.XmlUtil;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.entsoe.cgmes.balances_adjustment.data_exchange.DataExchanges;
import com.powsybl.entsoe.cgmes.balances_adjustment.data_exchange.DataExchangesXml;
import com.powsybl.entsoe.cgmes.balances_adjustment.util.CgmesBoundariesAreaFactory;
import com.powsybl.entsoe.cgmes.balances_adjustment.util.CgmesVoltageLevelsAreaFactory;
import com.powsybl.iidm.mergingview.MergingView;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Miora Ralambotiana <miora.ralambotiana at rte-france.com>
 * @author Coline Piloquet <coline.piloquet at rte-france.com>
 */
public final class EmfTutorial {

    // Load flow settings for EMF, that could be relaxed in case of divergence.
    private static final LoadFlowParameters LOAD_FLOW_PARAMETERS = new LoadFlowParameters()
            .setVoltageInitMode(LoadFlowParameters.VoltageInitMode.DC_VALUES)
            .setBalanceType(LoadFlowParameters.BalanceType.PROPORTIONAL_TO_GENERATION_P_MAX)
            .setReadSlackBus(true)
            .setPhaseShifterRegulationOn(true)
            .setTransformerVoltageControlOn(true)
            .setConnectedComponentMode(LoadFlowParameters.ConnectedComponentMode.ALL);

    private static final boolean LOAD_FLOW_PREPROCESSING = true; // To run load flows on IGMs.
    private static final boolean PREPARE_BALANCE_COMPUTATION = true; // To run a balances ajustment.

    private static final String SYNCHRONOUS_AREA_ID = "10YEU-CONT-SYNC0";

    private static final Logger LOGGER = LoggerFactory.getLogger(EmfTutorial.class);

    public static void main(String[] args) throws IOException {

        BalancesAdjustmentValidationParameters validationParameters = BalancesAdjustmentValidationParameters.load();
        log(validationParameters);

        // Import CGMES networks.
        Map<String, Network> networks = importNetworks(validationParameters);

        // Load flow preprocessing on each IGMs.
        Map<String, Network> validNetworks = new HashMap<>(networks);
        if (LOAD_FLOW_PREPROCESSING) {
            loadflowPreProcessing(networks, validNetworks);
        }

        // Merging the IGMs using the merging view.
        MergingView mergingView = MergingView.create("merged", "validation");
        mergingView.merge(validNetworks.values().toArray(Network[]::new));

        // Run load flow on merging view before balances ajustment.
        if (!PREPARE_BALANCE_COMPUTATION) {
            LOAD_FLOW_PARAMETERS.setReadSlackBus(false);
            LOAD_FLOW_PARAMETERS.setDistributedSlack(true);
            LoadFlowResult result = LoadFlow.run(mergingView, LOAD_FLOW_PARAMETERS);
            for (Generator gen : mergingView.getGenerators()) {
                gen.setTargetP(-gen.getTerminal().getP()); // because of slack distibution on generators.
            }
            System.out.println(result.isOk());
            System.out.println(result.getMetrics());
        }

        // IGM AC net positions from PEVF file and scalables creation (you can specify if you prepare the balance computation or not).
        // The ficticious area is needed in case of partial merging.
        List<BalanceComputationArea> balanceComputationAreas = new ArrayList<>();
        DataExchanges dataExchanges;
        try (InputStream is = Files.newInputStream(Paths.get(validationParameters.getDataExchangesPath()))) {
            dataExchanges = DataExchangesXml.parse(is);
        }
        if (PREPARE_BALANCE_COMPUTATION) {
            igmPreprocessing(mergingView, validNetworks, dataExchanges, balanceComputationAreas, validationParameters);
            prepareFictitiousArea(mergingView, validNetworks, dataExchanges, balanceComputationAreas);
        } else {
            igmPreprocessing(mergingView, validNetworks, dataExchanges, validationParameters);
            prepareFictitiousArea(mergingView, validNetworks, dataExchanges);
        }

        if (PREPARE_BALANCE_COMPUTATION) {
            // Create Balance computation parameters.
            BalanceComputationParameters parameters = new BalanceComputationParameters(1, 10);
            LOAD_FLOW_PARAMETERS.setReadSlackBus(false);
            LOAD_FLOW_PARAMETERS.setDistributedSlack(true);
            LOAD_FLOW_PARAMETERS.setBalanceType(LoadFlowParameters.BalanceType.PROPORTIONAL_TO_GENERATION_P_MAX);
            parameters.setLoadFlowParameters(LOAD_FLOW_PARAMETERS);

            // Run the balances ajustment.
            BalanceComputation balanceComputation = new BalanceComputationFactoryImpl()
                    .create(balanceComputationAreas, LoadFlow.find(), LocalComputationManager.getDefault());
            BalanceComputationResult result = balanceComputation.run(mergingView, mergingView.getVariantManager().getWorkingVariantId(), parameters).join();
            System.out.println(result.getStatus());

            // Generate merged SV file for the CGM.
            validationParameters.getOutputDir().ifPresent(outputDir -> {
                try (OutputStream os = Files.newOutputStream(Paths.get(outputDir + "/SV.xml"))) {
                    XMLStreamWriter writer = XmlUtil.initializeWriter(true, "   ", os);
                    StateVariablesExport.write(mergingView, writer, createContext(mergingView, validNetworks));
                } catch (XMLStreamException e) {
                    throw new UncheckedXmlStreamException(e);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    private static Map<String, Network> importNetworks(BalancesAdjustmentValidationParameters validationParameters) {
        Map<String, Network> networks = new HashMap<>();
        validationParameters.getIgmPaths().forEach((name, path) -> networks.put(name, Network.read(Paths.get(path))));
        return networks;
    }

    private static void loadflowPreProcessing(Map<String, Network> networks, Map<String, Network> validNetworks) {
        // Run load flow on each IGM.
        networks.forEach((name, network) -> {
            LoadFlowResult result = LoadFlow.run(network, LOAD_FLOW_PARAMETERS);
            System.out.println(name + " loadflow: " + result.isOk());
            if (!result.isOk()) {
                LOGGER.error("Load flow did not converge on {}, try relaxing the loadflow parameters", name);
                validNetworks.remove(name);
            }
        });
    }

    private static void igmPreprocessing(Network mergingView, Map<String, Network> networks, DataExchanges dataExchanges,
                                         BalancesAdjustmentValidationParameters validationParameters) {
        igmPreprocessing(mergingView, networks, dataExchanges, null, validationParameters);
    }

    private static void igmPreprocessing(Network mergingView, Map<String, Network> networks, DataExchanges dataExchanges,
                                         List<BalanceComputationArea> balanceComputationAreas,
                                         BalancesAdjustmentValidationParameters validationParameters) {
        networks.forEach((name, network) -> {

            // Retrieve CGMES control area.
            CgmesControlArea controlArea = network.getExtension(CgmesControlAreas.class).getCgmesControlAreas().iterator().next();

            // Retrieve target AC net position.
            double target = dataExchanges.getNetPosition(SYNCHRONOUS_AREA_ID, controlArea.getEnergyIdentificationCodeEIC(), Instant.parse(network.getCaseDate().toString()));

            NetworkAreaFactory factory = createFactory(controlArea, network);
            NetworkArea area = factory.create(mergingView);
            Scalable scalable = NetworkAreaUtil.createConformLoadScalable(area);

            // Calculate AC net position.
            double real = area.getNetPosition();

            if (balanceComputationAreas != null) {
                balanceComputationAreas.add(new BalanceComputationArea(controlArea.getId(), factory, scalable, target));
            }

            if (!PREPARE_BALANCE_COMPUTATION) {
                System.out.println(name + ": " + target + " (target AC net position) / " + real + " (calculated AC net position)");
            }
        });
    }

    private static void prepareFictitiousArea(Network mergingView, Map<String, Network> validNetworks, DataExchanges dataExchanges) {
        prepareFictitiousArea(mergingView, validNetworks, dataExchanges, null);
    }

    private static void prepareFictitiousArea(Network mergingView, Map<String, Network> validNetworks, DataExchanges dataExchanges,
                                              List<BalanceComputationArea> balanceComputationAreas) {
        // Create dangling line scalables.
        List<CgmesControlArea> cgmesControlAreas = validNetworks.values().stream()
                .map(n -> n.getExtension(CgmesControlAreas.class))
                .filter(Objects::nonNull)
                .map(cgmesControlAreaList -> ((CgmesControlAreas) cgmesControlAreaList).getCgmesControlAreas().iterator().next())
                .collect(Collectors.toList());
        Scalable scalable = createACDanglingLineScalable(mergingView, cgmesControlAreas);

        if (scalable == null) {
            return; // Synchronous area is complete.
        }

        // Create fictitious CGMES control area.
        NetworkAreaFactory factory = new CgmesBoundariesAreaFactory(cgmesControlAreas);
        NetworkArea fictitiousArea = factory.create(mergingView);

        // Retrieve target AC net position of this ficticious area.
        Map<String, Double> fictitiousTargets = dataExchanges.getNetPositionsWithInDomainId(SYNCHRONOUS_AREA_ID,
                Instant.parse(validNetworks.values().iterator().next().getCaseDate().toString()));
        double definedTargets = fictitiousTargets.entrySet().stream()
                .filter(entry -> cgmesControlAreas.stream().anyMatch(area -> area.getEnergyIdentificationCodeEIC().equals(entry.getKey())))
                .mapToDouble(Map.Entry::getValue)
                .sum();
        double target = 0.0 - definedTargets;

        if (balanceComputationAreas != null) {
            balanceComputationAreas.add(new BalanceComputationArea(SYNCHRONOUS_AREA_ID, factory, scalable, target));
        }

        if (!PREPARE_BALANCE_COMPUTATION) {
            System.out.println(SYNCHRONOUS_AREA_ID + ": " + target + " (target AC net position) / " + fictitiousArea.getNetPosition() + " (calculated AC net position)");
        }
    }

    private static NetworkAreaFactory createFactory(CgmesControlArea area, Network network) {
        return new CgmesVoltageLevelsAreaFactory(area, null, network.getVoltageLevelStream().map(Identifiable::getId).collect(Collectors.toList()));
    }

    private static void log(BalancesAdjustmentValidationParameters validationParameters) {
        validationParameters.getOutputDir().ifPresent(outputDir -> {
            String fileName = outputDir + "/output-balances-ajustment.log";
            try {
                System.setOut(new PrintStream(fileName));
            } catch (FileNotFoundException e) {
                LOGGER.warn("Could not create log file {}", fileName, e);
            }
        });
    }

    private static CgmesExportContext createContext(MergingView mergingView, Map<String, Network> validNetworks) {
        CgmesExportContext context = new CgmesExportContext();
        context.setScenarioTime(mergingView.getCaseDate());
        validNetworks.forEach((name, n) -> {
            context.addIidmMappings(n);
            context.getSvModelDescription().addDependencies(n.getExtension(CgmesSvMetadata.class).getDependencies());
        });
        context.setTopologyKind(CgmesTopologyKind.MIXED_TOPOLOGY);
        return context;
    }

    private static Scalable createACDanglingLineScalable(Network mergingView, List<CgmesControlArea> areas) {
        List<DanglingLine> danglingLines = mergingView.getDanglingLineStream()
                .filter(dl -> dl.getExtension(CgmesDanglingLineBoundaryNode.class) == null || !dl.getExtension(CgmesDanglingLineBoundaryNode.class).isHvdc())
                .filter(dl -> dl.getTerminal().getBusView().getBus() != null && dl.getTerminal().getBusView().getBus().isInMainSynchronousComponent())
                .filter(dl -> areas.stream().anyMatch(area -> area.getTerminals().stream().anyMatch(t -> t.getConnectable().getId().equals(dl.getId()))
                        || area.getBoundaries().stream().anyMatch(b -> b.getConnectable().getId().equals(dl.getId()))))
                .collect(Collectors.toList());
        if (danglingLines.isEmpty()) {
            return null; // there is no dangling line in the CGM.
        }
        float totalP0 = (float) danglingLines.stream().mapToDouble(dl -> Math.abs(dl.getP0())).sum();
        if (totalP0 == 0.0) {
            throw new PowsyblException("The sum of all dangling lines' active power flows is zero, scaling is impossible");
        }
        List<Float> percentages = danglingLines.stream().map(dl -> (float) (100f * Math.abs(dl.getP0()) / totalP0)).collect(Collectors.toList());
        return Scalable.proportional(percentages, danglingLines.stream().map(dl -> Scalable.onDanglingLine(dl.getId(), Scalable.ScalingConvention.LOAD)).collect(Collectors.toList()));
    }

    private EmfTutorial() {
    }
}
