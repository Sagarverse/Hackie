package com.example.rabit.ui.automation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.components.PremiumGlassCard
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroBuilderContent(
    viewModel: MacroBuilderViewModel,
    mainViewModel: MainViewModel
) {
    val actions by viewModel.actions.collectAsState()
    val isExecuting by viewModel.isExecuting.collectAsState()
    val connectionState by mainViewModel.connectionState.collectAsState()
    
    var showAddMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        
        Text(
            text = "MACRO BUILDER",
            color = Platinum,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(16.dp)
        )
        
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(actions) { action ->
                MacroActionCard(
                    action = action,
                    onUpdate = { viewModel.updateAction(it) },
                    onDelete = { viewModel.removeAction(action.id) }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showAddMenu = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Graphite),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Action", tint = AccentBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Action", color = Platinum)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        // Execute Button Area
        Surface(
            color = Obsidian,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val isConnected = connectionState is HidDeviceManager.ConnectionState.Connected
                if (!isConnected) {
                    Text(
                        text = "Device not connected. Connect via USB/Bluetooth to execute.",
                        color = AccentOrange,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                Button(
                    onClick = { viewModel.executeSequence(mainViewModel) },
                    enabled = isConnected && actions.isNotEmpty() && !isExecuting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentTeal,
                        disabledContainerColor = Graphite
                    )
                ) {
                    if (isExecuting) {
                        CircularProgressIndicator(color = Obsidian, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Executing...", color = Obsidian, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Execute", tint = Obsidian)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Execute Sequence", color = Obsidian, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showAddMenu) {
        ModalBottomSheet(
            onDismissRequest = { showAddMenu = false },
            containerColor = Graphite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Select Action", color = Platinum, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                
                ActionTypeItem("Unlock Device", Icons.Default.LockOpen) { 
                    viewModel.addAction(MacroAction.Unlock())
                    showAddMenu = false 
                }
                ActionTypeItem("Launch App", Icons.Default.Apps) { 
                    viewModel.addAction(MacroAction.LaunchApp())
                    showAddMenu = false 
                }
                ActionTypeItem("Terminal Command", Icons.Default.Terminal) { 
                    viewModel.addAction(MacroAction.TerminalCommand())
                    showAddMenu = false 
                }
                ActionTypeItem("Delay (Wait)", Icons.Default.Timer) { 
                    viewModel.addAction(MacroAction.Delay())
                    showAddMenu = false 
                }
                ActionTypeItem("Raw Keystrokes", Icons.Default.Keyboard) { 
                    viewModel.addAction(MacroAction.RawKeystroke())
                    showAddMenu = false 
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ActionTypeItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Obsidian,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = AccentBlue)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, color = Platinum)
        }
    }
}

@Composable
fun MacroActionCard(action: MacroAction, onUpdate: (MacroAction) -> Unit, onDelete: () -> Unit) {
    PremiumGlassCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val icon = when (action) {
                    is MacroAction.Unlock -> Icons.Default.LockOpen
                    is MacroAction.LaunchApp -> Icons.Default.Apps
                    is MacroAction.TerminalCommand -> Icons.Default.Terminal
                    is MacroAction.Delay -> Icons.Default.Timer
                    is MacroAction.RawKeystroke -> Icons.Default.Keyboard
                }
                val title = when (action) {
                    is MacroAction.Unlock -> "Unlock Device"
                    is MacroAction.LaunchApp -> "Launch App"
                    is MacroAction.TerminalCommand -> "Terminal Command"
                    is MacroAction.Delay -> "Delay"
                    is MacroAction.RawKeystroke -> "Raw Keystrokes"
                }
                Icon(icon, contentDescription = null, tint = AccentBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, color = Platinum, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = AccentOrange)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            when (action) {
                is MacroAction.Unlock -> {
                    Text("Wake up and prepare device.", color = Silver, fontSize = 12.sp)
                }
                is MacroAction.LaunchApp -> {
                    OutlinedTextField(
                        value = action.appName,
                        onValueChange = { onUpdate(action.copy(appName = it)) },
                        placeholder = { Text("App name (e.g. Terminal)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Platinum,
                            unfocusedTextColor = Platinum
                        )
                    )
                }
                is MacroAction.TerminalCommand -> {
                    OutlinedTextField(
                        value = action.command,
                        onValueChange = { onUpdate(action.copy(command = it)) },
                        placeholder = { Text("Command (e.g. curl ...)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Platinum,
                            unfocusedTextColor = Platinum
                        )
                    )
                }
                is MacroAction.Delay -> {
                    var msText by remember { mutableStateOf(action.milliseconds.toString()) }
                    OutlinedTextField(
                        value = msText,
                        onValueChange = { 
                            msText = it
                            val ms = it.toLongOrNull() ?: 0L
                            onUpdate(action.copy(milliseconds = ms)) 
                        },
                        placeholder = { Text("Milliseconds") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Platinum,
                            unfocusedTextColor = Platinum
                        )
                    )
                }
                is MacroAction.RawKeystroke -> {
                    OutlinedTextField(
                        value = action.text,
                        onValueChange = { onUpdate(action.copy(text = it)) },
                        placeholder = { Text("Type string...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Platinum,
                            unfocusedTextColor = Platinum
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = action.submit,
                            onCheckedChange = { onUpdate(action.copy(submit = it)) }
                        )
                        Text("Press Enter after typing", color = Silver, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
