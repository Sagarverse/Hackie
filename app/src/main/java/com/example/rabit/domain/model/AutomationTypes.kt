package com.example.rabit.domain.model

data class TerminalDevice(
    val ip: String, 
    val port: Int, 
    val protocol: String,
    val banner: String? = null,
    val riskLevel: String? = "UNKNOWN",
    val aiInsight: String? = null
)
data class RemoteProcess(val pid: Int, val command: String, val cpu: Double, val mem: Double, val user: String)
data class SystemStats(val cpuLoad: Float, val memUsage: Float, val diskUsage: Float, val uptime: String)

data class CustomMacro(
    val name: String,
    val command: String,
    val category: String = "General",
    val tags: List<String> = emptyList(),
    val cooldownMs: Long = 0L,
    val onlyWhenApp: String? = null
)

enum class EmergencyAction {
    LOCK_MACHINE,
    KILL_INTERNET_ADAPTER,
    STOP_AUDIO,
    CLEAR_CLIPBOARD,
    CLOSE_SENSITIVE_APPS
}
