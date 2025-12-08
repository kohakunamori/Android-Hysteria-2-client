package us.leaf3stones.hy2droid.data.datasource

import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.leaf3stones.hy2droid.App
import us.leaf3stones.hy2droid.data.KEY_IS_VPN_CONFIG_READY
import us.leaf3stones.hy2droid.data.KEY_VPN_CONFIG_PATH
import us.leaf3stones.hy2droid.data.MASTER_HYSTERIA_CONFIG_FILE_NAME
import us.leaf3stones.hy2droid.data.model.HysteriaConfig
import us.leaf3stones.hy2droid.data.model.HysteriaConfigV2
import us.leaf3stones.hy2droid.data.vpnPrefDataStore
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.nio.charset.StandardCharsets

class HysteriaConfigDataSource() {
    suspend fun saveConfig(config: HysteriaConfig) = withContext(Dispatchers.IO) {
        val context = App.appContext!!
        val vpnConfigFile = File(context.filesDir, MASTER_HYSTERIA_CONFIG_FILE_NAME)
        FileOutputStream(vpnConfigFile).use {
            it.write(config.getFullConfig().toByteArray(StandardCharsets.UTF_8))
        }

        val cachedFile = File(vpnConfigFile.absolutePath + "_cached.binary")
        ObjectOutputStream(FileOutputStream(cachedFile)).use {
            it.writeObject(config)
        }

        context.vpnPrefDataStore.edit {
            it[KEY_IS_VPN_CONFIG_READY] = true
            it[KEY_VPN_CONFIG_PATH] = vpnConfigFile.absolutePath
        }
    }
    
    suspend fun saveConfigV2(config: HysteriaConfigV2) = withContext(Dispatchers.IO) {
        val context = App.appContext!!
        val vpnConfigFile = File(context.filesDir, MASTER_HYSTERIA_CONFIG_FILE_NAME)
        
        android.util.Log.d("HysteriaConfigDataSource", "Saving ConfigV2: server=${config.server}, portHopInterval=${config.portHopInterval}, routeRules.enabled=${config.routeRules?.enabled}")
        
        // Generate full YAML config including ACL
        val fullConfig = config.generateConfig()
        android.util.Log.d("HysteriaConfigDataSource", "Generated config length: ${fullConfig.length}")
        
        FileOutputStream(vpnConfigFile).use {
            it.write(fullConfig.toByteArray(StandardCharsets.UTF_8))
        }

        val cachedFile = File(vpnConfigFile.absolutePath + "_cached_v2.binary")
        ObjectOutputStream(FileOutputStream(cachedFile)).use {
            it.writeObject(config)
        }

        context.vpnPrefDataStore.edit {
            it[KEY_IS_VPN_CONFIG_READY] = true
            it[KEY_VPN_CONFIG_PATH] = vpnConfigFile.absolutePath
        }
    }

    suspend fun loadConfig() : HysteriaConfig = withContext(Dispatchers.IO) {
        val context = App.appContext!!
        val vpnConfigFile = File(context.filesDir, MASTER_HYSTERIA_CONFIG_FILE_NAME)
        val cachedFile = File(vpnConfigFile.absolutePath + "_cached.binary")

        try {
            ObjectInputStream(FileInputStream(cachedFile)).use {
                val config = it.readObject() as? HysteriaConfig
                return@withContext config ?: HysteriaConfig() // Return a new instance if null
            }
        } catch (ignored: Exception) {
            return@withContext HysteriaConfig()
        }
    }
    
    suspend fun loadConfigV2() : HysteriaConfigV2 = withContext(Dispatchers.IO) {
        val context = App.appContext!!
        val vpnConfigFile = File(context.filesDir, MASTER_HYSTERIA_CONFIG_FILE_NAME)
        val cachedFile = File(vpnConfigFile.absolutePath + "_cached_v2.binary")

        try {
            ObjectInputStream(FileInputStream(cachedFile)).use {
                val config = it.readObject() as? HysteriaConfigV2
                if (config != null) {
                    android.util.Log.d("HysteriaConfigDataSource", "Loaded ConfigV2: server=${config.server}, portHopInterval=${config.portHopInterval}, routeRules.enabled=${config.routeRules?.enabled}")
                    return@withContext config
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("HysteriaConfigDataSource", "Failed to load V2 config: ${e.message}")
        }
        
        // If V2 config doesn't exist, return default
        return@withContext HysteriaConfigV2()
    }
}