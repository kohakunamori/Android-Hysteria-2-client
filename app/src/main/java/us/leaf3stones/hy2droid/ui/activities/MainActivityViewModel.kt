package us.leaf3stones.hy2droid.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import us.leaf3stones.hy2droid.data.repository.HysteriaConfigRepository
import us.leaf3stones.hy2droid.data.model.HysteriaConfig
import us.leaf3stones.hy2droid.data.model.ConfigListItem
import us.leaf3stones.hy2droid.data.model.HysteriaConfigV2
import us.leaf3stones.hy2droid.proxy.Hysteria2VpnService

class MainActivityViewModel : ViewModel() {
    private val _state = MutableStateFlow(
        UiState(
            isVpnConnected = false,
            configData = HysteriaConfig(),
            configDataV2 = HysteriaConfigV2(),
            configList = listOf(
                ConfigListItem(
                    id = HysteriaConfigV2().id,
                    name = "默认配置",
                    server = ""
                )
            ),
            shouldShowConfigInvalidReminder = false,
            validationError = null
        )
    )
    val state get() = _state.asStateFlow()

    private val configRepo = HysteriaConfigRepository()
    
    // Store all configurations in memory
    private val allConfigs = mutableMapOf<String, HysteriaConfigV2>()

    init {
        viewModelScope.launch {
            // Try to load V2 config first
            val v2Config = try {
                val loaded = configRepo.loadConfigV2()
                Log.d("MainActivityViewModel", "Loaded V2 config: server=${loaded.server}, routeRules.enabled=${loaded.routeRules?.enabled}, portHopInterval=${loaded.portHopInterval}")
                loaded
            } catch (e: Exception) {
                Log.w("MainActivityViewModel", "Failed to load V2 config: ${e.message}")
                // Fallback to legacy config if V2 doesn't exist
                val legacyConfig = configRepo.loadConfig()
                if (legacyConfig.server.isNotBlank()) {
                    HysteriaConfigV2(
                        name = "导入配置",
                        server = legacyConfig.server,
                        auth = legacyConfig.password,
                        tlsSni = legacyConfig.sni
                    )
                } else {
                    HysteriaConfigV2()
                }
            }
            
            // Initialize config storage
            allConfigs[v2Config.id] = v2Config
            
            // Also load legacy config for backward compatibility
            val legacyConfig = configRepo.loadConfig()
            
            _state.update {
                it.copy(
                    configData = legacyConfig,
                    configDataV2 = v2Config,
                    configList = listOf(
                        ConfigListItem(
                            id = v2Config.id,
                            name = v2Config.name,
                            server = v2Config.server
                        )
                    )
                )
            }
        }
    }

