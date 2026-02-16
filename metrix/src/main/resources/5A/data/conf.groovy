parameters {
computationType OPF
withRedispatchingResults true
}


branch("S_SO_2") {
baseCaseFlowResults true
maxThreatFlowResults true
branchRatingsBaseCase 'seuilN'
branchRatingsOnContingency 'seuilN'
}


for (g in ['SO_G1', 'SE_G', 'SO_G2', 'N_G']){
    generator(g){
    redispatchingDownCosts 1
    redispatchingUpCosts 100 
    }
}


