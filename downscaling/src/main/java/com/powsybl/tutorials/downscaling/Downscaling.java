package com.powsybl.tutorials.downscaling;

import com.google.common.collect.Range;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.datasource.DataSourceUtil;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.metrix.commons.ComputationRange;
import com.powsybl.metrix.commons.data.datatable.DataTableStore;
import com.powsybl.metrix.commons.data.timeseries.InMemoryTimeSeriesStore;
import com.powsybl.metrix.commons.observer.TimeSeriesMapperObserver;
import com.powsybl.metrix.mapping.MappingParameters;
import com.powsybl.metrix.mapping.NetworkPointWriter;
import com.powsybl.metrix.mapping.TimeSeriesDslLoader;
import com.powsybl.metrix.mapping.TimeSeriesMapper;
import com.powsybl.metrix.mapping.TimeSeriesMapperParameters;
import com.powsybl.metrix.mapping.TimeSeriesMappingLogger;
import com.powsybl.metrix.mapping.config.TimeSeriesMappingConfig;
import com.powsybl.metrix.mapping.config.TimeSeriesMappingConfigTableLoader;
import com.powsybl.metrix.mapping.observer.EquipmentTimeSeriesWriterObserver;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

public final class Downscaling {

    private static final Logger LOGGER = LoggerFactory.getLogger(Downscaling.class);

    public static void main(String[] args) throws IOException, URISyntaxException {
        LOGGER.warn("args[0]: {}", args[0]);
        // Load all networks from resources
        // Each network corresponds to one country
        final Set<Network> networks = loadNetworks();

        // Load the time series store
        final ReadOnlyTimeSeriesStore tsStore = initTSStore();

        // Load mapping groovy script. Build a DSL loader with it
        // This DSL loader will be later used when performing mapping
        final Path mappingFilePath = Paths.get(Objects.requireNonNull(Downscaling.class.getClassLoader().getResource("mapping.groovy")).toURI());
        TimeSeriesDslLoader dslLoader;
        try (Reader reader = Files.newBufferedReader(mappingFilePath)) {
            dslLoader = new TimeSeriesDslLoader(reader, mappingFilePath.getFileName().toString());
        }

        // Iterate over each network to perform mapping
        for (final Network network : networks) {
            // Register the time series to use for this network mapping
            // For each country, we need:
            //    - A LOAD_<country name> TS (for loads mapping)
            //    - An <energy type>_<country_name> TS for each of the networks energy type (for generators mapping)
            // Iterate over all generators in the network to know which energy types will be required
            Country country = network.getCountries().iterator().next();
            Set<String> tsNames = new HashSet<>();
            network.getGeneratorStream()
                   .map(Generator::getEnergySource)
                   .distinct()
                   .forEach(eSource -> tsNames.add(eSource.toString() + "_" + country.toString()));
            tsNames.add("LOAD_" + country.toString());

            // Build mapping config for this network
            //     - mapping parameters: control mapping behavior (loaded from config.yml)
            //     - computation range : control time series versions span for the mapping (here only first version)
            final MappingParameters mappingParameters = MappingParameters.load();
            final ComputationRange computationRange = new ComputationRange(tsStore.getTimeSeriesDataVersions(), 1, 1);
            final TimeSeriesMappingConfig mappingConfig = dslLoader.load(network, mappingParameters, tsStore, new DataTableStore(), computationRange);
            mappingConfig.setMappedTimeSeriesNames(tsNames);

            // Initialize mapping parameters
            final TimeSeriesMappingConfigTableLoader loader = new TimeSeriesMappingConfigTableLoader(mappingConfig, tsStore);
            final Range<Integer> pointRange = Range.closed(0, loader.checkIndexUnicity().getPointCount() - 1);
            final TimeSeriesMapperParameters tsMappingParams = new TimeSeriesMapperParameters(
                        new TreeSet<>(tsStore.getTimeSeriesDataVersions()),
                        pointRange,
                        true,
                        true,
                        false,
                        mappingParameters.getToleranceThreshold()
            );

            // Prepare an output directory (from the path in arguments)
            // Both logs and mapping results will be saved into it
            final Path outputPath = Paths.get(args[0]);
            // Init output for this network: create a directory with the country name
            // equipment writer will produce a CSV file for each version (eg: version_1.csv)
            // logger will produce a logfile containing all warning information about mapping operation
            final Path networkOutputDir = outputPath.resolve(country.getName());
            Files.createDirectories(networkOutputDir);
            final TimeSeriesMapperObserver equipmentWriter = new EquipmentTimeSeriesWriterObserver(network, mappingConfig, 10, pointRange, networkOutputDir);
            final DataSource dataSource = DataSourceUtil.createDataSource(networkOutputDir, "network", null, null);
            final TimeSeriesMapperObserver networkPointWriter = new NetworkPointWriter(network, dataSource);
            final List<TimeSeriesMapperObserver> observers = List.of(equipmentWriter, networkPointWriter);
            TimeSeriesMappingLogger logger = new TimeSeriesMappingLogger();

            // Perform mapping
            TimeSeriesMapper mapper = new TimeSeriesMapper(mappingConfig, tsMappingParams, network, logger);
            mapper.mapToNetwork(tsStore, observers);
            logger.writeCsv(networkOutputDir.resolve("mapping.log"));
        }
    }

    /**
     * Load TS store from resources ts-test.csv file
     *
     * @return a ReadOnlyTimeSeriesStore containing all time series
     */
    private static ReadOnlyTimeSeriesStore initTSStore() {
        final InMemoryTimeSeriesStore store = new InMemoryTimeSeriesStore();
        try (InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(Downscaling.class.getClassLoader().getResourceAsStream("ts-test.csv")));
            BufferedReader reader = new BufferedReader(isr)) {
            store.importTimeSeries(reader);
        } catch (IOException e) {
            throw new PowsyblException("Failed to load time series", e);
        }
        return store;
    }

    /**
     * Load all networks in CGMES format. Ignore invalid import files.
     * Iterate over each file in the "networks" directory
     * If the file is a zip file, try to load it as a CGMES network input
     *
     * @return produce a set containing al loaded networks
     */
    private static Set<Network> loadNetworks() throws IOException, URISyntaxException {
        Set<Network> networks = new HashSet<>();
        final URL networksDir = Downscaling.class.getClassLoader().getResource("networks");
        final Path networksDirPath = Paths.get(Objects.requireNonNull(networksDir).toURI());
        try (Stream<Path> walk = Files.walk(networksDirPath)) {
            walk.filter(Files::isRegularFile)
                .filter(f -> f.toString().endsWith(".zip"))
                .forEach(zipFile -> {
                    try {
                        final Network network = Network.read(zipFile.toFile().toString());
                        networks.add(network);
                    } catch (Exception e) {
                        String msg = "Could not load network from file [" + zipFile.getFileName().toString() + "]";
                        LOGGER.error(msg, e);
                    }
                });
        }
        return networks;
    }

    private Downscaling() {
    }
}
