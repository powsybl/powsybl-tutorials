parameters {
computationType OPF
withRedispatchingResults true
withAdequacyResults true
}


branch('S_SO_2') {
  baseCaseFlowResults true
  maxThreatFlowResults true
  branchRatingsBaseCase 'seuilN'
  branchRatingsOnContingency 'seuilN'
  }

load('SE_L1'){
curativeSheddingPercentage 100
curativeSheddingCost 10
onContingencies 'S_SO_1'
}

for (g in ['SO_G1', 'SE_G', 'SO_G2', 'N_G']){
    generator(g){
    redispatchingDownCosts 1
    redispatchingUpCosts 100 
    onContingencies 'S_SO_1'
    }
}
