mapToGenerators{
timeSeriesName 'SO_G1'
filter {generator.id=='SO_G1'}
}

mapToGenerators{
timeSeriesName 'SO_G2'
filter {generator.id=='SO_G2'}
}

mapToLoads{
timeSeriesName 'SE_L1'
filter {load.id=='SE_L1'}
}

timeSeries['nul']=0

mapToGenerators{
timeSeriesName 'nul'
filter {generator.id in ['N_G','SE_G'] }
}

mapToLoads{
timeSeriesName 'nul'
filter {load.id in ['SE_L2','SO_L']}
}

network.getTwoWindingsTransformer('NE_NO_1').phaseTapChanger.tapPosition=17

mapToHvdcLines{
timeSeriesName 'nul'
filter {hvdcLine.id in ['HVDC1','HVDC2'] }
}
