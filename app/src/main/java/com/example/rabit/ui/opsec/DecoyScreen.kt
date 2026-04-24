package com.example.rabit.ui.opsec

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecoyScreen(onDeactivate: () -> Unit) {
    // A completely benign, light-themed Note-taking UI.
    var noteTitle by remember { mutableStateOf("") }
    var noteBody by remember { mutableStateOf("") }

    val lightBg = Color(0xFFF9F9F9)
    val lightText = Color(0xFF333333)

    Scaffold(
        containerColor = lightBg,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "My Notes", 
                        color = lightText, 
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    // The Secret Knock: If title is "1337" and user long-presses the "My Notes" header
                                    if (noteTitle.trim() == "1337" || noteBody.trim() == "1337") {
                                        onDeactivate()
                                    }
                                }
                            )
                        }
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Menu, "Menu", tint = lightText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                modifier = Modifier.background(Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Normal behavior: Clear note or save it (locally benign)
                    noteTitle = ""
                    noteBody = ""
                },
                containerColor = Color(0xFF2196F3),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, "New Note")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            TextField(
                value = noteTitle,
                onValueChange = { noteTitle = it },
                placeholder = { Text("Title", fontSize = 22.sp, color = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = lightText,
                    unfocusedTextColor = lightText
                ),
                textStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth()
            )
            
            Divider(color = Color(0xFFEEEEEE))
            
            TextField(
                value = noteBody,
                onValueChange = { noteBody = it },
                placeholder = { Text("Start typing your note here...", color = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = lightText,
                    unfocusedTextColor = lightText
                ),
                textStyle = TextStyle(fontSize = 16.sp),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
