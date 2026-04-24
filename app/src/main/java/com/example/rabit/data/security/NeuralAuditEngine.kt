package com.example.rabit.data.security

import com.example.rabit.data.adb.RabitAdbClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Neural Vulnerability Auditor (NVA) Core.
 * Identifies security flaws and provides remediation guidance.
 */
class NeuralAuditEngine(private val adbClient: RabitAdbClient) {

    data class Finding(
        val id: String,
        val title: String,
        val severity: Severity,
        val category: Category,
        val description: String,
        val remediation: String,
        val cveReference: String? = null
    )

    enum class Severity { CRITICAL, HIGH, MEDIUM, LOW, INFO }
    enum class Category { SYSTEM, NETWORK, APP, AUTH, PRIVACY }

    /**
     * Performs a full security audit of the connected ADB device.
     */
    suspend fun performAudit(): List<Finding> = withContext(Dispatchers.IO) {
        val findings = mutableListOf<Finding>()
        
        // 1. Check System Properties
        checkSystemProps(findings)
        
        // 2. Check Package Security
        checkPackageSecurity(findings)

        // 3. Deep App Permission Audit
        checkAppPermissions(findings)
        
        // 4. Check Network Posture
        checkNetworkPosture(findings)
        
        findings.sortedBy { it.severity }
    }

    /**
     * Generates a professional tactical security report.
     */
    fun generateReport(findings: List<Finding>): String {
        val sb = StringBuilder()
        sb.append("========================================\n")
        sb.append("      HACKIE PRO - TACTICAL REPORT      \n")
        sb.append("========================================\n")
        sb.append("TIMESTAMP: ${java.util.Date()}\n")
        sb.append("TOTAL FINDINGS: ${findings.size}\n")
        sb.append("----------------------------------------\n\n")

        findings.forEach { finding ->
            sb.append("[${finding.severity}] ${finding.title}\n")
            sb.append("CATEGORY: ${finding.category}\n")
            sb.append("DESCRIPTION: ${finding.description}\n")
            sb.append("REMEDIATION: ${finding.remediation}\n")
            sb.append("----------------------------------------\n")
        }

        sb.append("\nEND OF REPORT\n")
        return sb.toString()
    }

    private suspend fun checkSystemProps(findings: MutableList<Finding>) {
        val roDebuggable = adbClient.executeCommand("getprop ro.debuggable").trim()
        if (roDebuggable == "1") {
            findings.add(Finding(
                id = "SYS-001",
                title = "Kernel Debugging Enabled",
                severity = Severity.CRITICAL,
                category = Category.SYSTEM,
                description = "The device kernel is debuggable (ro.debuggable=1). This allows memory dumping and easier root access for attackers.",
                remediation = "Recompile the kernel with ro.debuggable=0 or use a production-grade build."
            ))
        }

        val secureProp = adbClient.executeCommand("getprop ro.secure").trim()
        if (secureProp == "0") {
            findings.add(Finding(
                id = "SYS-002",
                title = "Insecure System Core",
                severity = Severity.CRITICAL,
                category = Category.SYSTEM,
                description = "The system core is running in insecure mode (ro.secure=0). ADB will always run as root.",
                remediation = "Ensure ro.secure=1 in build.prop."
            ))
        }
    }

    private suspend fun checkPackageSecurity(findings: MutableList<Finding>) {
        val backupApps = adbClient.executeCommand("pm list packages -f").trim()
        if (backupApps.contains("com.android.backupconfirm")) {
             findings.add(Finding(
                id = "APP-001",
                title = "Backup Confirmation Surface Exposed",
                severity = Severity.LOW,
                category = Category.PRIVACY,
                description = "System backup confirmation is available. If ADB is enabled, an attacker could potentially siphon app data.",
                remediation = "Disable ADB backup via 'adb shell bmgr backup' or disable ADB entirely."
            ))
        }
    }

    private suspend fun checkAppPermissions(findings: MutableList<Finding>) {
        val dangerousApps = adbClient.executeCommand("pm list packages -u").split("\n")
        dangerousApps.take(10).forEach { line ->
            if (line.contains("package:")) {
                val pkg = line.substringAfter("package:").trim()
                val perms = adbClient.executeCommand("dumpsys package $pkg").lowercase()
                
                if (perms.contains("android.permission.read_sms") && perms.contains("android.permission.internet")) {
                    findings.add(Finding(
                        id = "APP-PRIV-001",
                        title = "High-Risk App: $pkg",
                        severity = Severity.HIGH,
                        category = Category.PRIVACY,
                        description = "The application '$pkg' has permission to read SMS and access the Internet. This is a common pattern for spyware exfiltrating private messages.",
                        remediation = "Audit this app's origin. If unknown, uninstall via 'pm uninstall $pkg'."
                    ))
                }
            }
        }
    }

    private suspend fun checkNetworkPosture(findings: MutableList<Finding>) {
        val netstat = adbClient.executeCommand("netstat -tuln").trim()
        if (netstat.contains(":5555")) {
            findings.add(Finding(
                id = "NET-001",
                title = "ADB-over-WiFi Active",
                severity = Severity.HIGH,
                category = Category.NETWORK,
                description = "The device is listening for ADB connections on port 5555. This is highly vulnerable to remote exploitation on public networks.",
                remediation = "Execute 'adb tcpip -1' or disable Wireless Debugging in developer options."
            ))
        }
    }
}
