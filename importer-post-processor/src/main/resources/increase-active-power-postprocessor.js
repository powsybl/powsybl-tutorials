var debug = true;

function increaseLoadActivePower( load, percent) {
	if (load != null) {
		var p = load.getTerminal().getP();
		load.getTerminal().setP(p * percent);
		if (debug)
			print("Load id: "+load.getId() +" Increase load active power, from " + p + " to " +  load.getTerminal().getP());
	}

}

var percent = 1.01;

if (network == null) {
    throw new NullPointerException()
}

for each (load in network.getLoads()) {
    increaseLoadActivePower(load , percent);
}