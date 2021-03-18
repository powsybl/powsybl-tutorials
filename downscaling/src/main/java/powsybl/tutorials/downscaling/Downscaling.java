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

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
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
        final Set<Network> networks = loadNetworks();

        // Load ts store and prepare TS names to map
        final ReadOnlyTimeSeriesStore tsStore = initTSStore();

        // Load mapping file
        final Path mappingFilePath = Paths.get(Downscaling.class.getClassLoader().getResource("mapping.groovy").toURI());
        TimeSeriesDslLoader dslLoader;
        try (Reader reader = Files.newBufferedReader(mappingFilePath)) {
            dslLoader = new TimeSeriesDslLoader(reader, mappingFilePath.getFileName().toString());
        }

        // Load output
        final Path outputPath = Paths.get(args[0]);

        // 1. Init mapping params
        // Load default params
        final MappingParameters mappingParameters = MappingParameters.load();
        // Only first variant of each ts will be used
        final ComputationRange computationRange = new ComputationRange(tsStore.getTimeSeriesDataVersions(), 1, 1);

        // Iterate over each network to perform mapping
        for (final Network network : networks) {
            // Retrieve indexes
            Country country = network.getCountries().iterator().next();
            Set<String> tsNames = new HashSet<>();
            Streams.stream(network.getGenerators())
                   .map(Generator::getEnergySource)
                   .distinct()
                   .forEach(eSource -> {
                       tsNames.add(eSource.toString() + "_" + country.toString());
                   });
            tsNames.add("LOAD_" + country.toString());

            // Load mapping config for this network
            final TimeSeriesMappingConfig mappingConfig = dslLoader.load(network, mappingParameters, tsStore, computationRange);
            mappingConfig.setMappedTimeSeriesNames(tsNames);

            // Init params
            final TimeSeriesMapperParameters tsMappingParams = new TimeSeriesMapperParameters(
                        new TreeSet<>(tsStore.getTimeSeriesDataVersions()),
                        Range.closed(0, mappingConfig.checkIndexUnicity(tsStore).getPointCount() - 1),
                        true,
                        true,
                        mappingParameters.getToleranceThreshold()
            );

            // Init output
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
     * Load TS store from resources
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
     *
     * @return produce a set containing al loaded networks
     */
    private static Set<Network> loadNetworks() throws IOException, URISyntaxException {
        Set<Network> networks = new HashSet<>();
        final URL networksDir = Downscaling.class.getClassLoader().getResource("networks");
        Files.walk(Paths.get(networksDir.toURI()))
             .filter(Files::isRegularFile)
             .filter(f -> f.endsWith(".zip"))
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
