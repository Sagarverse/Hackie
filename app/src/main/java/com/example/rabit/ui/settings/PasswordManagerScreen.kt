package com.example.rabit.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.data.secure.BiometricAuthenticator
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.components.PremiumGlassCard
import com.example.rabit.ui.components.PremiumSectionHeader
import com.example.rabit.ui.theme.AccentBlue
import com.example.rabit.ui.theme.AccentGold
import com.example.rabit.ui.theme.AccentOrange
import com.example.rabit.ui.theme.AccentTeal
import com.example.rabit.ui.theme.BorderColor
import com.example.rabit.ui.theme.Graphite
import com.example.rabit.ui.theme.Obsidian
import com.example.rabit.ui.theme.Platinum
import com.example.rabit.ui.theme.Silver
import com.example.rabit.ui.theme.SuccessGreen
import kotlinx.coroutines.delay

@Composable
fun PasswordManagerScreen(
    settingsViewModel: com.example.rabit.ui.settings.SettingsViewModel, viewModel: com.example.rabit.ui.MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricAuthenticator = remember(activity) { activity?.let { BiometricAuthenticator(it) } }

    val connectionState by viewModel.connectionState.collectAsState()
    val biometricMacAutofillEnabled by settingsViewModel.biometricMacAutofillEnabled.collectAsState()
    val macAutofillPreEnter by settingsViewModel.macAutofillPreEnter.collectAsState()
    val macAutofillPostEnter by settingsViewModel.macAutofillPostEnter.collectAsState()
    val macPassword by settingsViewModel.macPassword.collectAsState()
    val vaultEntries by settingsViewModel.passwordVaultEntries.collectAsState()

    var showPasswordDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showVaultEntryDialog by remember { mutableStateOf(false) }
    var biometricSessionExpiryMs by rememberSaveable { mutableStateOf(0L) }
    var biometricNowMs by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }

    val biometricSessionDurationMs = 45_000L
    val isBiometricSessionActive = biometricMacAutofillEnabled && biometricNowMs < biometricSessionExpiryMs
    val remainingBiometricSeconds = ((biometricSessionExpiryMs - biometricNowMs).coerceAtLeast(0L) + 999L) / 1000L

    LaunchedEffect(biometricMacAutofillEnabled, biometricSessionExpiryMs) {
        biometricNowMs = System.currentTimeMillis()
        if (!biometricMacAutofillEnabled || biometricSessionExpiryMs <= 0L) return@LaunchedEffect

        while (biometricNowMs < biometricSessionExpiryMs) {
            delay(1000)
            biometricNowMs = System.currentTimeMillis()
        }
    }

    Scaffold(containerColor = Obsidian) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PremiumSectionHeader("PASSWORD VAULT")
            PremiumGlassCard {
                Text(
                    text = "Mac unlock secret is stored in encrypted storage on this phone.",
                    color = Silver,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = BorderColor.copy(alpha = 0.4f)
                )

                SettingsToggleItem(
                    title = "Biometric Session Cache",
                    subtitle = "Keep unlock active for 45 seconds after success",
                    icon = Icons.Default.Fingerprint,
                    iconColor = SuccessGreen,
                    checked = biometricMacAutofillEnabled,
                    onCheckedChange = {
                        settingsViewModel.setBiometricMacAutofillEnabled(it)
                        if (!it) {
                            biometricSessionExpiryMs = 0L
                            biometricNowMs = System.currentTimeMillis()
                        }
                    }
                )

                Text(
                    text = if (biometricMacAutofillEnabled) {
                        if (isBiometricSessionActive) {
                            "Biometric session: Active (${remainingBiometricSeconds}s left)"
                        } else {
                            "Biometric session: Locked"
                        }
                    } else {
                        "Session cache disabled. Biometric required for every push."
                    },
                    color = Silver,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = BorderColor.copy(alpha = 0.4f)
                )

                SettingsToggleItem(
                    title = "Press Enter Before Typing",
                    subtitle = "Wake login field before password injection",
                    icon = Icons.Default.Keyboard,
                    iconColor = AccentBlue,
                    checked = macAutofillPreEnter,
                    onCheckedChange = { settingsViewModel.setMacAutofillPreEnter(it) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = BorderColor.copy(alpha = 0.4f)
                )

                SettingsToggleItem(
                    title = "Press Enter After Typing",
                    subtitle = "Auto-submit after password injection",
                    icon = Icons.Default.Key,
                    iconColor = AccentTeal,
                    checked = macAutofillPostEnter,
                    onCheckedChange = { settingsViewModel.setMacAutofillPostEnter(it) }
                )
            }

            PremiumSectionHeader("VAULT ACTIONS")
            PremiumGlassCard {
                Text(
                    text = if (macPassword.isBlank()) "No password stored" else "Password stored securely",
                    color = if (macPassword.isBlank()) AccentOrange else SuccessGreen,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )

                Button(
                    onClick = { showPasswordDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Icon(Icons.Default.Password, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (macPassword.isBlank()) "Set Mac Password" else "Update Mac Password")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val pushAfterBiometric = {
                            val error = viewModel.sendMacPassword(macPassword, macAutofillPreEnter, macAutofillPostEnter)
                            Toast.makeText(context, error ?: "Password sent securely.", Toast.LENGTH_SHORT).show()
                        }

                        if (isBiometricSessionActive) {
                            pushAfterBiometric()
                        } else if (biometricAuthenticator?.isBiometricAvailable() == true) {
                            biometricAuthenticator.authenticate(
                                title = "Hackie Password Manager",
                                subtitle = "Authenticate to push password",
                                onSuccess = {
                                    biometricSessionExpiryMs = if (biometricMacAutofillEnabled) {
                                        System.currentTimeMillis() + biometricSessionDurationMs
                                    } else {
                                        0L
                                    }
                                    pushAfterBiometric()
                                },
                                onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                            )
                        } else {
                            Toast.makeText(context, "Biometric auth is unavailable on this device.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = connectionState is HidDeviceManager.ConnectionState.Connected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null)
                    Text("Biometric Push to Mac")
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = AccentGold)
                    Text("Clear Stored Password", color = AccentGold)
                }
            }

            PremiumSectionHeader("APP PASSWORD VAULT")
            PremiumGlassCard {
                Text(
                    text = "Store passwords by app/site name. Tap Push after biometric check to send the selected password to your Mac.",
                    color = Silver,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )

                Button(
                    onClick = { showVaultEntryDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Vault Entry")
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (vaultEntries.isEmpty()) {
                    Text(
                        text = "No app passwords saved yet.",
                        color = Silver.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                } else {
                    vaultEntries.forEachIndexed { index, entry ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                thickness = 0.5.dp,
                                color = BorderColor.copy(alpha = 0.4f)
                            )
                        }
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            Text(entry.appName, color = Platinum, fontWeight = FontWeight.Bold)
                            if (entry.username.isNotBlank()) {
                                Text("User: ${entry.username}", color = Silver, fontSize = 12.sp)
                            }
                            if (entry.notes.isNotBlank()) {
                                Text(entry.notes, color = Silver.copy(alpha = 0.8f), fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        val pushAction = {
                                            val error = viewModel.sendMacPassword(entry.password, macAutofillPreEnter, macAutofillPostEnter)
                                            Toast.makeText(context, error ?: "Password pushed for ${entry.appName}", Toast.LENGTH_SHORT).show()
                                        }

                                        if (isBiometricSessionActive) {
                                            pushAction()
                                        } else if (biometricAuthenticator?.isBiometricAvailable() == true) {
                                            biometricAuthenticator.authenticate(
                                                title = "Unlock ${entry.appName}",
                                                subtitle = "Authenticate to push password",
                                                onSuccess = {
                                                    biometricSessionExpiryMs = if (biometricMacAutofillEnabled) {
                                                        System.currentTimeMillis() + biometricSessionDurationMs
                                                    } else {
                                                        0L
                                                    }
                                                    pushAction()
                                                },
                                                onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                                            )
                                        } else {
                                            Toast.makeText(context, "Biometric auth is unavailable on this device.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = connectionState is HidDeviceManager.ConnectionState.Connected,
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                                ) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Biometric Push", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { settingsViewModel.removeVaultEntry(entry.id) },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentOrange)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Delete", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            PremiumSectionHeader("STATUS")
            PremiumGlassCard {
                Text(
                    text = if (connectionState is HidDeviceManager.ConnectionState.Connected) {
                        "Connected to host. Password autofill is available."
                    } else {
                        "Host disconnected. Connect to use password push."
                    },
                    color = Silver,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showPasswordDialog) {
        var tempPass by remember { mutableStateOf(macPassword) }
        var hidden by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            containerColor = Graphite,
            title = { Text("Set Mac Unlock Password", color = Platinum) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tempPass,
                        onValueChange = { tempPass = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (hidden) PasswordVisualTransformation() else VisualTransformation.None,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            TextButton(onClick = { hidden = !hidden }) {
                                Text(if (hidden) "Show" else "Hide")
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Platinum,
                            unfocusedTextColor = Platinum
                        )
                    )
                    Text("Stored locally using encrypted app storage.", color = Silver)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        settingsViewModel.setMacPassword(tempPass)
                        showPasswordDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text("Cancel", color = Silver)
                }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = Graphite,
            title = { Text("Clear Stored Password?", color = Platinum) },
            text = { Text("This removes the saved Mac password from local encrypted storage.", color = Silver) },
            confirmButton = {
                Button(
                    onClick = {
                        settingsViewModel.setMacPassword("")
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = Silver)
                }
            }
        )
    }

    if (showVaultEntryDialog) {
        var appName by remember { mutableStateOf("") }
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var hidden by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showVaultEntryDialog = false },
            containerColor = Graphite,
            title = { Text("Add Vault Entry", color = Platinum) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = appName,
                        onValueChange = { appName = it },
                        label = { Text("App / Site Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Platinum,
                            unfocusedTextColor = Platinum
                        )
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Platinum,
                            unfocusedTextColor = Platinum
                        )
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (hidden) PasswordVisualTransformation() else VisualTransformation.None,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            TextButton(onClick = { hidden = !hidden }) {
                                Text(if (hidden) "Show" else "Hide")
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Platinum,
                            unfocusedTextColor = Platinum
                        )
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Platinum,
                            unfocusedTextColor = Platinum
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        settingsViewModel.addVaultEntry(appName, username, password, notes)
                        showVaultEntryDialog = false
                    },
                    enabled = appName.isNotBlank() && password.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVaultEntryDialog = false }) {
                    Text("Cancel", color = Silver)
                }
            }
        )
    }
}
