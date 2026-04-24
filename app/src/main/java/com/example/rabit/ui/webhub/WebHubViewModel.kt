package com.example.rabit.ui.webhub

import android.app.Application
import android.content.Context
import android.net.Uri
import android.net.wifi.WifiManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.nio.ByteBuffer

sealed class ServerState {
    object Idle : ServerState()
    object Starting : ServerState()
    data class Running(val url: String) : ServerState()
    data class Error(val message: String) : ServerState()
}

class WebHubViewModel(application: Application) : AndroidViewModel(application) {
    private val _serverState = MutableStateFlow<ServerState>(ServerState.Idle)
    val serverState = _serverState.asStateFlow()

    private val _selectedFileUri = MutableStateFlow<Uri?>(null)
    val selectedFileUri = _selectedFileUri.asStateFlow()

    private var server: io.ktor.server.engine.ApplicationEngine? = null
    private val port = 8080

    fun selectFile(uri: Uri) {
        _selectedFileUri.value = uri
    }

    fun startServer() {
        val uri = _selectedFileUri.value
        if (uri == null) {
            _serverState.value = ServerState.Error("No file selected")
            return
        }

        viewModelScope.launch {
            _serverState.value = ServerState.Starting
            try {
                val ip = getLocalIpAddress()
                if (ip == null) {
                    _serverState.value = ServerState.Error("Could not find local IP. Are you on Wi-Fi?")
                    return@launch
                }

                val content = readFileContent(uri)
                
                server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
                    routing {
                        get("/{...}") {
                            call.respondText(content, io.ktor.http.ContentType.Text.Html)
                        }
                    }
                }.start(wait = false)

                _serverState.value = ServerState.Running("http://$ip:$port")
            } catch (e: Exception) {
                _serverState.value = ServerState.Error("Server failure: ${e.localizedMessage}")
            }
        }
    }

    fun stopServer() {
        viewModelScope.launch {
            server?.stop(1000, 2000, java.util.concurrent.TimeUnit.MILLISECONDS)
            server = null
            _serverState.value = ServerState.Idle
        }
    }

    private suspend fun readFileContent(uri: Uri): String = withContext(Dispatchers.IO) {
        getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader().use { it.readText() }
        } ?: throw Exception("Could not read file")
    }

    private fun getLocalIpAddress(): String? {
        val wifiManager = getApplication<Application>().getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipAddress = wifiManager.connectionInfo.ipAddress
        if (ipAddress == 0) return null
        return String.format(
            "%d.%d.%d.%d",
            (ipAddress and 0xff),
            (ipAddress shr 8 and 0xff),
            (ipAddress shr 16 and 0xff),
            (ipAddress shr 24 and 0xff)
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopServer()
    }
}
