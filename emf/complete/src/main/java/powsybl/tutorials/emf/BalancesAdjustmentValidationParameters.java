/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package powsybl.tutorials.emf;

import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;

import java.util.*;

/**
 * @author Miora Ralambotiana <miora.ralambotiana at rte-france.com>
 */
public final class BalancesAdjustmentValidationParameters {

    private final Map<String, String> igmPaths = new HashMap<>();

    private String dataExchangesPath = null;
    private String outputDir = null;

    public static BalancesAdjustmentValidationParameters load() {
        BalancesAdjustmentValidationParameters parameters = new BalancesAdjustmentValidationParameters();
        PlatformConfig platformConfig = PlatformConfig.defaultConfig();

        ModuleConfig config = platformConfig.getModuleConfig("balances-adjustment-validation-parameters");
        config.getStringListProperty("igm-paths").forEach(path -> {
            String[] pathArray = path.split(",");
            parameters.putIgmPath(pathArray[0].replaceAll("\\s+", ""), pathArray[1].replaceAll("\\s+", ""));
        });
        parameters.setDataExchangesPath(config.getStringProperty("data-exchanges-path"));
        config.getOptionalStringProperty("output-dir").ifPresent(parameters::setOutputDir);
        return parameters;
    }

    private void putIgmPath(String name, String path) {
        igmPaths.put(name, path);
    }

    public Map<String, String> getIgmPaths() {
        return Collections.unmodifiableMap(igmPaths);
    }

    private void setDataExchangesPath(String dataExchangesPath) {
        this.dataExchangesPath = Objects.requireNonNull(dataExchangesPath);
    }

    public String getDataExchangesPath() {
        return dataExchangesPath;
    }

    public Optional<String> getOutputDir() {
        return Optional.ofNullable(outputDir);
    }

    private void setOutputDir(String outputDir) {
        this.outputDir = Objects.requireNonNull(outputDir);
    }

    private BalancesAdjustmentValidationParameters() {
    }
}
