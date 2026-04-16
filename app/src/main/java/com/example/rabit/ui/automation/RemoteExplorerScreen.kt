package com.example.rabit.ui.automation

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
import androidx.compose.ui.graphics.Color
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
    val files by viewModel.remoteFiles.collectAsState()
    val currentPath by viewModel.currentRemotePath.collectAsState()
    val isMounted by viewModel.isRemoteMounted.collectAsState()
    val isConnecting by viewModel.isRemoteLoading.collectAsState()
    val error by viewModel.remoteError.collectAsState()

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
                    IconButton(onClick = { viewModel.toggleRemoteMount() }) {
                        Icon(
                            if (isMounted) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                            null,
                            tint = if (isMounted) SuccessGreen else Silver.copy(alpha = 0.5f)
                        )
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
            // Mounting Status Banner
            AnimatedVisibility(visible = isMounted) {
                Surface(
                    color = SuccessGreen.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Info, null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "SYSTEM MOUNT ACTIVE: Visible in Files app",
                            color = SuccessGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
                        CircularProgressIndicator(color = AccentTeal)
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
                        Text("No files found", color = Silver.copy(alpha = 0.5f))
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
                color = if (file.isFolder) AccentBlue.copy(alpha = 0.1f) else Platinum.copy(alpha = 0.05f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (file.isFolder) Icons.Default.Folder else Icons.Default.Description,
                        null,
                        tint = if (file.isFolder) AccentBlue else Platinum.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
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
                    Text(
                        "${file.size / 1024} KB",
                        color = Silver.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
            }
            
            if (!file.isFolder) {
                val context = androidx.compose.ui.platform.LocalContext.current
                IconButton(onClick = { viewModel.downloadRemoteFile(file, context) }) {
                    Icon(Icons.Default.Download, null, tint = SuccessGreen.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            } else {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = Silver.copy(alpha = 0.2f), modifier = Modifier.size(16.dp))
            }
        }
    }
}
