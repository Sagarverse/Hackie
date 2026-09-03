package com.example.rabit.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rabit.ui.components.AppCard
import com.example.rabit.ui.components.IconTile
import com.example.rabit.ui.components.ScreenScaffold
import com.example.rabit.ui.components.SettingsToggleRow
import com.example.rabit.ui.theme.HackieSpacing

private data class SearchCommand(
    val id: String,
    val title: String,
    val route: String,
    val keywords: List<String>,
)

private data class SearchFeature(
    val route: String,
    val title: String,
    val keywords: List<String>,
)

@Composable
fun GlobalSearchScreen(
    currentRoute: String,
    availableRoutes: Set<String>,
    availableActionIds: Set<String>,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onExecuteAction: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var quickRunEnabled by remember { mutableStateOf(true) }

    val commands = remember {
        listOf(
            SearchCommand("action_unlock_mac", "Unlock Mac", "automation", listOf("unlock", "mac", "login", "password")),
            SearchCommand("action_lock_screen", "Lock Screen", "automation", listOf("lock", "screen", "security")),
            SearchCommand("action_media_play_pause", "Play / Pause", "home", listOf("media", "music", "play", "pause")),
            SearchCommand("action_media_vol_up", "Volume Up", "home", listOf("media", "volume", "up", "sound")),
            SearchCommand("action_media_vol_down", "Volume Down", "home", listOf("media", "volume", "down", "sound")),
            SearchCommand("action_now_playing", "Refresh Now Playing", "home", listOf("now", "playing", "song", "metadata")),
            SearchCommand("action_wol_send", "Wake Mac", "wake_on_lan", listOf("wake", "wol", "mac", "power")),
            SearchCommand("action_disconnect_keyboard", "Disconnect Keyboard", "keyboard", listOf("disconnect", "keyboard", "hid")),
            SearchCommand("action_web_bridge_toggle", "Toggle Web Bridge", "web_bridge", listOf("web", "bridge", "portal", "toggle")),
        )
    }

    val features = remember {
        listOf(
            SearchFeature("keyboard", "Control Hub", listOf("keyboard", "trackpad", "mouse")),
            SearchFeature("automation", "Automation", listOf("automation", "macro", "shortcut")),
            SearchFeature("injector", "Payload Injector", listOf("injector", "payload", "ducky")),
            SearchFeature("password_manager", "Password Manager", listOf("password", "vault", "biometric")),
            SearchFeature("wake_on_lan", "Wake-on-LAN", listOf("wake", "wol", "magic")),
            SearchFeature("ssh_terminal", "SSH Terminal", listOf("ssh", "terminal", "shell")),
            SearchFeature("web_bridge", "Web Bridge", listOf("web", "bridge", "file", "transfer")),
            SearchFeature("assistant", "AI Assistant", listOf("assistant", "ai", "chat")),
            SearchFeature("snippets", "Snippets", listOf("snippet", "text", "template")),
            SearchFeature("settings", "Settings", listOf("settings", "config")),
            SearchFeature("customization", "Customization", listOf("customization", "theme")),
        )
    }

    val q = query.trim().lowercase()
    val matchedCommands = remember(q, commands, availableActionIds) {
        commands.filter { command ->
            availableActionIds.contains(command.id) &&
                (q.isBlank() || command.title.lowercase().contains(q) || command.keywords.any { it.contains(q) })
        }
    }
    val matchedFeatures = remember(q, features, matchedCommands, availableRoutes) {
        val commandRoutes = matchedCommands.map { it.route }.toSet()
        features.filter { feature ->
            availableRoutes.contains(feature.route) &&
                !commandRoutes.contains(feature.route) &&
                (q.isBlank() || feature.title.lowercase().contains(q) || feature.keywords.any { it.contains(q) })
        }
    }

    ScreenScaffold(
        title = "Search",
        subtitle = "Find a feature or run a command.",
        onBack = onBack,
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = HackieSpacing.md),
            verticalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Search") },
                shape = MaterialTheme.shapes.large,
            )

            AppCard {
                SettingsToggleRow(
                    title = "Quick run",
                    subtitle = if (quickRunEnabled) "Tap a command to execute instantly."
                    else "Tap a result to open its feature page.",
                    checked = quickRunEnabled,
                    onCheckedChange = { quickRunEnabled = it },
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = HackieSpacing.xs, bottom = HackieSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(HackieSpacing.xs),
            ) {
                if (matchedCommands.isNotEmpty()) {
                    item("commands_header") {
                        Text(
                            text = "Commands",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = HackieSpacing.xs),
                        )
                    }
                    items(matchedCommands, key = { it.id }) { command ->
                        AppCard(
                            onClick = { if (quickRunEnabled) onExecuteAction(command.id) else onNavigate(command.route) },
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = HackieSpacing.xs),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
                            ) {
                                IconTile(
                                    icon = Icons.Default.Bolt,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = command.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "Action",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (availableRoutes.contains(command.route)) {
                                    TextButton(onClick = { onNavigate(command.route) }) {
                                        Text("Open")
                                    }
                                }
                            }
                        }
                    }
                }

                if (matchedFeatures.isNotEmpty()) {
                    item("features_header") {
                        Text(
                            text = "Features",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = HackieSpacing.xs),
                        )
                    }
                    items(matchedFeatures, key = { it.route }) { feature ->
                        AppCard(onClick = { onNavigate(feature.route) }) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
                            ) {
                                Spacer(Modifier.size(40.dp))
                                Text(
                                    text = feature.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                if (matchedCommands.isEmpty() && matchedFeatures.isEmpty()) {
                    item("empty") {
                        AppCard {
                            Text(
                                text = if (q.isBlank()) "Start typing to search."
                                else "No results for \"$query\".",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = HackieSpacing.sm),
                            )
                        }
                    }
                }
            }
        }
    }
}
