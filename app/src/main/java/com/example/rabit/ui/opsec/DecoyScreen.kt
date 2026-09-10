package com.example.rabit.ui.opsec

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sagar.rabit.R

data class DummyNote(val title: String, val body: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecoyScreen(onDeactivate: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val lightBg = Color(0xFFFFFFFF)
    val textColor = Color(0xFF202124)
    val hintColor = Color(0xFF5F6368)
    val surfaceColor = Color(0xFFF1F3F4)

    val dummyNotes = listOf(
        DummyNote("Groceries", "Milk\nEggs\nBread\nCheese"),
        DummyNote("Ideas", "App for tracking water intake\nSmart mirror UI"),
        DummyNote("", "Call mom at 5 PM"),
        DummyNote("Workout", "Push: Chest/Triceps\nPull: Back/Biceps\nLegs"),
        DummyNote("Books to read", "- 1984\n- Brave New World\n- Dune")
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = lightBg,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { },
                containerColor = lightBg,
                elevation = FloatingActionButtonDefaults.elevation(6.dp),
                modifier = Modifier.size(56.dp)
            ) {
                // A generic plus icon to mimic Google Keep
                Icon(Icons.Default.Add, contentDescription = "New Note", tint = Color.Black)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Search Bar (Google Notes style)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                if (searchQuery.trim() == "6202") {
                                    onDeactivate()
                                }
                            }
                        )
                    },
                shape = RoundedCornerShape(26.dp),
                color = surfaceColor,
                contentColor = textColor,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = hintColor)
                    }
                    
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search your notes", color = hintColor, fontSize = 16.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        singleLine = true
                    )
                    
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.ViewAgenda, contentDescription = "View", tint = hintColor)
                    }
                    
                    Spacer(Modifier.width(8.dp))
                    
                    Image(
                        painter = painterResource(id = R.drawable.sagar_profile),
                        contentDescription = "Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color.LightGray, CircleShape)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Notes Grid
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                verticalItemSpacing = 8.dp,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(dummyNotes) { note ->
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        colors = CardDefaults.outlinedCardColors(containerColor = lightBg)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (note.title.isNotEmpty()) {
                                Text(
                                    text = note.title,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp,
                                    color = textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Text(
                                text = note.body,
                                fontSize = 14.sp,
                                color = textColor,
                                maxLines = 8,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
