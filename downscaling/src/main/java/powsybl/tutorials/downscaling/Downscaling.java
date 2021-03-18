package powsybl.tutorials.downscaling;

import com.github.jsonldjava.shaded.com.google.common.collect.Streams;
import com.google.common.collect.Range;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.metrix.mapping.*;
import com.powsybl.metrix.mapping.timeseries.InMemoryTimeSeriesStore;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public final class Downscaling {

    private static final Logger LOGGER = LoggerFactory.getLogger(Downscaling.class);

    public static void main(String[] args) throws IOException, URISyntaxException {
        // Load all networks from resources
        // Each network covers one country
        final Set<Network> networks = loadNetworks();

        // Load ts store
        final ReadOnlyTimeSeriesStore tsStore = initTSStore();

        // Load mapping groovy script. Build a DSL loader with it
        // This DSL loader will be later used when performing mapping
        final Path mappingFilePath = Paths.get(Downscaling.class.getClassLoader().getResource("mapping.groovy").toURI());
        TimeSeriesDslLoader dslLoader;
        try (Reader reader = Files.newBufferedReader(mappingFilePath)) {
            dslLoader = new TimeSeriesDslLoader(reader, mappingFilePath.getFileName().toString());
        }

        // Prepare an output directory (from path in arguments)
        // Both logs and mapping results will be saved into it
        final Path outputPath = Paths.get(args[0]);

        // Iterate over each network to perform mapping
        for (final Network network : networks) {
            // Register the timeseries to use for this network's mapping
            // For each country, we need :
            //    - A LOAD_<country name> TS (for loads mapping)
            //    - An <energy type>_<country_name> TS for each of the networks energy type (for generators mapping)
            // Iterate over all generators in the network to know which energy types will be required
            Country country = network.getCountries().iterator().next();
            Set<String> tsNames = new HashSet<>();
            Streams.stream(network.getGenerators())
                   .map(Generator::getEnergySource)
                   .distinct()
                   .forEach(eSource -> {
                       tsNames.add(eSource.toString() + "_" + country.toString());
                   });
            tsNames.add("LOAD_" + country.toString());

            // Build mapping config for this network
            //     - mapping parameters : control mapping behavior (loaded from config.yml)
            //     - computation range : control timeseries versions span for the mapping (here only first version)
            final MappingParameters mappingParameters = MappingParameters.load();
            final ComputationRange computationRange = new ComputationRange(tsStore.getTimeSeriesDataVersions(), 1, 1);
            final TimeSeriesMappingConfig mappingConfig = dslLoader.load(network, mappingParameters, tsStore, computationRange);
            mappingConfig.setMappedTimeSeriesNames(tsNames);

            // Initialize mapping parameters
            final TimeSeriesMapperParameters tsMappingParams = new TimeSeriesMapperParameters(
                        new TreeSet<>(tsStore.getTimeSeriesDataVersions()),
                        Range.closed(0, mappingConfig.checkIndexUnicity(tsStore).getPointCount() - 1),
                        true,
                        true,
                        mappingParameters.getToleranceThreshold()
            );

            // Init output for this network : create a directory with the country name
            // equipment writer will produce a CSV file for each version (eg: version_1.csv)
            // logger will produce a logfile containing all warning information about mapping operation
            final Path networkOutputDir = outputPath.resolve(country.getName());
            Files.createDirectories(networkOutputDir);
            TimeSeriesMapperObserver equipmentWriter = new EquipmentTimeSeriesWriter(networkOutputDir);
            TimeSeriesMappingLogger logger = new TimeSeriesMappingLogger();

            // Perform mapping
            TimeSeriesMapper mapper = new TimeSeriesMapper(mappingConfig, network, logger);
            mapper.mapToNetwork(tsStore, tsMappingParams, Collections.singletonList(equipmentWriter));
            logger.writeCsv(networkOutputDir.resolve("mapping.log"));
        }
    }

    /**
     * Load TS store from resources ts-test.csv file
     *
     * @return a ReadOnlyTimeSeriesStore containing all timeseries
     */
    private static ReadOnlyTimeSeriesStore initTSStore() {
        final InMemoryTimeSeriesStore store = new InMemoryTimeSeriesStore();
        InputStreamReader isr = new InputStreamReader(Downscaling.class.getClassLoader().getResourceAsStream("ts-test.csv"));
        final BufferedReader reader = new BufferedReader(isr);
        store.importTimeSeries(reader);
        return store;
    }

    /**
     * Load all networks in CGMES format. Ignore invalid import files.
     * Iterate over each file in "networks" directory
     * If the file is a zipfile, try to load it as a CGMES network input
     *
     * @return produce a set containing al loaded networks
     */
    private static Set<Network> loadNetworks() throws IOException, URISyntaxException {
        Set<Network> networks = new HashSet<>();
        final URL networksDir = Downscaling.class.getClassLoader().getResource("networks");
        Files.walk(Paths.get(networksDir.toURI()))
             .filter(Files::isRegularFile)
             .filter(f -> f.toString().endsWith(".zip"))
             .forEach(zipFile -> {
                 try {
                     final Network network = Importers.loadNetwork(zipFile.toFile().toString());
                     networks.add(network);
                 } catch (Exception e) {
                     LOGGER.error("Could not load network from file [" + zipFile.getFileName().toString() + "]", e);
                 }
             });
        return networks;
    }

    private Downscaling() {
    }
}
