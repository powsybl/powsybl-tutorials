package powsybl.tutorials.downscaling;

import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.datasource.DataSourceUtil;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.metrix.mapping.*;
import com.powsybl.metrix.mapping.timeseries.InMemoryTimeSeriesStore;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.TimeSeriesIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public final class Downscaling {

    private static final Logger LOG = LoggerFactory.getLogger(Downscaling.class);

    public static void main(String[] args) {
        // Load networks
        final List<Network> networks;
        try {
            networks = loadNetworks("D:\\projects\\powsybl\\powsybl-tutorials\\downscaling\\src\\main\\resources\\networks");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        for (final Network network : networks) {
            LOG.info("Network : " + network.getVoltageLevels());
        }

        System.exit(0);

        // Load network, ts store and mapping file
        final Network network = initNetwork();
        final ReadOnlyTimeSeriesStore store = initTSStore();
        final Path mappingFilePath = initMappingFile();

        // Init mapping params
        final MappingParameters mappingParameters = MappingParameters.load(); // Load default params
        final ComputationRange computationRange = new ComputationRange(store.getTimeSeriesDataVersions(), 1, 1);
        final TimeSeriesMappingLogger logger = new TimeSeriesMappingLogger();
        final List<TimeSeriesMapperObserver> obs = new ArrayList<>();

        // Create a mapping config
        final TimeSeriesMappingConfig mappingConfig = initMappingConfig(
                    network,
                    store,
                    mappingFilePath,
                    mappingParameters,
                    computationRange
        );
        mappingConfig.setMappedTimeSeriesNames(Sets.newHashSet("SE_L1", "SO_G2"));

        // Init output generation
        final TimeSeriesMappingConfigCsvWriter writer = new TimeSeriesMappingConfigCsvWriter(mappingConfig, network);
        final Path outputDir = Paths.get("D:\\tmp\\mapping");
        final Path outputNetwork = Paths.get("D:\\tmp\\mapping");
        final Path outputEquipment = Paths.get("D:\\tmp\\mapping");
        writer.writeMappingCsv(outputDir, store, computationRange, mappingParameters);

        // Also write network and equipment
        final DataSource dataSource = DataSourceUtil.createDataSource(outputNetwork, network.getId(), null);
        obs.add(new NetworkPointWriter(network, dataSource));
        obs.add(new EquipmentTimeSeriesWriter(outputEquipment));

        // Apply mapping to network
        final TimeSeriesMapper tsMapper = new TimeSeriesMapper(mappingConfig, network, logger);
        final TimeSeriesIndex tsIndex = mappingConfig.checkIndexUnicity(store);
        final int lastPoint = Math.min(1, tsIndex.getPointCount()) - 1;
        final TreeSet<Integer> versions = new TreeSet<>(store.getTimeSeriesDataVersions());
        final TimeSeriesMapperParameters tsMappingParams = new TimeSeriesMapperParameters(
                    versions,
                    Range.closed(0, lastPoint),
                    true,
                    true,
                    mappingParameters.getToleranceThreshold()
        );
        tsMapper.mapToNetwork(store, tsMappingParams, obs);

    }

    private static TimeSeriesMappingConfig initMappingConfig(Network network, ReadOnlyTimeSeriesStore store, Path mappingFilePath, MappingParameters mappingParameters, ComputationRange computationRange) {
        try (final Reader reader = Files.newBufferedReader(mappingFilePath)) {
            final TimeSeriesDslLoader dslLoader = new TimeSeriesDslLoader(reader, mappingFilePath.getFileName().toString());
            return dslLoader.load(network, mappingParameters, store, computationRange);
        } catch (IOException ioe) {
            throw new UncheckedIOException("Could not read mapping file", ioe);
        }
    }

    private static Path initMappingFile() {
        try {
            final URI uri = Downscaling.class.getClassLoader().getResource("mapping.groovy").toURI();
            return Paths.get(uri);
        } catch (URISyntaxException use) {
            throw new IllegalStateException("Could not find mapping file", use);
        }
    }

    private static Network initNetwork() {
        final InputStream networkIs = Downscaling.class.getClassLoader().getResourceAsStream("eurostag-tutorial1-lf.xml");
        return Importers.loadNetwork("eurostag-tutorial1-lf.xml", networkIs);
    }

    private static ReadOnlyTimeSeriesStore initTSStore() {
        try {
            final InMemoryTimeSeriesStore store = new InMemoryTimeSeriesStore();
            final Path path = Paths.get("D:\\vrac\\ts-test.csv");
            final BufferedReader reader = Files.newBufferedReader(path);
            store.importTimeSeries(reader);
            return store;
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    /**
     * Load all networks in CGMES format
     *
     * @return loaded networkds
     */
    private static List<Network> loadNetworks(final String rootPathStr) throws IOException {
        List<Network> networks = new ArrayList<>();
        final Path rootPath = Paths.get(rootPathStr);
        try (final DirectoryStream<Path> zipFiles = Files.newDirectoryStream(rootPath, p -> p.toString().endsWith(".zip"))) {
            zipFiles.forEach(zipFile -> {
                try {
                    final Network network = Importers.loadNetwork(zipFile);
                    networks.add(network);
                } catch (Exception e) {
                    LOG.error("Could not load network from file [" + zipFile.getFileName().toString() + "]", e);
                }
            });
        }
        return networks;
    }

    private Downscaling() {
    }
}
