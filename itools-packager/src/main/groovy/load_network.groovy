import com.powsybl.iidm.network.Substation
/**
 * Main execution
 * @param args arguments provided by command-line
 */
def exec(String[] args) {
    println("> Exec start")
    println("> Check arguments validity")
    if (args.length == 0) {
        throw new IllegalArgumentException("Please provide an input file path")
    }
    // Load a network
    println("> Load network from file : " + args[0])
    def network = loadNetwork(args[0])
    // Say stuff about loaded network
    println("> Network loaded : has " + network.getSubstations().size() + " substations")
    for (substation in network.getSubstations()) {
        printSubstation(substation)
    }
}

/**
 * Print info about provided substation
 * @param substation any substation (cannot be null)
 * @return nothing
 */
def printSubstation(Substation substation) {
    println("Substation " + substation.getNameOrId());
    println(" - Optional name " + substation.getOptionalName());
    def levels = "";
    substation.getVoltageLevels().forEach{levels += " " + it.getNominalV()}
    println(" - Voltage level [" + levels + " ]");
    println(" - Two windings transformers " + substation.getTwoWindingsTransformers());
    println(" - Three windings transformers " + substation.getThreeWindingsTransformers());
}

/*
Script execution
 */
exec(args)