package com.example.rabit.ui.snippets

import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.hackieColors
import com.example.rabit.ui.components.*
import com.example.rabit.ui.components.ScreenScaffold
import com.example.rabit.ui.theme.HackieSpacing
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.ui.graphics.Color

data class TextSnippet(val name: String, val content: String, val category: String = "General")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE) }

    var snippets by remember { mutableStateOf(loadSnippets(prefs)) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var snippetToEdit by remember { mutableStateOf<TextSnippet?>(null) }
    var expandedSnippet by rememberSaveable { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var importExportMessage by remember { mutableStateOf<String?>(null) }

    val normalizedQuery = searchQuery.trim().lowercase()
    val filteredSnippets = remember(snippets, normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            snippets
        } else {
            snippets.filter {
                it.name.lowercase().contains(normalizedQuery) ||
                    it.content.lowercase().contains(normalizedQuery) ||
                    it.category.lowercase().contains(normalizedQuery)
            }
        }
    }

    val openDrawer = com.example.rabit.ui.components.LocalOpenGlobalDrawer.current
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val scope = rememberCoroutineScope()
    val colors = hackieColors()

    ScreenScaffold(
        title = "Snippets",
        subtitle = "Saved text templates",
        onBack = onBack,
        actions = {
            IconButton(onClick = { exportSnippets(context, snippets, prefs) }) {
                Icon(Icons.Default.FileDownload, "Export snippets", tint = colors.textPrimary)
            }
            IconButton(onClick = {
                val msg = importSnippetsFromAssets(context, prefs)
                snippets = loadSnippets(prefs)
                importExportMessage = msg
            }) {
                Icon(Icons.Default.FileUpload, "Import snippets", tint = colors.textPrimary)
            }
            IconButton(onClick = {
                // Scroll to the search field and focus it.
                scope.launch { focusRequester.requestFocus() }
            }) {
                Icon(Icons.Default.Search, "Search", tint = colors.textPrimary)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
        if (snippets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.AutoMirrored.Filled.TextSnippet,
                        contentDescription = null,
                        tint = colors.textSecondary.copy(alpha = 0.2f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No snippets yet", color = colors.textSecondary.copy(alpha = 0.5f), fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Tap + to save frequently used text", color = colors.textSecondary.copy(alpha = 0.3f), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(24.dp))

                    // Suggestion chips
                    Text("SUGGESTIONS", color = colors.textSecondary.copy(alpha = 0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    val suggestions = listOf(
                        "Email Signature" to "Best regards,\n[Your Name]\n[Your Title]",
                        "Git Commit" to "git add . && git commit -m \"\" && git push",
                        "Meeting Response" to "Thank you for the invite. I'll be there.",
                    )
                    suggestions.forEach { (name, content) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .padding(vertical = 4.dp)
                                .clickable {
                                    snippets = snippets + TextSnippet(name, content)
                                    saveSnippets(prefs, snippets)
                                },
                            color = colors.accentTeal.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.accentTeal.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = colors.accentTeal, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(name, color = colors.accentTeal, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                item("snippets_stats") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.warning.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.warning.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Saved snippets", color = colors.textSecondary, fontSize = 12.sp)
                            Text(snippets.size.toString(), color = colors.warning, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
                item("snippets_search") {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        label = { Text("Search snippet name or content") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accentTeal,
                            unfocusedBorderColor = colors.outline,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        )
                    )
                }
                if (searchQuery.isNotBlank()) {
                    item("snippets_results") {
                        Text(
                            "${filteredSnippets.size} result(s) for '$searchQuery'",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                if (filteredSnippets.isEmpty()) {
                    item("snippets_empty_filtered") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.surface1.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.outline.copy(alpha = 0.4f))
                        ) {
                            Text(
                                if (searchQuery.isBlank()) "No snippets available" else "No snippets found for '$searchQuery'",
                                color = colors.textSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
                items(filteredSnippets.size) { index ->
                    val snippet = filteredSnippets[index]
                    SnippetCard(
                        snippet = snippet,
                        isExpanded = expandedSnippet == snippet.name,
                        showMoveUp = index > 0 && searchQuery.isBlank(),
                        showMoveDown = index < filteredSnippets.size - 1 && searchQuery.isBlank(),
                        onExpandToggle = {
                            expandedSnippet = if (expandedSnippet == snippet.name) null else snippet.name
                        },
                        onPush = { viewModel.sendText(snippet.content) },
                        onEdit = { snippetToEdit = snippet },
                        onDelete = {
                            snippets = snippets.filterNot { it.name == snippet.name }
                            saveSnippets(prefs, snippets)
                        },
                        onMoveUp = {
                            if (index > 0) {
                                val newList = snippets.toMutableList()
                                val actualIdx = newList.indexOfFirst { it.name == snippet.name }
                                if (actualIdx > 0) {
                                    java.util.Collections.swap(newList, actualIdx, actualIdx - 1)
                                    snippets = newList
                                    saveSnippets(prefs, snippets)
                                }
                            }
                        },
                        onMoveDown = {
                            if (index < snippets.size - 1) {
                                val newList = snippets.toMutableList()
                                val actualIdx = newList.indexOfFirst { it.name == snippet.name }
                                if (actualIdx < newList.size - 1) {
                                    java.util.Collections.swap(newList, actualIdx, actualIdx + 1)
                                    snippets = newList
                                    saveSnippets(prefs, snippets)
                                }
                            }
                        }
                    )
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = colors.warning,
            contentColor = colors.canvas,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Snippet")
        }
        }
    }

    if (showAddDialog || snippetToEdit != null) {
        val isEdit = snippetToEdit != null
        var name by remember { mutableStateOf(snippetToEdit?.name ?: "") }
        var content by remember { mutableStateOf(snippetToEdit?.content ?: "") }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                snippetToEdit = null
            },
            containerColor = colors.surface1,
            title = { Text(if (isEdit) "Edit Snippet" else "New Snippet", color = colors.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Snippet Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.warning,
                            unfocusedBorderColor = colors.outline,
                            focusedTextColor = colors.textPrimary
                        )
                    )
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Content") },
                        minLines = 3,
                        maxLines = 8,
                        placeholder = { Text("Paste or type your snippet text…") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.warning,
                            unfocusedBorderColor = colors.outline,
                            focusedTextColor = colors.textPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && content.isNotBlank()) {
                            if (isEdit) {
                                snippets = snippets.map { if (it.name == snippetToEdit?.name) TextSnippet(name, content) else it }
                                if (expandedSnippet == snippetToEdit?.name && name != snippetToEdit?.name) {
                                    expandedSnippet = name
                                }
                            } else {
                                snippets = snippets + TextSnippet(name, content)
                            }
                            saveSnippets(prefs, snippets)
                            showAddDialog = false
                            snippetToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.warning)
                ) { Text("Save", color = colors.canvas, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    snippetToEdit = null
                }) { Text("Cancel", color = colors.textSecondary) }
            }
        )
    }

    importExportMessage?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(2500)
            importExportMessage = null
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HackieSpacing.lg),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    msg,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(HackieSpacing.md),
                )
            }
        }
    }
}

@Composable
fun SnippetCard(
    snippet: TextSnippet,
    isExpanded: Boolean,
    showMoveUp: Boolean,
    showMoveDown: Boolean,
    onExpandToggle: () -> Unit,
    onPush: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val colors = hackieColors()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { onExpandToggle() },
        color = colors.surface1,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        Icons.AutoMirrored.Filled.TextSnippet,
                        contentDescription = null,
                        tint = colors.warning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(snippet.name, color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        if (!isExpanded) {
                            Text(
                                snippet.content.take(60).replace("\n", " "),
                                color = colors.textSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = colors.textSecondary.copy(alpha = 0.5f)
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = colors.canvas,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        snippet.content,
                        color = colors.textPrimary.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        if (showMoveUp) {
                            IconButton(onClick = onMoveUp) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = colors.textSecondary)
                            }
                        }
                        if (showMoveDown) {
                            IconButton(onClick = onMoveDown) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = colors.textSecondary)
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onDelete) {
                            Text("Delete", color = colors.error, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(onClick = onEdit) {
                            Text("Edit", color = colors.warning, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onPush,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accentTeal),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Push to Mac", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun loadSnippets(prefs: android.content.SharedPreferences): List<TextSnippet> {
    val json = prefs.getString("text_snippets_json", null) ?: return emptyList()
    return try {
        val array = JSONArray(json)
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            TextSnippet(obj.getString("name"), obj.getString("content"), obj.optString("category", "General"))
        }
    } catch (e: Exception) { emptyList() }
}

private fun saveSnippets(prefs: android.content.SharedPreferences, snippets: List<TextSnippet>) {
    val array = JSONArray()
    snippets.forEach { s ->
        array.put(JSONObject().apply {
            put("name", s.name)
            put("content", s.content)
            put("category", s.category)
        })
    }
    prefs.edit().putString("text_snippets_json", array.toString()).apply()
}

/**
 * Export snippets to a JSON file in app cache. Returns a status message.
 * Without SAF integration this writes to the cache dir; the user can pull
 * it via `adb pull` or by sharing the file from a file manager.
 */
private fun exportSnippets(
    context: Context,
    snippets: List<TextSnippet>,
    prefs: android.content.SharedPreferences,
): String {
    return try {
        val array = JSONArray()
        snippets.forEach { s ->
            array.put(JSONObject().apply {
                put("name", s.name)
                put("content", s.content)
                put("category", s.category)
            })
        }
        val file = java.io.File(context.cacheDir, "hackie_snippets.json")
        file.writeText(array.toString(2))
        "Exported ${snippets.size} snippets to ${file.absolutePath}"
    } catch (e: Exception) {
        "Export failed: ${e.message}"
    }
}

/**
 * Best-effort import. Reads from cache if it exists, else from assets/samples.
 * Returns a status message describing what was imported.
 */
private fun importSnippetsFromAssets(
    context: Context,
    prefs: android.content.SharedPreferences,
): String {
    val existing = loadSnippets(prefs)
    val candidates = mutableListOf<TextSnippet>()
    val cacheFile = java.io.File(context.cacheDir, "hackie_snippets.json")
    val source = if (cacheFile.exists()) {
        cacheFile.readText()
    } else {
        try {
            context.assets.open("samples/snippets.json").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            return "No snippet file found in cache or assets"
        }
    }
    return try {
        val arr = JSONArray(source)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            candidates += TextSnippet(
                name = o.getString("name"),
                content = o.getString("content"),
                category = o.optString("category", "General"),
            )
        }
        val merged = (existing + candidates).distinctBy { it.name }
        saveSnippets(prefs, merged)
        "Imported ${candidates.size} snippet(s); total ${merged.size}"
    } catch (e: Exception) {
        "Import failed: ${e.message}"
    }
}
