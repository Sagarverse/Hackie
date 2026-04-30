package com.example.rabit.ui.automation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.annotation.SuppressLint
import com.example.rabit.ui.theme.*

data class HidProfile(val name: String, val provider: String, val description: String)

@Composable
fun IdentityLabContent(
    viewModel: AutomationViewModel
) {
    val profiles = listOf(
        HidProfile("Hackie Keyboard & Mouse", "Hackie", "Keyboard + Mouse"),
        HidProfile("Apple Magic Keyboard", "Apple Inc.", "Bluetooth Keyboard"),
        HidProfile("Logitech MX Master 3", "Logitech", "Advanced Wireless Mouse"),
        HidProfile("Generic USB Audio Device", "Generic", "Audio Control"),
        HidProfile("Keychron K2", "Keychron", "Mechanical Keyboard")
    )

    var selectedProfile by remember { mutableStateOf(profiles[0]) }
    var isApplying by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Surface(
            color = Color(0xFFFF9F0A).copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9F0A).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Warning, null, tint = Color(0xFFFF9F0A), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Spoofing the HID Descriptor will temporarily drop the current Bluetooth connection. Reconnection occurs automatically.",
                    color = Color(0xFFFF9F0A),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text("SELECT PROFILE", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(profiles) { profile ->
                ProfileCard(
                    profile = profile,
                    isSelected = selectedProfile == profile,
                    onClick = { selectedProfile = profile }
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                isApplying = true
                viewModel.updateHidIdentity(selectedProfile.name, selectedProfile.provider, selectedProfile.description)
                // Reset applying state after a delay (simulating reconnect time)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    isApplying = false
                }, 2000)
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFBC13FE),
                contentColor = Platinum
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isApplying) {
                CircularProgressIndicator(color = Platinum, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("APPLY DESCRIPTOR", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun ProfileCard(profile: HidProfile, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) Color(0xFFBC13FE).copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFFBC13FE) else Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Devices, null, tint = if (isSelected) Color(0xFFBC13FE) else Silver, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                @SuppressLint("MissingPermission")
                Text(profile.name, color = Platinum, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("${profile.provider} • ${profile.description}", color = Silver, fontSize = 11.sp)
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFFBC13FE), modifier = Modifier.size(20.dp))
            }
        }
    }
}
