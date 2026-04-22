package com.example.rabit.ui.automation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroLabScreen(
    viewModel: AutomationViewModel,
    onBack: () -> Unit
) {
    val accentColor = Color(0xFFE11D48) // Crimson for recording
    val bgColor = Obsidian

    var isRecording by remember { mutableStateOf(false) }
    var currentMacro by remember { mutableStateOf("") }
    var inputText by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }
    var macroName by remember { mutableStateOf("") }

    Scaffold(
        containerColor = bgColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "MACRO LAB",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = Platinum
                            )
                        )
                        Text("HID RECORDER & COMPILER", color = accentColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Platinum)
                    }
                },
                actions = {
                    if (currentMacro.isNotBlank()) {
                        IconButton(onClick = { showSaveDialog = true }) {
                            Icon(Icons.Default.Save, "Save Macro", tint = SuccessGreen)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Control Panel
            Surface(
                color = Color.Black.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isRecording) accentColor else Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            if (isRecording) {
                                isRecording = false
                            } else {
                                isRecording = true
                                currentMacro = ""
                                inputText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) Color.Transparent else accentColor,
                            contentColor = if (isRecording) accentColor else Platinum
                        ),
                        border = if (isRecording) androidx.compose.foundation.BorderStroke(2.dp, accentColor) else null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(if (isRecording) Icons.Default.Stop else Icons.Default.RadioButtonChecked, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isRecording) "STOP REC" else "START REC", fontWeight = FontWeight.Black)
                    }
                    
                    Spacer(Modifier.width(16.dp))
                    
                    Button(
                        onClick = { viewModel.executeDuckyScript(currentMacro) },
                        enabled = !isRecording && currentMacro.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SuccessGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text("TEST", fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Intercept Field
            if (isRecording) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { newValue ->
                        if (newValue.length > inputText.length) {
                            val char = newValue.last()
                            val duckyCmd = when (char) {
                                '\n' -> "ENTER\n"
                                ' ' -> "SPACE\n"
                                else -> "STRING $char\n"
                            }
                            // Clean up consecutive STRING commands later, but for now append
                            currentMacro += duckyCmd
                        } else if (newValue.length < inputText.length) {
                            currentMacro += "BACKSPACE\n"
                        }
                        inputText = newValue
                    },
                    label = { Text("RECORDING SENSOR (TYPE HERE)", color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Black) },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = accentColor.copy(alpha = 0.5f),
                        focusedTextColor = Platinum,
                        unfocusedTextColor = Platinum
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        currentMacro += "ENTER\n"
                    })
                )
                
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    SpecialKeyButton("CTRL", "CTRL", onClick = { currentMacro += "CTRL\n" })
                    SpecialKeyButton("ALT", "ALT", onClick = { currentMacro += "ALT\n" })
                    SpecialKeyButton("SHIFT", "SHIFT", onClick = { currentMacro += "SHIFT\n" })
                    SpecialKeyButton("GUI", "GUI", onClick = { currentMacro += "GUI\n" })
                    SpecialKeyButton("DELAY", "DELAY 500", onClick = { currentMacro += "DELAY 500\n" })
                }
            }

            Spacer(Modifier.height(24.dp))

            // DuckyScript Compiler Output
            Surface(
                color = Color.Black,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "COMPILED DUCKYSCRIPT",
                        color = Silver.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = currentMacro.ifBlank { "Awaiting macro interception..." },
                        color = if (currentMacro.isBlank()) Silver.copy(alpha = 0.3f) else SuccessGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    )
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Macro", color = Platinum, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = macroName,
                    onValueChange = { macroName = it },
                    label = { Text("Macro Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Platinum, unfocusedTextColor = Platinum
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (macroName.isNotBlank()) {
                            viewModel.addCustomMacro(macroName, currentMacro)
                            showSaveDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel", color = Silver)
                }
            },
            containerColor = Graphite
        )
    }
}

@Composable
fun SpecialKeyButton(label: String, cmd: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Text(
            label,
            color = Platinum,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}