    fun startVpnService(context: Context) {
        val vpnStatusObserver = object : Hysteria2VpnService.Companion.VpnStatusObserver {
            override fun onVpnStarted() {
                _state.update { curr ->
                    curr.copy(isVpnConnected = true)
                }
            }

            override fun onVpnStopped() {
                _state.update { curr ->
                    curr.copy(isVpnConnected = false)
                }
            }
        }
        Hysteria2VpnService.addObserver(vpnStatusObserver)

        Intent(context, Hysteria2VpnService::class.java).apply {
            setAction(Hysteria2VpnService.ACTION_START_VPN)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, this)
            } else {
                context.startService(this)
            }
        }
    }

    fun stopVpnService(context: Context) {
        Intent(context, Hysteria2VpnService::class.java).apply {
            setAction(Hysteria2VpnService.ACTION_STOP_VPN)
            context.startService(this)
        }
    }


    fun onServerChanged(string: String) {
        onConfigDataChanged(_state.value.configData.copy(server = string))
    }

    fun onPasswordChanged(password: String) {
        onConfigDataChanged(_state.value.configData.copy(password = password))
    }

    fun onSniChanged(sni: String) {
        onConfigDataChanged(_state.value.configData.copy(sni = sni))
    }

    private fun onConfigDataChanged(newConfigData: HysteriaConfig) {
        _state.update {
            it.copy(configData = newConfigData)
        }
    }

    // New V2 Config handlers
    fun onConfigV2Changed(newConfigData: HysteriaConfigV2) {
        _state.update {
            it.copy(configDataV2 = newConfigData)
        }
        // Update in-memory storage
        allConfigs[newConfigData.id] = newConfigData
    }
    
    fun onConfigSelected(configId: String) {
        allConfigs[configId]?.let { selectedConfig ->
            _state.update {
                it.copy(configDataV2 = selectedConfig)
            }
        }
    }
    
    fun onConfigDeleted(configId: String) {
        // Don't delete if it's the only config
        if (allConfigs.size <= 1) return
        
        // Don't delete current config
        if (_state.value.configDataV2.id == configId) return
        
        allConfigs.remove(configId)
        updateConfigList()
    }
    
    fun onConfigDuplicated() {
        val currentConfig = _state.value.configDataV2
        val newConfig = currentConfig.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = "${currentConfig.name} (副本)"
        )
        
        allConfigs[newConfig.id] = newConfig
        _state.update {
            it.copy(configDataV2 = newConfig)
        }
        updateConfigList()
    }
    
    private fun updateConfigList() {
        val configList = allConfigs.values.map { config ->
            ConfigListItem(
                id = config.id,
                name = config.name,
                server = config.server
            )
        }.sortedByDescending { it.lastUsed }
        
        _state.update {
            it.copy(configList = configList)
        }
    }
    
    fun updateRouteRules(routeRules: us.leaf3stones.hy2droid.data.model.RouteRulesConfig) {
        val updatedConfig = _state.value.configDataV2.copy(routeRules = routeRules)
        _state.update {
            it.copy(configDataV2 = updatedConfig)
        }
        // Update in-memory storage
        allConfigs[updatedConfig.id] = updatedConfig
        Log.d("MainActivityViewModel", "Route rules updated for config: ${updatedConfig.id}")
        
        // Auto-save config when route rules are updated
        viewModelScope.launch {
            Log.d("MainActivityViewModel", "Auto-saving config after route rules update")
            Log.d("MainActivityViewModel", "Config: server=${updatedConfig.server}, routeRules.enabled=${updatedConfig.routeRules?.enabled}, rules count=${updatedConfig.routeRules?.rules?.size}")
            configRepo.saveConfigV2(updatedConfig)
            Log.d("MainActivityViewModel", "Config auto-saved successfully")
        }
    }

    fun onConfigV2Confirmed() {
        Log.d("MainActivityViewModel", "Config V2 confirmed: ${_state.value.configDataV2}")
        
        val config = _state.value.configDataV2
        val validationResult = config.validate()
        
        if (validationResult.isValid) {
            viewModelScope.launch {
                // Update in-memory storage
                allConfigs[config.id] = config
                
                // Update config list
                updateConfigList()
                
                // Save V2 config directly
                Log.d("MainActivityViewModel", "Saving config: server=${config.server}, routeRules.enabled=${config.routeRules?.enabled}, portHopInterval=${config.portHopInterval}")
                Log.d("MainActivityViewModel", "RouteRules details: ${config.routeRules}")
                
                configRepo.saveConfigV2(config)
                
                Log.d("MainActivityViewModel", "Config V2 saved successfully with routeRules: ${config.routeRules?.enabled}")
            }
        } else {
            _state.update {
                it.copy(
                    shouldShowConfigInvalidReminder = true,
                    validationError = validationResult.errorMessage
                )
            }
        }
    }

    fun onConfigConfirmed() {
        Log.d("MainActivityViewModel", "confirmed: " + _state.value.configData)
        if (isUserConfigValid()) {
            viewModelScope.launch {
                configRepo.saveConfig(_state.value.configData)
            }
        } else {
            _state.update {
                it.copy(shouldShowConfigInvalidReminder = true)
            }
        }
    }

    fun onConfigInvalidReminderDismissed() {
        _state.update {
            it.copy(shouldShowConfigInvalidReminder = false)
        }
    }

    private fun isUserConfigValid(): Boolean {
        val config = _state.value.configData
        return config.server.isNotBlank() && config.password.isNotBlank()
    }
}

data class UiState(
    val isVpnConnected: Boolean,
    val configData: HysteriaConfig,
    val configDataV2: HysteriaConfigV2,
    val configList: List<ConfigListItem>,
    val shouldShowConfigInvalidReminder: Boolean,
    val validationError: String? = null
)
