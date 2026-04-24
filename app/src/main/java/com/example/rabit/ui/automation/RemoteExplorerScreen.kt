package com.example.rabit.ui.automation

import android.content.Intent
import android.provider.DocumentsContract
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.domain.model.RemoteFile
import com.example.rabit.ui.helper.HelperViewModel
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteExplorerScreen(viewModel: HelperViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val files by viewModel.remoteFiles.collectAsState()
    val currentPath by viewModel.currentRemotePath.collectAsState()
    val isMounted by viewModel.isRemoteMounted.collectAsState()
    val mountStatus by viewModel.remoteMountStatus.collectAsState()
    val isConnecting by viewModel.isRemoteLoading.collectAsState()
    val error by viewModel.remoteError.collectAsState()
    val sshConnected by viewModel.sshConnected.collectAsState()
    val remoteSource by viewModel.remoteSource.collectAsState()
    val previewContent by viewModel.filePreviewContent.collectAsState()
    val previewName by viewModel.filePreviewName.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()

    // Dialog states
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf<RemoteFile?>(null) }
    var showRenameDialog by remember { mutableStateOf<RemoteFile?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var selectedFileForActions by remember { mutableStateOf<RemoteFile?>(null) }

    // Preview overlay
    if (previewContent != null) {
        AlertDialog(
            onDismissRequest = { viewModel.closePreview() },
            title = { Text(previewName, color = Platinum, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1) },
            text = {
                Surface(color = Obsidian, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    Text(
                        previewContent ?: "",
                        color = Platinum,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.closePreview() }) { Text("CLOSE", color = AccentBlue) }
            },
            containerColor = Surface0
        )
    }

    // New Folder Dialog
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("New Folder", color = Platinum, fontWeight = FontWeight.Black) },
            text = {
                OutlinedTextField(
                    value = newFolderName, onValueChange = { newFolderName = it },
                    placeholder = { Text("Folder name", color = Silver.copy(alpha = 0.5f)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Platinum, unfocusedTextColor = Platinum, focusedBorderColor = AccentBlue, unfocusedBorderColor = BorderColor),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.createRemoteFolder(newFolderName); showNewFolderDialog = false; newFolderName = "" }) { Text("CREATE", color = AccentBlue) }
            },
            dismissButton = { TextButton(onClick = { showNewFolderDialog = false }) { Text("CANCEL", color = Silver) } },
            containerColor = Surface0
        )
    }

    // Delete Dialog
    showDeleteDialog?.let { file ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete ${file.name}?", color = Color.Red, fontWeight = FontWeight.Black) },
            text = { Text("This action cannot be undone.", color = Platinum) },
            confirmButton = { TextButton(onClick = { viewModel.deleteRemoteFile(file); showDeleteDialog = null; selectedFileForActions = null }) { Text("DELETE", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text("CANCEL", color = Silver) } },
            containerColor = Surface0
        )
    }

    // Rename Dialog
    showRenameDialog?.let { file ->
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Rename", color = Platinum, fontWeight = FontWeight.Black) },
            text = {
                OutlinedTextField(
                    value = renameValue, onValueChange = { renameValue = it },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Platinum, unfocusedTextColor = Platinum, focusedBorderColor = AccentBlue, unfocusedBorderColor = BorderColor),
                    singleLine = true
                )
            },
            confirmButton = { TextButton(onClick = { viewModel.renameRemoteFile(file, renameValue); showRenameDialog = null; selectedFileForActions = null }) { Text("RENAME", color = AccentBlue) } },
            dismissButton = { TextButton(onClick = { showRenameDialog = null }) { Text("CANCEL", color = Silver) } },
            containerColor = Surface0
        )
    }

    // Bottom sheet for file actions
    selectedFileForActions?.let { file ->
        AlertDialog(
            onDismissRequest = { selectedFileForActions = null },
            title = { Text(file.name, color = Platinum, fontWeight = FontWeight.Black, maxLines = 1, fontSize = 14.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!file.isDirectory && isPreviewable(file.extension)) {
                        TextButton(onClick = { viewModel.previewRemoteFile(file); selectedFileForActions = null }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Visibility, null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Preview", color = Platinum, modifier = Modifier.weight(1f))
                        }
                    }
                    if (!file.isDirectory) {
                        TextButton(onClick = { viewModel.downloadRemoteFile(file, context); selectedFileForActions = null }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Download, null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Download", color = Platinum, modifier = Modifier.weight(1f))
                        }
                    }
                    TextButton(onClick = { renameValue = file.name; showRenameDialog = file; selectedFileForActions = null }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Edit, null, tint = AccentGold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Rename", color = Platinum, modifier = Modifier.weight(1f))
                    }
                    TextButton(onClick = { showDeleteDialog = file; selectedFileForActions = null }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Delete", color = Color.Red, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { selectedFileForActions = null }) { Text("CLOSE", color = Silver) } },
            containerColor = Surface0
        )
    }

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("REMOTE EXPLORER", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = Platinum))
                        Text("SOURCE: $remoteSource", color = AccentBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Platinum) } },
                actions = {
                    IconButton(onClick = { showNewFolderDialog = true }) { Icon(Icons.Default.CreateNewFolder, null, tint = AccentBlue) }
                    IconButton(onClick = { viewModel.refreshRemoteFiles() }) { Icon(Icons.Default.Refresh, null, tint = AccentTeal) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Source Toggle: SSH / ADB
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = remoteSource == "SSH",
                    onClick = { viewModel.switchRemoteSource("SSH") },
                    label = { Text("SSH", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Lan, null, modifier = Modifier.size(14.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                        selectedLabelColor = AccentBlue,
                        containerColor = Color.Transparent, labelColor = Silver
                    ), modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = remoteSource == "ADB",
                    onClick = { viewModel.switchRemoteSource("ADB") },
                    label = { Text("ADB", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Usb, null, modifier = Modifier.size(14.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SuccessGreen.copy(alpha = 0.2f),
                        selectedLabelColor = SuccessGreen,
                        containerColor = Color.Transparent, labelColor = Silver
                    ), modifier = Modifier.weight(1f)
                )
                if (remoteSource == "SSH" && sshConnected) {
                    FilterChip(
                        selected = isMounted,
                        onClick = { viewModel.toggleRemoteMount() },
                        label = { Text(if (isMounted) "Mounted" else "Mount", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        leadingIcon = { Icon(if (isMounted) Icons.Default.CloudDone else Icons.Default.CloudOff, null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SuccessGreen.copy(alpha = 0.2f), selectedLabelColor = SuccessGreen,
                            containerColor = Color.Transparent, labelColor = Silver
                        ), modifier = Modifier.weight(1f)
                    )
                }
            }

            // Download progress snackbar
            AnimatedVisibility(visible = downloadProgress != null) {
                Surface(color = if (downloadProgress?.startsWith("✓") == true) SuccessGreen.copy(alpha = 0.15f) else AccentBlue.copy(alpha = 0.15f), modifier = Modifier.fillMaxWidth()) {
                    Text(downloadProgress ?: "", modifier = Modifier.padding(12.dp), color = Platinum, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Breadcrumb bar with back button
            Surface(color = Color.Black.copy(alpha = 0.3f), modifier = Modifier.fillMaxWidth().height(40.dp)) {
                Row(modifier = Modifier.padding(horizontal = 8.dp).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.goUpRemote() }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AccentTeal, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    val parts = currentPath.split("/").filter { it.isNotEmpty() }
                    Text("root", color = AccentTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { viewModel.navigateRemote(if (remoteSource == "ADB") "/sdcard" else "/") })
                    parts.forEachIndexed { index, part ->
                        Text(" > ", color = Silver.copy(alpha = 0.3f), fontSize = 12.sp)
                        Text(part, color = Platinum, fontSize = 12.sp, modifier = Modifier.clickable {
                            viewModel.navigateRemote("/" + parts.take(index + 1).joinToString("/"))
                        })
                    }
                }
            }

            // Main content
            Box(modifier = Modifier.weight(1f)) {
                when {
                    isConnecting -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = AccentTeal)
                                Spacer(Modifier.height(12.dp))
                                Text("Loading...", color = Silver.copy(alpha = 0.5f), fontSize = 12.sp)
                            }
                        }
                    }
                    error != null -> {
                        Column(Modifier.fillMaxSize().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.ErrorOutline, null, tint = Color.Red, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(16.dp))
                            Text(error!!, color = Platinum, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Spacer(Modifier.height(24.dp))
                            Button(onClick = { viewModel.refreshRemoteFiles() }, colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)) { Text("Retry") }
                        }
                    }
                    files.isEmpty() && !isConnecting -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                                Icon(Icons.Default.FolderOff, null, tint = Silver.copy(alpha = 0.2f), modifier = Modifier.size(64.dp))
                                Spacer(Modifier.height(16.dp))
                                Text("Empty or not connected", color = Silver, fontSize = 14.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("Connect via SSH or ADB first, then refresh.", color = Silver.copy(alpha = 0.5f), fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(files) { rFile ->
                                RemoteFileItem(
                                    file = rFile,
                                    onClick = {
                                        if (rFile.isDirectory) viewModel.navigateRemote(rFile.path)
                                        else selectedFileForActions = rFile
                                    },
                                    onLongClick = { selectedFileForActions = rFile },
                                    onDownload = { viewModel.downloadRemoteFile(rFile, context) }
                                )
                            }
                        }
                    }
                }
            }

            // Status bar
            Surface(color = Color.Black.copy(alpha = 0.4f), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(
                        if ((remoteSource == "SSH" && sshConnected) || remoteSource == "ADB") SuccessGreen else Color.Red
                    ))
                    Spacer(Modifier.width(8.dp))
                    Text("${files.size} items", color = Silver, fontSize = 10.sp, modifier = Modifier.weight(1f))
                    Text(currentPath, color = Silver.copy(alpha = 0.5f), fontSize = 10.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoteFileItem(file: RemoteFile, onClick: () -> Unit, onLongClick: () -> Unit, onDownload: () -> Unit) {
    val fileIcon = when {
        file.isDirectory -> Icons.Default.Folder
        file.extension in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp") -> Icons.Default.Image
        file.extension in listOf("mp4", "avi", "mkv", "mov") -> Icons.Default.VideoFile
        file.extension in listOf("mp3", "wav", "ogg", "flac", "aac") -> Icons.Default.AudioFile
        file.extension == "pdf" -> Icons.Default.PictureAsPdf
        file.extension in listOf("zip", "gz", "tar", "rar", "7z") -> Icons.Default.FolderZip
        file.extension in listOf("txt", "md", "log", "csv", "json", "xml", "yaml", "yml") -> Icons.Default.TextSnippet
        file.extension in listOf("py", "java", "kt", "js", "ts", "sh", "swift", "go", "rs", "c", "cpp", "h", "html", "css") -> Icons.Default.Code
        file.extension in listOf("apk") -> Icons.Default.Android
        file.extension in listOf("db", "sqlite", "sqlite3") -> Icons.Default.Storage
        else -> Icons.Default.Description
    }
    val iconColor = when {
        file.isDirectory -> AccentBlue
        file.extension in listOf("jpg", "jpeg", "png", "gif", "webp") -> Color(0xFFE879F9)
        file.extension in listOf("mp4", "avi", "mkv", "mov") -> Color(0xFF60A5FA)
        file.extension in listOf("mp3", "wav", "ogg") -> AccentGold
        file.extension == "pdf" -> Color(0xFFEF4444)
        file.extension in listOf("zip", "gz", "tar") -> WarningYellow
        file.extension in listOf("py", "java", "kt", "js", "sh", "html", "css") -> AccentTeal
        file.extension == "apk" -> SuccessGreen
        else -> Platinum.copy(alpha = 0.7f)
    }

    Surface(
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(38.dp), color = iconColor.copy(alpha = 0.1f), shape = RoundedCornerShape(10.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(fileIcon, null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(file.name, color = Platinum, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!file.isDirectory) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (file.size > 0) Text(formatFileSize(file.size), color = Silver.copy(alpha = 0.5f), fontSize = 10.sp)
                        if (file.extension.isNotBlank()) Text(file.extension.uppercase(), color = Silver.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
            if (!file.isDirectory) {
                IconButton(onClick = onDownload, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Download, null, tint = SuccessGreen.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }
            }
            IconButton(onClick = onLongClick, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.MoreVert, null, tint = Silver.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

private fun isPreviewable(ext: String): Boolean {
    return ext.lowercase() in listOf(
        "txt", "md", "log", "csv", "json", "xml", "yaml", "yml", "ini", "conf", "cfg",
        "py", "java", "kt", "js", "ts", "sh", "swift", "go", "rs", "c", "cpp", "h",
        "html", "css", "sql", "rb", "pl", "php", "lua", "r", "m", "toml", "env",
        "gitignore", "dockerfile", "makefile", "gradle", "properties"
    )
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824 -> String.format("%.1f GB", bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
