package powsybl.tutorials.loadflow;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;

import java.io.File;


public final class LoadflowTutorial {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadflowTutorial.class);

    public static void main(String[] args) {
        LOGGER.info("Starting the load flow tutorial execution");
    }

    private LoadflowTutorial() {
    }
}

