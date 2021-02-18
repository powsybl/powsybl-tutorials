package powsybl.tutorials.flowtransfer.engine;

import java.util.Map;
import java.util.Objects;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FlowTransfer {

    private final String outage;
    private final double nFlow;
    private final Map<String, FlowCollection> flowCollections;

    public FlowTransfer(String outage, double nFlow, Map<String, FlowCollection> flowCollections) {
        this.outage = Objects.requireNonNull(outage);
        this.nFlow = nFlow;
        this.flowCollections = Objects.requireNonNull(flowCollections);
    }

    /**
     * Get the outage that is simulated.
     */
    public String getOutage() {
        return outage;
    }

    /**
     * Get the active power that flows on the branch before the contingency.
     */
    public double getNFlow() {
        return nFlow;
    }

    /**
     * Get the active power that flows on the other branch before and after the contingency.
     */
    public Map<String, FlowCollection> getFlowCollections() {
        return flowCollections;
    }
}
