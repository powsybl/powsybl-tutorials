parameters {
computationType OPF
withRedispatchingResults true
}

for (l in network.branches){
branch(l.id){
baseCaseFlowResults true
maxThreatFlowResults true
}
}

branch('S_SO_2') {
  baseCaseFlowResults true
  maxThreatFlowResults true
  branchRatingsBaseCase 'seuilN'
  branchRatingsOnContingency 'seuilN'
  }



for (g in ['SO_G1', 'SE_G', 'SO_G2', 'N_G']){
    generator(g){
    redispatchingDownCosts 1
    redispatchingUpCosts 100 
    onContingencies 'S_SO_1'
    }
}

