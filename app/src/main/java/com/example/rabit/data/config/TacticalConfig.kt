package com.example.rabit.data.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object TacticalConfig {
    private val _isTunnelActive = MutableStateFlow(false)
    val isTunnelActive = _isTunnelActive.asStateFlow()

    private val _globalC2Address = MutableStateFlow("")
    val globalC2Address = _globalC2Address.asStateFlow()

    fun setTunnelState(active: Boolean, address: String) {
        _isTunnelActive.value = active
        _globalC2Address.value = address
    }
}
