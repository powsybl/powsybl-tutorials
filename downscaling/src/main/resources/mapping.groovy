import com.powsybl.iidm.network.EnergySource

country = network.getCountries().iterator()[0]

for (eSource in EnergySource.values()) {
    // To each generator its country's / energy source TS
    mapToGenerators {
        timeSeriesName eSource.toString() + '_' + country.toString()
        distributionKey {
            generator.maxP
        }
        filter {
            generator.energySource == eSource
        }
    }
}

// To each load its country's TS
mapToLoads {
    timeSeriesName 'LOAD_' + country.toString()
    distributionKey {
        load.p0
    }
}
