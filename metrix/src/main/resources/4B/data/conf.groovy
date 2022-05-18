

parameters {
computationType OPF_WITHOUT_REDISPATCHING
}

branch('S_SO_2') {
	baseCaseFlowResults true
	maxThreatFlowResults true
	branchRatingsBaseCase 'seuilN'
	branchRatingsOnContingency 'seuilN'
}

phaseShifter('NE_NO_1'){
controlType OPTIMIZED_ANGLE_CONTROL
onContingencies 'S_SO_1'
}
