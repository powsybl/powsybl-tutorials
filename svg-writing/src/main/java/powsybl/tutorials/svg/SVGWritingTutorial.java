package powsybl.tutorials.svg;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.FictitiousSwitchFactory;
import com.powsybl.sld.NetworkGraphBuilder;
import com.powsybl.sld.SubstationDiagram;
import com.powsybl.sld.VoltageLevelDiagram;
import com.powsybl.sld.layout.HorizontalSubstationLayoutFactory;
import com.powsybl.sld.layout.LayoutParameters;
import com.powsybl.sld.layout.PositionVoltageLevelLayoutFactory;
import com.powsybl.sld.layout.VoltageLevelLayoutFactory;
import com.powsybl.sld.layout.positionbyclustering.PositionByClustering;
import com.powsybl.sld.library.ComponentLibrary;
import com.powsybl.sld.library.ResourcesComponentLibrary;
import com.powsybl.sld.svg.DefaultDiagramLabelProvider;
import com.powsybl.sld.svg.DefaultSVGWriter;
import com.powsybl.sld.util.NominalVoltageDiagramStyleProvider;

import java.nio.file.Paths;

public final class SVGWritingTutorial {

    private SVGWritingTutorial() {
    }

    public static void main(String[] args) {
        // Style instantiation
        final ComponentLibrary componentLibrary = new ResourcesComponentLibrary("/ConvergenceLibrary");

        // Layout
        final VoltageLevelLayoutFactory voltageLevelLayoutFactory = new PositionVoltageLevelLayoutFactory(new PositionByClustering());
        final LayoutParameters layoutParameters = new LayoutParameters();

        // Params
        final boolean useName = true; // use name rather than ID on lines
        final String prefix = ""; // Prefix IDs of all diagram components

        // Load network
        Network network = FictitiousSwitchFactory.create();

        // Voltage diagrams
        // printVoltageDiagram("N", network, componentLibrary, voltageLevelLayoutFactory, layoutParameters, useName, prefix);
        // printVoltageDiagram("C", network, componentLibrary, voltageLevelLayoutFactory, layoutParameters, useName, prefix);

        // Complete substation diagram
        // printSubstationDiagram("A", componentLibrary, voltageLevelLayoutFactory, layoutParameters, useName, prefix, network);

        // Import CGMES archive
        String inputName = "D:\\projects\\powsybl\\powsybl-tutorials\\svg-writing\\src\\main\\resources\\CGMES_v2.zip";
        Network network2 = Importers.loadNetwork(inputName);
        // Print cgmes
        printVoltageDiagram("_8bbd7e74-ae20-4dce-8780-c20f8e18c2e0", network2, componentLibrary,
                voltageLevelLayoutFactory, layoutParameters, useName, prefix);
        printSubstationDiagram("_c49942d6-8b01-4b01-b5e8-f1180f84906c", network2, componentLibrary,
                voltageLevelLayoutFactory, layoutParameters, useName, prefix);

    }

    private static void printSubstationDiagram(String substationId, Network network, ComponentLibrary componentLibrary, VoltageLevelLayoutFactory voltageLevelLayoutFactory, LayoutParameters layoutParameters, boolean useName, String prefix) {
        SubstationDiagram substationDiagram = SubstationDiagram.build(
                new NetworkGraphBuilder(network),
                substationId,
                new HorizontalSubstationLayoutFactory(),
                voltageLevelLayoutFactory,
                useName
        );
        substationDiagram.writeSvg(
                prefix,
                new DefaultSVGWriter(componentLibrary, layoutParameters),
                Paths.get("/tmp/sub_" + substationId + ".svg"),
                new DefaultDiagramLabelProvider(network, componentLibrary, layoutParameters),
                new NominalVoltageDiagramStyleProvider(network)
        );
    }

    private static void printVoltageDiagram(String levelId, Network network, ComponentLibrary componentLibrary, VoltageLevelLayoutFactory voltageLevelLayoutFactory, LayoutParameters layoutParameters, boolean useName, String prefix) {
        VoltageLevelDiagram voltageLevelDiagram = VoltageLevelDiagram.build(
                new NetworkGraphBuilder(network),
                levelId,
                voltageLevelLayoutFactory,
                useName
        );
        // Output SVG
        voltageLevelDiagram.writeSvg(
                prefix,
                new DefaultSVGWriter(componentLibrary, layoutParameters),
                new DefaultDiagramLabelProvider(network, componentLibrary, layoutParameters),
                new NominalVoltageDiagramStyleProvider(network),
                Paths.get("/tmp/volt_" + levelId + ".svg")
        );
    }
}
