/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package powsybl.tutorials.mergingview;

import com.powsybl.cgmes.conversion.export.CgmesExportContext;
import com.powsybl.cgmes.conversion.export.StateVariablesExport;
import com.powsybl.cgmes.conversion.extensions.CgmesSvMetadata;
import com.powsybl.commons.xml.XmlUtil;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.mergingview.MergingView;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.powsybl.iidm.xml.IidmXmlConstants.INDENT;

/**
 * @author Miora Ralambotiana <miora.ralambotiana at rte-france.com>
 */
public final class MergingViewTutorial {

    public static void main(String[] args) throws IOException, XMLStreamException {
        File fileBe = new File(MergingViewTutorial.class.getResource("/MicroGridTestConfiguration_T4_BE_BB_Complete_v2.zip").getPath());
        Network n1 = Importers.loadNetwork(fileBe.toString());
        File fileNl = new File(MergingViewTutorial.class.getResource("/MicroGridTestConfiguration_T4_NL_BB_Complete_v2.zip").getPath());
        Network n2 = Importers.loadNetwork(fileNl.toString());

        MergingView mergingView = MergingView.create("merge", "test");
        mergingView.merge(n1, n2);

        LoadFlowResult result = LoadFlow.run(mergingView, LoadFlowParameters.load());
        System.out.println(result.isOk());
        System.out.println(result.getMetrics());

        try (OutputStream os = Files.newOutputStream(Paths.get(System.getProperty("java.io.tmpdir") + "/MicroGridTestConfiguration_T4_BE_NL_BB_SV_v2_TEST.xml"))) { // put your own output path
            XMLStreamWriter writer = XmlUtil.initializeWriter(true, INDENT, os);
            StateVariablesExport.write(mergingView, writer, createContext(mergingView, n1, n2));
        }
    }

    private static CgmesExportContext createContext(MergingView mergingView, Network n1, Network n2) {
        CgmesExportContext context = new CgmesExportContext();
        context.setScenarioTime(mergingView.getCaseDate())
                .getSvModelDescription()
                .addDependencies(n1.getExtension(CgmesSvMetadata.class).getDependencies())
                .addDependencies(n2.getExtension(CgmesSvMetadata.class).getDependencies());
        return context;
    }

    private MergingViewTutorial() {
    }
}
