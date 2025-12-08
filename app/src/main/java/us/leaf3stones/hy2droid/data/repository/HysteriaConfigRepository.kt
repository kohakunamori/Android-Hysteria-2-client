package us.leaf3stones.hy2droid.data.repository

import us.leaf3stones.hy2droid.data.datasource.HysteriaConfigDataSource
import us.leaf3stones.hy2droid.data.model.HysteriaConfig
import us.leaf3stones.hy2droid.data.model.HysteriaConfigV2

class HysteriaConfigRepository(private val singleSaveConfigDataStore: HysteriaConfigDataSource = HysteriaConfigDataSource()) {
    suspend fun saveConfig(config: HysteriaConfig) {
        singleSaveConfigDataStore.saveConfig(config)
    }
    
    suspend fun saveConfigV2(config: HysteriaConfigV2) {
        singleSaveConfigDataStore.saveConfigV2(config)
    }

    suspend fun loadConfig() =
        singleSaveConfigDataStore.loadConfig()
        
    suspend fun loadConfigV2() =
        singleSaveConfigDataStore.loadConfigV2()

}