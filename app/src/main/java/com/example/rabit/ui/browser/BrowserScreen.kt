package com.example.rabit.ui.browser

import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    onBack: () -> Unit
) {
    val currentUrl by viewModel.currentUrl.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val canGoBack by viewModel.canGoBack.collectAsState()
    val canGoForward by viewModel.canGoForward.collectAsState()
    
    var urlInput by remember { mutableStateOf(currentUrl) }
    var webView: WebView? by remember { mutableStateOf(null) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(currentUrl) {
        if (urlInput != currentUrl) {
            urlInput = currentUrl
        }
    }

    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatSurface)
    ) {
        // ── URL Bar ──
        Surface(
            color = Surface0.copy(alpha = 0.95f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to Rabit", tint = TextPrimary)
                    }

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Graphite.copy(alpha = 0.3f),
                            unfocusedContainerColor = Graphite.copy(alpha = 0.2f),
                            focusedBorderColor = AccentBlue.copy(alpha = 0.5f),
                            unfocusedBorderColor = BorderColor.copy(alpha = 0.2f),
                            selectionColors = TextSelectionColors,
                            cursorColor = AccentBlue,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextSecondary
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            var targetUrl = urlInput.trim()
                            if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
                                targetUrl = "https://$targetUrl"
                            }
                            viewModel.updateUrl(targetUrl)
                            webView?.loadUrl(targetUrl)
                            focusManager.clearFocus()
                        }),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        trailingIcon = {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = AccentBlue
                                )
                            } else {
                                IconButton(onClick = { webView?.reload() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Browser Settings", tint = TextPrimary)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor = Surface0,
                            modifier = Modifier.border(0.5.dp, BorderColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Set as Home Page", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, tint = AccentBlue) },
                                onClick = {
                                    showMenu = false
                                    viewModel.setHomePage(currentUrl)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Go Home", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Launch, contentDescription = null, tint = AccentBlue) },
                                onClick = {
                                    showMenu = false
                                    val home = viewModel.getHomePage()
                                    viewModel.updateUrl(home)
                                    webView?.loadUrl(home)
                                }
                            )
                        }
                    }
                }

                // Progress line
                AnimatedVisibility(visible = isLoading) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = AccentBlue,
                        trackColor = Color.Transparent
                    )
                }
            }
        }

        // ── WebView ──
        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                viewModel.setLoading(true)
                                url?.let { urlInput = it }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                viewModel.setLoading(false)
                                viewModel.setNavigationState(view?.canGoBack() ?: false, view?.canGoForward() ?: false)
                                url?.let { viewModel.updateUrl(it) }
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                return false
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                viewModel.setProgress(newProgress)
                            }
                        }

                        loadUrl(currentUrl)
                        webView = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = {
                    // Update if needed
                }
            )
        }

        // ── Navigation Bar ──
        Surface(
            color = Surface0.copy(alpha = 0.95f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { webView?.goBack() },
                    enabled = canGoBack
                ) {
                    Icon(
                        Icons.Default.ArrowBackIosNew, 
                        contentDescription = "Back", 
                        tint = if (canGoBack) TextPrimary else TextSecondary.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = { webView?.goForward() },
                    enabled = canGoForward
                ) {
                    Icon(
                        Icons.Default.ArrowForwardIos, 
                        contentDescription = "Forward", 
                        tint = if (canGoForward) TextPrimary else TextSecondary.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = { 
                    val home = viewModel.getHomePage()
                    viewModel.updateUrl(home)
                    webView?.loadUrl(home)
                }) {
                    Icon(Icons.Default.Home, contentDescription = "Home", tint = TextPrimary)
                }
                IconButton(onClick = { webView?.reload() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = TextPrimary)
                }
            }
        }
    }
}
