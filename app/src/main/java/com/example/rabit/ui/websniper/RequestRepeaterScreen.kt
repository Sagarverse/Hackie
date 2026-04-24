package com.example.rabit.ui.websniper

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestRepeaterScreen(
    viewModel: WebSniperViewModel,
    onBack: () -> Unit
) {
    var method by remember { mutableStateOf("GET") }
    var url by remember { mutableStateOf("http://192.168.1.100/api/v1/user") }
    var headersText by remember { mutableStateOf("Authorization: Bearer <token>\nUser-Agent: Mozilla/5.0") }
    var bodyText by remember { mutableStateOf("") }
    
    val isRepeating by viewModel.isRepeating.collectAsState()
    val responseHeaders by viewModel.repeaterHeaders.collectAsState()
    val responseBody by viewModel.repeaterResponse.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Request, 1: Response

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "REQUEST REPEATER",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = Platinum
                            )
                        )
                        Text("MOBILE PROXY & CRAFTER", color = Color(0xFFBC13FE), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Platinum)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (!isRepeating) {
                                viewModel.sendRepeaterRequest(method, url, headersText, bodyText)
                                selectedTab = 1 // Auto-switch to response
                            }
                        }
                    ) {
                        if (isRepeating) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFFBC13FE))
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, "Send Request", tint = Color(0xFFBC13FE))
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color(0xFFBC13FE),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFFBC13FE)
                    )
                }
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("REQUEST") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("RESPONSE") })
            }

            Spacer(Modifier.height(16.dp))

            if (selectedTab == 0) {
                // Request Pane
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = method,
                            onValueChange = { method = it },
                            label = { Text("Method") },
                            modifier = Modifier.weight(0.3f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFFBC13FE),
                                unfocusedTextColor = Color(0xFFBC13FE)
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            label = { Text("URL") },
                            modifier = Modifier.weight(0.7f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Platinum,
                                unfocusedTextColor = Platinum
                            )
                        )
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = headersText,
                        onValueChange = { headersText = it },
                        label = { Text("Headers (Key: Value)") },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Silver,
                            unfocusedTextColor = Silver
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = bodyText,
                        onValueChange = { bodyText = it },
                        label = { Text("Request Body") },
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SuccessGreen,
                            unfocusedTextColor = SuccessGreen
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    )
                }
            } else {
                // Response Pane
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                        if (responseHeaders.isNotEmpty()) {
                            Text(
                                text = responseHeaders,
                                color = Color(0xFFBC13FE),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(color = BorderColor)
                            Spacer(Modifier.height(16.dp))
                        }
                        
                        Text(
                            text = responseBody,
                            color = Platinum,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
