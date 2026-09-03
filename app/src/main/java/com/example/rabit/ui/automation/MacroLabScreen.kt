package com.example.rabit.ui.automation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.rabit.ui.theme.*
import com.example.rabit.ui.components.AppCard
import com.example.rabit.ui.components.IconTile
import com.example.rabit.ui.components.LabelPill
import com.example.rabit.ui.components.PrimaryButton
import com.example.rabit.ui.components.ScreenScaffold
import com.example.rabit.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroLabScreen(
    viewModel: AutomationViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val recordingColor = MaterialTheme.colorScheme.error

    var isRecording by rememberSaveable { mutableStateOf(false) }
    var currentMacro by rememberSaveable { mutableStateOf("") }
    var inputText by rememberSaveable { mutableStateOf("") }
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var macroName by rememberSaveable { mutableStateOf("") }

    val savedMacros by viewModel.customMacros.collectAsState()
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    ScreenScaffold(
        title = "Macro lab",
        subtitle = "Record and compile HID sequences",
        onBack = onBack,
        actions = {
            if (currentMacro.isNotBlank()) {
                IconButton(onClick = { showSaveDialog = true }) {
                    Icon(Icons.Default.Save, "Save macro", tint = Success)
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = HackieSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(HackieSpacing.md),
        ) {
            // Status & controls card
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(HackieSpacing.sm)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
                    ) {
                        IconTile(
                            icon = if (isRecording) Icons.Default.FiberManualRecord else Icons.Default.RadioButtonUnchecked,
                            color = if (isRecording) recordingColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isRecording) "Recording" else "Idle",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isRecording) recordingColor
                                else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (isRecording) "Type or use the special keys below."
                                else "Press record to start intercepting input.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (isRecording) {
                            LabelPill(
                                text = "Live",
                                background = recordingColor.copy(alpha = 0.15f),
                                foreground = recordingColor,
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (isRecording) {
                                    isRecording = false
                                } else {
                                    isRecording = true
                                    currentMacro = ""
                                    inputText = ""
                                }
                            },
                            modifier = Modifier.weight(1f),
                            border = if (isRecording)
                                androidx.compose.foundation.BorderStroke(2.dp, recordingColor)
                            else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        ) {
                            Icon(
                                if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(HackieSpacing.xs))
                            Text(if (isRecording) "Stop" else "Record")
                        }

                        PrimaryButton(
                            text = "Test",
                            onClick = { viewModel.executeDuckyScript(currentMacro) },
                            enabled = !isRecording && currentMacro.isNotBlank(),
                            icon = Icons.Default.PlayArrow,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Intercept field
            if (isRecording) {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(HackieSpacing.sm)) {
                        SectionHeader(title = "Type here")
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
                                    currentMacro += duckyCmd
                                } else if (newValue.length < inputText.length) {
                                    currentMacro += "BACKSPACE\n"
                                }
                                inputText = newValue
                            },
                            placeholder = { Text("Type or paste to record keystrokes") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                currentMacro += "ENTER\n"
                            })
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(HackieSpacing.xs),
                        ) {
                            SpecialKeyButton("Ctrl", onClick = { currentMacro += "CTRL\n" }, modifier = Modifier.weight(1f))
                            SpecialKeyButton("Alt", onClick = { currentMacro += "ALT\n" }, modifier = Modifier.weight(1f))
                            SpecialKeyButton("Shift", onClick = { currentMacro += "SHIFT\n" }, modifier = Modifier.weight(1f))
                            SpecialKeyButton("Gui", onClick = { currentMacro += "GUI\n" }, modifier = Modifier.weight(1f))
                            SpecialKeyButton("Delay", onClick = { currentMacro += "DELAY 500\n" }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Compiler output
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(HackieSpacing.xs),
                ) {
                    SectionHeader(title = "Compiled DuckyScript", modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            currentMacro = ""
                            inputText = ""
                        },
                        enabled = currentMacro.isNotBlank(),
                    ) {
                        Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(HackieSpacing.xxs))
                        Text("Clear")
                    }
                    TextButton(
                        onClick = {
                            copyToClipboard(context, currentMacro)
                            snackbarMessage = "Copied to clipboard"
                        },
                        enabled = currentMacro.isNotBlank(),
                    ) {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(HackieSpacing.xxs))
                        Text("Copy")
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(HackieSpacing.sm),
                ) {
                    Text(
                        text = currentMacro.ifBlank { "Awaiting macro interception…" },
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = if (currentMacro.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                        else Success,
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                    )
                }
            }

            // Saved macros (so the Save dialog has a destination)
            if (savedMacros.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(HackieSpacing.xs)) {
                    SectionHeader(title = "Saved macros (${savedMacros.size})")
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(HackieSpacing.xxs),
                    ) {
                        items(savedMacros, key = { it.name }) { macro ->
                            SavedMacroRow(
                                name = macro.name,
                                command = macro.command,
                                onLoad = {
                                    currentMacro = macro.command
                                    inputText = macro.command
                                        .lineSequence()
                                        .filter { it.startsWith("STRING ") }
                                        .joinToString("") { it.removePrefix("STRING ") }
                                    snackbarMessage = "Loaded '${macro.name}'"
                                },
                                onDelete = {
                                    viewModel.deleteCustomMacro(macro)
                                    snackbarMessage = "Deleted '${macro.name}'"
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save macro") },
            text = {
                OutlinedTextField(
                    value = macroName,
                    onValueChange = { macroName = it },
                    label = { Text("Macro name") },
                    singleLine = true,
                    isError = savedMacros.any { it.name.equals(macroName, ignoreCase = true) },
                    supportingText = {
                        if (savedMacros.any { it.name.equals(macroName, ignoreCase = true) }) {
                            Text("A macro with this name already exists")
                        }
                    },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = macroName.isNotBlank() &&
                        savedMacros.none { it.name.equals(macroName, ignoreCase = true) },
                    onClick = {
                        viewModel.addCustomMacro(macroName, currentMacro)
                        showSaveDialog = false
                        macroName = ""
                        snackbarMessage = "Saved macro"
                    },
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            },
        )
    }

    snackbarMessage?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(2500)
            snackbarMessage = null
        }
    }
}

@Composable
private fun SavedMacroRow(
    name: String,
    command: String,
    onLoad: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HackieSpacing.xs),
            modifier = Modifier.padding(HackieSpacing.sm),
        ) {
            Icon(
                Icons.Default.Bookmark,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = command.lineSequence().firstOrNull() ?: "",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onLoad) {
                Icon(Icons.Default.FileOpen, "Load", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun SpecialKeyButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(vertical = 10.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText("Hackie macro", text))
}
