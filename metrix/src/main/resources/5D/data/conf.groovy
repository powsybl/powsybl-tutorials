parameters {computationType OPF}


branch('S_SO_2') {
  baseCaseFlowResults true
  maxThreatFlowResults true
  branchRatingsBaseCase 'seuilN'
  branchRatingsOnContingency 'seuilN'
  }


for (g in ['SO_G1', 'SE_G', 'SO_G2']){
    generator(g){
    redispatchingDownCosts 1
    redispatchingUpCosts 100 
    onContingencies 'S_SO_1'
    }
}

load('SO_L'){
preventiveSheddingPercentage 100
}
