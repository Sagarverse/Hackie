package com.example.rabit.ui.automation

import android.content.Intent
import android.provider.DocumentsContract
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.domain.model.RemoteFile
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteExplorerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val files by viewModel.remoteFiles.collectAsState()
    val currentPath by viewModel.currentRemotePath.collectAsState()
    val isMounted by viewModel.isRemoteMounted.collectAsState()
    val mountStatus by viewModel.remoteMountStatus.collectAsState()
    val isConnecting by viewModel.isRemoteLoading.collectAsState()
    val error by viewModel.remoteError.collectAsState()
    val sshConnected by viewModel.sshConnected.collectAsState()

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "REMOTE EXPLORER",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp,
                                color = Platinum
                            )
                        )
                        Text(
                            currentPath,
                            style = MaterialTheme.typography.labelSmall,
                            color = Silver.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Platinum)
                    }
                },
                actions = {
                    // Mount/Unmount button
                    IconButton(onClick = { viewModel.toggleRemoteMount() }) {
                        Icon(
                            if (isMounted) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                            null,
                            tint = if (isMounted) SuccessGreen else Silver.copy(alpha = 0.5f)
                        )
                    }
                    // Open in Files app
                    if (isMounted) {
                        IconButton(onClick = {
                            try {
                                val rootsUri = DocumentsContract.buildRootsUri(
                                    "${context.packageName}.remote.documents"
                                )
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(rootsUri, "vnd.android.document/root")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                // Fallback: open the system file manager
                                val browseIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = "*/*"
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    context.startActivity(browseIntent)
                                }
                            } catch (_: Exception) {
                                // Ignore if Files app isn't available
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = AccentBlue)
                        }
                    }
                    IconButton(onClick = { viewModel.refreshRemoteFiles() }) {
                        Icon(Icons.Default.Refresh, null, tint = AccentTeal)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Mount Status Banner
            AnimatedVisibility(visible = isMounted) {
                Surface(
                    color = SuccessGreen.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CloudDone, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "STORAGE MOUNTED",
                                color = SuccessGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                "Visible in Files app as \"Hackie Remote\"",
                                color = SuccessGreen.copy(alpha = 0.7f),
                                fontSize = 10.sp
                            )
                        }
                        TextButton(
                            onClick = {
                                try {
                                    val browseIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "*/*"
                                    }
                                    context.startActivity(browseIntent)
                                } catch (_: Exception) {}
                            }
                        ) {
                            Text("OPEN FILES", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Mount Action Card (when not mounted & not connected yet)
            AnimatedVisibility(visible = !isMounted && sshConnected) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.mountRemoteStorage() },
                    color = AccentBlue.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.FolderShared, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Mount as External Storage",
                                color = Platinum,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Access this device's files from your File Manager",
                                color = Silver.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = AccentBlue)
                    }
                }
            }

            // Mount status text
            if (mountStatus.isNotBlank() && !isMounted) {
                Surface(
                    color = WarningYellow.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        mountStatus,
                        modifier = Modifier.padding(8.dp),
                        color = WarningYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Path / Breadcrumb Bar
            Surface(
                color = Color.Black.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth().height(40.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val parts = currentPath.split("/").filter { it.isNotEmpty() }
                    Text(
                        "root",
                        color = AccentTeal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { viewModel.navigateRemote("/") }
                    )
                    parts.forEachIndexed { index, part ->
                        Text(" > ", color = Silver.copy(alpha = 0.3f), fontSize = 12.sp)
                        Text(
                            part,
                            color = Platinum,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable {
                                val path = "/" + parts.take(index + 1).joinToString("/")
                                viewModel.navigateRemote(path)
                            }
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (isConnecting) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AccentTeal)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Loading remote files...", color = Silver.copy(alpha = 0.5f), fontSize = 12.sp)
                        }
                    }
                } else if (error != null) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = Color.Red, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(error!!, color = Platinum, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                        ) {
                            Text("Go Back")
                        }
                    }
                } else if (files.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(Icons.Default.CloudOff, null, tint = Silver.copy(alpha = 0.2f), modifier = Modifier.size(72.dp))
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("No Remote Connection", color = Platinum, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                color = Graphite.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("How to Connect & Mount:", color = AccentBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("1. Use Hackie Helper (Zero Config):", color = Platinum, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("   • Open 'Hackie Helper' from the side menu", color = Silver, fontSize = 12.sp)
                                    Text("   • Enter the PIN shown on your Mac's Helper App", color = Silver, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("2. Use SSH Terminal (Advanced):", color = Platinum, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("   • Open 'SSH Terminal' from the side menu", color = Silver, fontSize = 12.sp)
                                    Text("   • Connect to your Mac using IP & Password", color = Silver, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Tip: Once connected, return here and tap the Cloud icon to mount as a local drive!", color = AccentTeal, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(files) { rFile: RemoteFile ->
                            RemoteFileItem(
                                file = rFile,
                                viewModel = viewModel,
                                onClick = {
                                    if (rFile.isFolder) viewModel.navigateRemote(rFile.path)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoteFileItem(
    file: RemoteFile,
    viewModel: MainViewModel,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val fileIcon = when {
        file.isFolder -> Icons.Default.Folder
        file.extension in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp") -> Icons.Default.Image
        file.extension in listOf("mp4", "avi", "mkv", "mov") -> Icons.Default.VideoFile
        file.extension in listOf("mp3", "wav", "ogg", "flac", "aac") -> Icons.Default.AudioFile
        file.extension in listOf("pdf") -> Icons.Default.PictureAsPdf
        file.extension in listOf("zip", "gz", "tar", "rar") -> Icons.Default.FolderZip
        file.extension in listOf("txt", "md", "log", "csv") -> Icons.Default.TextSnippet
        file.extension in listOf("py", "java", "kt", "js", "ts", "sh", "swift", "go", "rs") -> Icons.Default.Code
        else -> Icons.Default.Description
    }

    val iconColor = when {
        file.isFolder -> AccentBlue
        file.extension in listOf("jpg", "jpeg", "png", "gif", "webp") -> Color(0xFFE879F9)
        file.extension in listOf("mp4", "avi", "mkv", "mov") -> Color(0xFF60A5FA)
        file.extension in listOf("mp3", "wav", "ogg") -> AccentGold
        file.extension == "pdf" -> Color(0xFFEF4444)
        file.extension in listOf("zip", "gz", "tar") -> WarningYellow
        file.extension in listOf("py", "java", "kt", "js", "sh") -> AccentTeal
        else -> Platinum.copy(alpha = 0.7f)
    }

    Surface(
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                color = iconColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(fileIcon, null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    file.name,
                    color = Platinum,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!file.isFolder) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (file.size > 0) {
                            Text(
                                formatFileSize(file.size),
                                color = Silver.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }
                        if (file.extension.isNotBlank()) {
                            Text(
                                file.extension.uppercase(),
                                color = Silver.copy(alpha = 0.4f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
            
            if (!file.isFolder) {
                IconButton(onClick = { viewModel.downloadRemoteFile(file, context) }) {
                    Icon(Icons.Default.Download, null, tint = SuccessGreen.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            } else {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = Silver.copy(alpha = 0.2f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824 -> String.format("%.1f GB", bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
