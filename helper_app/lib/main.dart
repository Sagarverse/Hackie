import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:device_info_plus/device_info_plus.dart';
import 'package:clipboard/clipboard.dart';
import 'package:file_selector/file_selector.dart';
import 'package:tray_manager/tray_manager.dart';
import 'package:desktop_drop/desktop_drop.dart';
import 'package:window_manager/window_manager.dart';
import 'package:get_mac_address/get_mac_address.dart';
import 'package:webview_flutter/webview_flutter.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final bool isMobile = Platform.isAndroid || Platform.isIOS;
  final bool isDesktop =
      Platform.isMacOS || Platform.isWindows || Platform.isLinux;

  if (isMobile) {
    await initializeService();
  } else if (isDesktop) {
    await windowManager.ensureInitialized();
    const WindowOptions windowOptions = WindowOptions(
      size: Size(400, 600),
      center: true,
      skipTaskbar: true,
      titleBarStyle: TitleBarStyle.hidden,
    );
    await windowManager.waitUntilReadyToShow(windowOptions, () async {
      await windowManager.setAlwaysOnTop(true);
      await windowManager.hide(); // Start hidden, accessible via tray
    });

    // Start background sync loop for desktop
    startDesktopSync();
  }

  runApp(const GoogleServiceApp());
}

Future<void> initializeService() async {
  // ... [Existing mobile initialization]
}

String lastClipboardContent = '';
HttpServer? _directHelperServer;
String _helperDeviceName = 'Desktop';
String _helperIpAddress = 'Unknown IP';
String _helperMacAddress = 'Unknown MAC';
RawDatagramSocket? _beaconSocket;

class _HelperRuntimeStatus {
  final bool serverOnline;
  final DateTime? lastClientSeenAt;
  final String lastClientIp;

  const _HelperRuntimeStatus({
    required this.serverOnline,
    required this.lastClientSeenAt,
    required this.lastClientIp,
  });
}

class _BrowserTab {
  final int id;
  final WebViewController controller;
  String url;
  bool isLoading = true;
  String? loadError;

  _BrowserTab({required this.id, required this.controller, required this.url});
}

class _ZoomInBrowserIntent extends Intent {
  const _ZoomInBrowserIntent();
}

class _ZoomOutBrowserIntent extends Intent {
  const _ZoomOutBrowserIntent();
}

enum _SearchMode { web, ai, images, news }

final ValueNotifier<_HelperRuntimeStatus> _runtimeStatus = ValueNotifier(
  const _HelperRuntimeStatus(
    serverOnline: false,
    lastClientSeenAt: null,
    lastClientIp: 'Unknown',
  ),
);
final ValueNotifier<String?> _pendingBrowserHandoffUrl = ValueNotifier(null);

void _markClientSeen(HttpRequest request) {
  final clientIp = request.connectionInfo?.remoteAddress.address ?? 'Unknown';
  _runtimeStatus.value = _HelperRuntimeStatus(
    serverOnline: _directHelperServer != null,
    lastClientSeenAt: DateTime.now(),
    lastClientIp: clientIp,
  );
}

Future<void> _startLanBeacon() async {
  if (_beaconSocket != null) return;
  try {
    _beaconSocket = await RawDatagramSocket.bind(InternetAddress.anyIPv4, 0);
    _beaconSocket!.broadcastEnabled = true;
    Timer.periodic(const Duration(seconds: 2), (_) {
      final payload = jsonEncode({
        'type': 'hackie_helper_beacon',
        'name': _helperDeviceName,
        'ipAddress': _helperIpAddress,
        'macAddress': _helperMacAddress,
        'port': 8765,
      });
      _beaconSocket?.send(
        utf8.encode(payload),
        InternetAddress('255.255.255.255'),
        8766,
      );
    });
  } catch (e) {
    debugPrint('Failed to start LAN beacon: $e');
  }
}

Future<Map<String, dynamic>> _runCommand(Map data) async {
  final String? command = data['command'] as String?;
  if (command == null) return {'success': false, 'error': 'Missing command'};

  if (command == 'open_url') {
    final String url = (data['url'] as String?) ?? '';
    if (url.isNotEmpty) {
      _pendingBrowserHandoffUrl.value = url;
    }
    return {'success': true};
  }

  if (command == 'lock_screen') {
    if (Platform.isMacOS) {
      await Process.run('pmset', ['displaysleepnow']);
    } else if (Platform.isWindows) {
      await Process.run('rundll32.exe', ['user32.dll,LockWorkStation']);
    } else if (Platform.isLinux) {
      await Process.run('xdg-screensaver', ['lock']);
    }
    return {'success': true};
  }

  if (command == 'run_shell') {
    final String shellCmd = (data['cmd'] as String?) ?? '';
    if (shellCmd.isNotEmpty) {
      final ProcessResult result = Platform.isWindows
          ? await Process.run('cmd.exe', ['/c', shellCmd])
          : await Process.run('sh', ['-c', shellCmd]);
      return {
        'success': true,
        'stdout': result.stdout.toString(),
        'stderr': result.stderr.toString(),
      };
    }
  }

  return {'success': false, 'error': 'Unsupported command'};
}

Future<void> _startDirectHelperServer() async {
  if (_directHelperServer != null) return;
  try {
    _directHelperServer = await HttpServer.bind(InternetAddress.anyIPv4, 8765);
  } on SocketException catch (e) {
    debugPrint('Helper server already bound or unavailable: $e');
    _runtimeStatus.value = _HelperRuntimeStatus(
      serverOnline: false,
      lastClientSeenAt: _runtimeStatus.value.lastClientSeenAt,
      lastClientIp: _runtimeStatus.value.lastClientIp,
    );
    return;
  }

  _runtimeStatus.value = _HelperRuntimeStatus(
    serverOnline: true,
    lastClientSeenAt: _runtimeStatus.value.lastClientSeenAt,
    lastClientIp: _runtimeStatus.value.lastClientIp,
  );
  _directHelperServer!.listen((HttpRequest request) async {
    try {
      _markClientSeen(request);
      final path = request.uri.path;

      if (request.method == 'GET' && path == '/health') {
        request.response.headers.contentType = ContentType.json;
        request.response.write(
          jsonEncode({
            'status': 'online',
            'name': _helperDeviceName,
            'platform': Platform.operatingSystem,
          }),
        );
        await request.response.close();
        return;
      }

      if (request.method == 'GET' && path == '/info') {
        request.response.headers.contentType = ContentType.json;
        request.response.write(
          jsonEncode({
            'name': _helperDeviceName,
            'platform': Platform.operatingSystem,
            'ipAddress': _helperIpAddress,
            'macAddress': _helperMacAddress,
            'status': 'online',
          }),
        );
        await request.response.close();
        return;
      }

      if (request.method == 'GET' && path == '/clipboard') {
        final currentClipboard = await FlutterClipboard.paste();
        request.response.headers.contentType = ContentType.json;
        request.response.write(jsonEncode({'content': currentClipboard}));
        await request.response.close();
        return;
      }

      if (request.method == 'POST' && path == '/clipboard') {
        final payload =
            jsonDecode(await utf8.decoder.bind(request).join()) as Map;
        final content = (payload['content'] as String?) ?? '';
        if (content.isNotEmpty) {
          await FlutterClipboard.copy(content);
          lastClipboardContent = content;
        }
        request.response.headers.contentType = ContentType.json;
        request.response.write(jsonEncode({'success': true}));
        await request.response.close();
        return;
      }

      if (request.method == 'GET' && path == '/files') {
        final targetPath =
            request.uri.queryParameters['path'] ??
            (Platform.isWindows ? 'C:\\' : '/');
        final dir = Directory(targetPath);
        final entities = dir.listSync();
        final items = entities
            .map(
              (e) => {
                'name': e.path.split(Platform.pathSeparator).last,
                'isDir': e is Directory,
                'path': e.path,
              },
            )
            .toList();
        request.response.headers.contentType = ContentType.json;
        request.response.write(
          jsonEncode({'path': targetPath, 'items': items}),
        );
        await request.response.close();
        return;
      }

      if (request.method == 'POST' && path == '/upload') {
        final name =
            request.uri.queryParameters['name'] ??
            'hackie_file_${DateTime.now().millisecondsSinceEpoch}';
        final downloads = Platform.environment['HOME'] != null
            ? Directory('${Platform.environment['HOME']}/Downloads')
            : Directory.systemTemp;
        if (!downloads.existsSync()) {
          downloads.createSync(recursive: true);
        }
        final file = File('${downloads.path}${Platform.pathSeparator}$name');
        final sink = file.openWrite();
        await request.cast<List<int>>().pipe(sink);
        await sink.flush();
        await sink.close();

        request.response.headers.contentType = ContentType.json;
        request.response.write(
          jsonEncode({'success': true, 'savedPath': file.path}),
        );
        await request.response.close();
        return;
      }

      if (request.method == 'POST' && path == '/command') {
        final payload =
            jsonDecode(await utf8.decoder.bind(request).join()) as Map;
        final response = await _runCommand(payload);
        request.response.headers.contentType = ContentType.json;
        request.response.write(jsonEncode(response));
        await request.response.close();
        return;
      }

      request.response.statusCode = HttpStatus.notFound;
      await request.response.close();
    } catch (e) {
      request.response.statusCode = HttpStatus.internalServerError;
      request.response.headers.contentType = ContentType.json;
      request.response.write(
        jsonEncode({'success': false, 'error': e.toString()}),
      );
      await request.response.close();
    }
  });
}

Future<void> startDesktopSync() async {
  final deviceInfo = DeviceInfoPlugin();
  String deviceName = "Desktop";

  if (Platform.isMacOS) {
    MacOsDeviceInfo macInfo = await deviceInfo.macOsInfo;
    deviceName = macInfo.computerName;
  } else if (Platform.isWindows) {
    WindowsDeviceInfo winInfo = await deviceInfo.windowsInfo;
    deviceName = winInfo.computerName;
  }

  String ipAddress = "Unknown IP";
  String macAddress = "Unknown MAC";

  try {
    macAddress = await GetMacAddress().getMacAddress() ?? "Unknown MAC";
  } catch (e) {
    debugPrint("Failed to get MAC: $e");
  }

  try {
    for (var interface in await NetworkInterface.list()) {
      for (var addr in interface.addresses) {
        if (addr.type == InternetAddressType.IPv4 && !addr.isLoopback) {
          ipAddress = addr.address;
          break;
        }
      }
      if (ipAddress != "Unknown IP") break;
    }
  } catch (e) {
    debugPrint("Failed to get IP: $e");
  }

  _helperDeviceName = deviceName;
  _helperIpAddress = ipAddress;
  _helperMacAddress = macAddress;
  await _startDirectHelperServer();
  await _startLanBeacon();
}

class GoogleServiceApp extends StatefulWidget {
  const GoogleServiceApp({super.key});

  @override
  State<GoogleServiceApp> createState() => _GoogleServiceAppState();
}

class _GoogleServiceAppState extends State<GoogleServiceApp>
    with TrayListener, WindowListener {
  static const String _defaultBrowserUrl = 'https://www.google.com/?igu=1';

  int _tabIdCounter = 0;

  final TextEditingController _addressController = TextEditingController(
    text: _defaultBrowserUrl,
  );
  final TextEditingController _phoneEndpointController =
      TextEditingController();

  bool isConnected = false;
  bool _showHelperDashboard = false;
  bool _showBrowserToolbar = false;
  bool _isAlwaysOnTop = true;
  bool _isDraggingFiles = false;
  bool _isPinchingBrowser = false;
  double _browserZoom = 1.0;
  double _pinchStartZoom = 1.0;
  _SearchMode _searchMode = _SearchMode.ai;
  String _dropStatus = 'Drop files or images here to upload';
  String _phoneUploadEndpoint = '';
  String _phoneShareStatus =
      'Drop files in the Helper dashboard to send them to phone Downloads';
  String deviceName = "Unknown Device";
  String lastClientIp = "Unknown";
  Timer? _statusRefreshTimer;
  final List<_BrowserTab> _tabs = [];
  int _activeTabIndex = 0;

  _BrowserTab get _activeTab => _tabs[_activeTabIndex];

  String _searchModeLabel(_SearchMode mode) {
    switch (mode) {
      case _SearchMode.web:
        return 'Web';
      case _SearchMode.ai:
        return 'AI';
      case _SearchMode.images:
        return 'Images';
      case _SearchMode.news:
        return 'News';
    }
  }

  void _setSearchMode(_SearchMode mode) {
    setState(() {
      _searchMode = mode;
    });
  }

  Future<void> _setAlwaysOnTop(bool value) async {
    _isAlwaysOnTop = value;
    await windowManager.setAlwaysOnTop(value);
    if (mounted) setState(() {});
  }

  String _buildSearchUrl(String query) {
    final encoded = Uri.encodeComponent(query);
    switch (_searchMode) {
      case _SearchMode.web:
        return 'https://www.google.com/search?q=$encoded';
      case _SearchMode.ai:
        return 'https://www.google.com/search?udm=50&q=$encoded';
      case _SearchMode.images:
        return 'https://www.google.com/search?tbm=isch&q=$encoded';
      case _SearchMode.news:
        return 'https://www.google.com/search?tbm=nws&q=$encoded';
    }
  }

  String _sanitizeFileName(String path) {
    final fileName = path.split(Platform.pathSeparator).last;
    return fileName.isEmpty
        ? 'dropped_${DateTime.now().millisecondsSinceEpoch}'
        : fileName;
  }

  Future<File> _uniqueFileInDirectory(
    Directory directory,
    String fileName,
  ) async {
    final nameParts = fileName.split('.');
    final baseName = nameParts.length > 1
        ? nameParts.sublist(0, nameParts.length - 1).join('.')
        : fileName;
    final extension = nameParts.length > 1 ? '.${nameParts.last}' : '';
    var candidate = File('${directory.path}${Platform.pathSeparator}$fileName');
    var index = 1;
    while (await candidate.exists()) {
      candidate = File(
        '${directory.path}${Platform.pathSeparator}$baseName ($index)$extension',
      );
      index += 1;
    }
    return candidate;
  }

  Future<void> _saveDroppedFiles(List<String> paths) async {
    if (paths.isEmpty) return;
    final downloads = Platform.environment['HOME'] != null
        ? Directory('${Platform.environment['HOME']}/Downloads')
        : Directory.systemTemp;
    if (!downloads.existsSync()) {
      downloads.createSync(recursive: true);
    }

    final savedNames = <String>[];
    for (final path in paths) {
      final source = File(path);
      if (!await source.exists()) continue;
      final fileName = _sanitizeFileName(path);
      final target = await _uniqueFileInDirectory(downloads, fileName);
      await source.copy(target.path);
      savedNames.add(target.path.split(Platform.pathSeparator).last);
    }

    if (!mounted) return;
    setState(() {
      _dropStatus = savedNames.isEmpty
          ? 'No valid files were dropped'
          : 'Saved ${savedNames.length} file(s): ${savedNames.join(', ')}';
    });
  }

  _BrowserTab _createTab(String initialUrl) {
    final tabId = _tabIdCounter++;
    late final _BrowserTab tab;

    final controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setNavigationDelegate(
        NavigationDelegate(
          onPageStarted: (url) {
            if (!mounted) return;
            setState(() {
              tab.url = url;
              tab.isLoading = true;
              tab.loadError = null;
              if (_tabs[_activeTabIndex].id == tab.id) {
                _addressController.text = url;
              }
            });
          },
          onPageFinished: (url) {
            if (!mounted) return;
            setState(() {
              tab.url = url;
              tab.isLoading = false;
              if (_tabs[_activeTabIndex].id == tab.id) {
                _addressController.text = url;
              }
            });
            _applyBrowserZoom(tab);
          },
          onWebResourceError: (error) {
            if (!mounted) return;
            setState(() {
              tab.isLoading = false;
              tab.loadError = error.description;
            });
          },
        ),
      );

    tab = _BrowserTab(id: tabId, controller: controller, url: initialUrl);
    controller.loadRequest(Uri.parse(initialUrl));
    return tab;
  }

  void _openNewTab([String? url]) {
    final newTab = _createTab(url ?? _defaultBrowserUrl);
    setState(() {
      _tabs.add(newTab);
      _activeTabIndex = _tabs.length - 1;
      _addressController.text = newTab.url;
    });
  }

  double _clampZoom(double value) {
    return value.clamp(0.7, 1.8).toDouble();
  }

  Future<void> _applyBrowserZoom(_BrowserTab tab) async {
    try {
      final zoomPercent = (_browserZoom * 100).round();
      await tab.controller.runJavaScript(
        "document.body && (document.body.style.zoom = '$zoomPercent%');"
        "document.documentElement && (document.documentElement.style.zoom = '$zoomPercent%');",
      );
    } catch (_) {}
  }

  void _setBrowserZoom(double nextZoom) {
    setState(() {
      _browserZoom = _clampZoom(nextZoom);
    });
    _applyBrowserZoom(_activeTab);
  }

  void _beginBrowserPinch() {
    _pinchStartZoom = _browserZoom;
    _isPinchingBrowser = true;
  }

  void _updateBrowserPinch(double scale) {
    if (!_isPinchingBrowser) return;
    _setBrowserZoom(_pinchStartZoom * scale);
  }

  void _endBrowserPinch() {
    _isPinchingBrowser = false;
  }

  void _activateTab(int index) {
    if (index < 0 || index >= _tabs.length) return;
    setState(() {
      _activeTabIndex = index;
      _addressController.text = _tabs[index].url;
    });
    _applyBrowserZoom(_tabs[index]);
  }

  void _closeTab(int index) {
    if (_tabs.length <= 1) {
      _tabs[0].controller.loadRequest(Uri.parse(_defaultBrowserUrl));
      _activateTab(0);
      return;
    }

    setState(() {
      _tabs.removeAt(index);
      if (_activeTabIndex >= _tabs.length) {
        _activeTabIndex = _tabs.length - 1;
      } else if (_activeTabIndex > index) {
        _activeTabIndex -= 1;
      }
      _addressController.text = _activeTab.url;
    });
  }

  @override
  void initState() {
    super.initState();

    _tabs.add(_createTab(_defaultBrowserUrl));
    _addressController.text = _tabs.first.url;

    if (Platform.isMacOS || Platform.isWindows || Platform.isLinux) {
      trayManager.addListener(this);
      windowManager.addListener(this);
      _initTray();
      unawaited(windowManager.setAlwaysOnTop(_isAlwaysOnTop));
    }
    _runtimeStatus.addListener(_syncStatusFromRuntime);
    _pendingBrowserHandoffUrl.addListener(_consumePendingBrowserHandoff);
    _statusRefreshTimer = Timer.periodic(const Duration(seconds: 1), (_) {
      _syncStatusFromRuntime();
    });
    _phoneEndpointController.text = _phoneUploadEndpoint;
    _checkConnection();
  }

  Future<void> _initTray() async {
    await trayManager.setIcon(
      Platform.isWindows ? 'web/icons/Icon-192.png' : 'web/icons/Icon-192.png',
    );
    await trayManager.setToolTip('Google Helper (Hackie Client)');
    await trayManager.setContextMenu(
      Menu(
        items: [
          MenuItem(key: 'open_dashboard', label: 'Show Dashboard'),
          MenuItem.separator(),
          MenuItem(key: 'exit_app', label: 'Quit'),
        ],
      ),
    );
  }

  @override
  void onTrayIconMouseDown() {
    trayManager.popUpContextMenu();
  }

  @override
  void onTrayIconRightMouseDown() {
    trayManager.popUpContextMenu();
  }

  @override
  void onTrayMenuItemClick(MenuItem menuItem) {
    if (menuItem.key == 'open_dashboard') {
      if (mounted) {
        setState(() {
          _showHelperDashboard = false;
          _addressController.text = _activeTab.url;
        });
      }
      unawaited(windowManager.setAlwaysOnTop(_isAlwaysOnTop));
      windowManager.show();
      windowManager.focus();
    } else if (menuItem.key == 'exit_app') {
      exit(0);
    }
  }

  @override
  void dispose() {
    _addressController.dispose();
    _phoneEndpointController.dispose();
    _statusRefreshTimer?.cancel();
    _runtimeStatus.removeListener(_syncStatusFromRuntime);
    _pendingBrowserHandoffUrl.removeListener(_consumePendingBrowserHandoff);
    trayManager.removeListener(this);
    windowManager.removeListener(this);
    super.dispose();
  }

  String _normalizePhoneUploadEndpoint(String raw) {
    var trimmed = raw.trim();
    while (trimmed.endsWith('/')) {
      trimmed = trimmed.substring(0, trimmed.length - 1);
    }
    if (trimmed.isEmpty) {
      return lastClientIp.isNotEmpty && lastClientIp != 'Unknown'
          ? 'http://$lastClientIp:8080'
          : '';
    }
    final withScheme =
        trimmed.startsWith('http://') || trimmed.startsWith('https://')
        ? trimmed
        : 'http://$trimmed';
    final parsed = Uri.tryParse(withScheme);
    if (parsed == null || parsed.host.isEmpty) return '';
    final scheme = parsed.scheme.isEmpty ? 'http' : parsed.scheme;
    final port = parsed.hasPort ? parsed.port : 8080;
    return '$scheme://${parsed.host}:$port';
  }

  Future<void> _sendSingleFileToPhone(String path) async {
    final endpoint = _normalizePhoneUploadEndpoint(_phoneUploadEndpoint);
    if (endpoint.isEmpty) {
      throw Exception(
        'Phone endpoint is unknown. Connect phone to helper first.',
      );
    }

    final file = File(path);
    if (!await file.exists()) {
      throw Exception('File not found: $path');
    }

    final fileName = _sanitizeFileName(path);
    final boundary = 'hackie_${DateTime.now().microsecondsSinceEpoch}';
    final bytes = await file.readAsBytes();
    final uploadPaths = ['/helper/upload', '/upload'];
    Object? lastError;

    for (final uploadPath in uploadPaths) {
      try {
        final request = await HttpClient().postUrl(
          Uri.parse('$endpoint$uploadPath'),
        );
        request.headers.set(
          HttpHeaders.contentTypeHeader,
          'multipart/form-data; boundary=$boundary',
        );

        final header =
            '--$boundary\r\n'
            'Content-Disposition: form-data; name="file"; filename="$fileName"\r\n'
            'Content-Type: application/octet-stream\r\n\r\n';
        request.add(utf8.encode(header));
        request.add(bytes);
        request.add(utf8.encode('\r\n--$boundary--\r\n'));

        final response = await request.close();
        final body = await utf8.decoder.bind(response).join();
        if (response.statusCode >= 200 && response.statusCode <= 299) {
          return;
        }
        lastError = Exception('Upload failed (${response.statusCode}): $body');
      } catch (e) {
        lastError = e;
      }
    }

    throw Exception('Phone upload failed: ${lastError ?? 'Unknown error'}');
  }

  Future<void> _sendFilesToPhone(List<String> paths) async {
    if (paths.isEmpty) return;
    setState(() {
      _phoneShareStatus = 'Sending ${paths.length} file(s) to phone...';
    });

    var sent = 0;
    for (final path in paths) {
      await _sendSingleFileToPhone(path);
      sent += 1;
    }

    if (!mounted) return;
    setState(() {
      _phoneShareStatus = 'Sent $sent file(s) to phone Downloads';
    });
  }

  Future<void> _pickFilesAndSendToPhone() async {
    try {
      final files = await openFiles();
      if (files.isEmpty) return;

      final paths = files
          .map((file) => file.path)
          .whereType<String>()
          .where((path) => path.isNotEmpty)
          .toList();
      if (paths.isEmpty) return;

      await _sendFilesToPhone(paths);
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _phoneShareStatus = 'File picker failed: $e';
      });
    }
  }

  void _consumePendingBrowserHandoff() {
    final handoffUrl = _pendingBrowserHandoffUrl.value;
    if (handoffUrl == null || handoffUrl.isEmpty || !mounted) return;

    _pendingBrowserHandoffUrl.value = null;
    setState(() {
      _showHelperDashboard = false;
      _showBrowserToolbar = true;
      _addressController.text = handoffUrl;
    });
    _activeTab.controller.loadRequest(Uri.parse(handoffUrl));
  }

  Future<void> _submitAddress(String rawInput) async {
    final query = rawInput.trim();
    if (query.isEmpty) return;

    final Uri targetUri;
    if (query.contains(' ') ||
        (!query.contains('.') && !query.startsWith('http'))) {
      targetUri = Uri.parse(_buildSearchUrl(query));
    } else if (query.startsWith('http://') || query.startsWith('https://')) {
      targetUri = Uri.parse(query);
    } else {
      targetUri = Uri.parse('https://$query');
    }

    await _activeTab.controller.loadRequest(targetUri);
  }

  Widget _buildTabStrip() {
    return Container(
      height: 44,
      color: const Color(0xFF121722),
      child: Row(
        children: [
          Expanded(
            child: ListView.builder(
              scrollDirection: Axis.horizontal,
              itemCount: _tabs.length,
              itemBuilder: (context, index) {
                final tab = _tabs[index];
                final selected = index == _activeTabIndex;
                final uri = Uri.tryParse(tab.url);
                final title = (uri?.host.isNotEmpty ?? false)
                    ? uri!.host
                    : tab.url;

                return GestureDetector(
                  onTap: () => _activateTab(index),
                  child: Container(
                    margin: const EdgeInsets.symmetric(
                      horizontal: 4,
                      vertical: 6,
                    ),
                    padding: const EdgeInsets.symmetric(
                      horizontal: 10,
                      vertical: 6,
                    ),
                    decoration: BoxDecoration(
                      color: selected
                          ? const Color(0xFF1C2536)
                          : const Color(0xFF161C29),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(
                        color: selected
                            ? Colors.blueAccent.withValues(alpha: 0.6)
                            : Colors.white.withValues(alpha: 0.08),
                      ),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(
                          tab.isLoading
                              ? Icons.hourglass_bottom
                              : Icons.language,
                          size: 14,
                          color: selected ? Colors.blueAccent : Colors.white70,
                        ),
                        const SizedBox(width: 6),
                        ConstrainedBox(
                          constraints: const BoxConstraints(maxWidth: 170),
                          child: Text(
                            title,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(
                              fontSize: 12,
                              color: selected ? Colors.white : Colors.white70,
                              fontWeight: selected
                                  ? FontWeight.w600
                                  : FontWeight.w400,
                            ),
                          ),
                        ),
                        const SizedBox(width: 6),
                        GestureDetector(
                          onTap: () => _closeTab(index),
                          child: const Icon(
                            Icons.close,
                            size: 14,
                            color: Colors.white60,
                          ),
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
          ),
          IconButton(
            tooltip: 'New Tab',
            onPressed: _openNewTab,
            icon: const Icon(Icons.add, color: Colors.white70, size: 18),
          ),
        ],
      ),
    );
  }

  Widget _buildBrowserControls() {
    return Container(
      padding: const EdgeInsets.fromLTRB(12, 10, 12, 8),
      color: const Color(0xFF151821),
      child: Column(
        children: [
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _addressController,
                  onSubmitted: _submitAddress,
                  style: const TextStyle(color: Colors.white),
                  decoration: InputDecoration(
                    hintText:
                        'Search ${_searchModeLabel(_searchMode)} or type URL',
                    hintStyle: TextStyle(color: Colors.grey.shade500),
                    filled: true,
                    fillColor: const Color(0xFF0F0F13),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(24),
                      borderSide: BorderSide.none,
                    ),
                    prefixIcon: const Icon(Icons.search, color: Colors.white70),
                  ),
                ),
              ),
              const SizedBox(width: 8),
              IconButton(
                tooltip: 'Go',
                onPressed: () => _submitAddress(_addressController.text),
                icon: const Icon(Icons.arrow_forward, color: Colors.white),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            crossAxisAlignment: WrapCrossAlignment.center,
            children: [
              _buildModeChip('Web', _SearchMode.web),
              _buildModeChip('AI', _SearchMode.ai),
              _buildModeChip('Images', _SearchMode.images),
              _buildModeChip('News', _SearchMode.news),
              OutlinedButton.icon(
                onPressed: () {
                  setState(() {
                    _browserZoom = 1.0;
                  });
                  _applyBrowserZoom(_activeTab);
                },
                icon: const Icon(Icons.restart_alt, size: 18),
                label: const Text('100%'),
              ),
              OutlinedButton.icon(
                onPressed: () => _setBrowserZoom(_browserZoom - 0.1),
                icon: const Icon(Icons.remove, size: 18),
                label: const Text('Zoom out'),
              ),
              OutlinedButton.icon(
                onPressed: () => _setBrowserZoom(_browserZoom + 0.1),
                icon: const Icon(Icons.add, size: 18),
                label: const Text('Zoom in'),
              ),
              OutlinedButton.icon(
                onPressed: () => _setAlwaysOnTop(!_isAlwaysOnTop),
                icon: Icon(
                  _isAlwaysOnTop ? Icons.push_pin : Icons.push_pin_outlined,
                  size: 18,
                ),
                label: Text(_isAlwaysOnTop ? 'Pinned' : 'Pin'),
              ),
              OutlinedButton.icon(
                onPressed: _openNewTab,
                icon: const Icon(Icons.add, size: 18),
                label: const Text('New tab'),
              ),
              OutlinedButton.icon(
                onPressed: () {
                  setState(() {
                    _showBrowserToolbar = !_showBrowserToolbar;
                  });
                },
                icon: Icon(
                  _showBrowserToolbar
                      ? Icons.keyboard_arrow_down
                      : Icons.keyboard_arrow_up,
                  size: 18,
                ),
                label: Text(
                  _showBrowserToolbar ? 'Hide controls' : 'Show controls',
                ),
              ),
              IconButton(
                tooltip: 'Back',
                onPressed: () async {
                  if (await _activeTab.controller.canGoBack()) {
                    await _activeTab.controller.goBack();
                  }
                },
                icon: const Icon(Icons.arrow_back, color: Colors.white70),
              ),
              IconButton(
                tooltip: 'Forward',
                onPressed: () async {
                  if (await _activeTab.controller.canGoForward()) {
                    await _activeTab.controller.goForward();
                  }
                },
                icon: const Icon(Icons.arrow_forward, color: Colors.white70),
              ),
              IconButton(
                tooltip: 'Reload',
                onPressed: () => _activeTab.controller.reload(),
                icon: const Icon(Icons.refresh, color: Colors.white70),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildModeChip(String label, _SearchMode mode) {
    final selected = _searchMode == mode;
    return ChoiceChip(
      label: Text(label),
      selected: selected,
      selectedColor: Colors.blueAccent.withValues(alpha: 0.24),
      onSelected: (_) => _setSearchMode(mode),
      labelStyle: TextStyle(
        color: selected ? Colors.white : Colors.white70,
        fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
      ),
      side: BorderSide(color: selected ? Colors.blueAccent : Colors.white24),
      backgroundColor: const Color(0xFF0F0F13),
    );
  }

  Widget _buildBrowserToggleBar() {
    return Container(
      height: 38,
      padding: const EdgeInsets.symmetric(horizontal: 12),
      color: const Color(0xFF151821),
      child: Row(
        children: [
          Text(
            'Browser controls',
            style: TextStyle(
              color: Colors.grey.shade300,
              fontSize: 12,
              fontWeight: FontWeight.w600,
            ),
          ),
          const Spacer(),
          IconButton(
            tooltip: 'Show browser controls',
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints.tightFor(width: 32, height: 32),
            onPressed: () {
              setState(() {
                _showBrowserToolbar = true;
              });
            },
            icon: const Icon(Icons.keyboard_arrow_up, color: Colors.white70),
          ),
        ],
      ),
    );
  }

  Widget _buildBrowserView() {
    return Shortcuts(
      shortcuts: <ShortcutActivator, Intent>{
        const SingleActivator(
          LogicalKeyboardKey.equal,
          meta: true,
          shift: true,
        ): const _ZoomInBrowserIntent(),
        const SingleActivator(LogicalKeyboardKey.minus, meta: true):
            const _ZoomOutBrowserIntent(),
      },
      child: Actions(
        actions: <Type, Action<Intent>>{
          _ZoomInBrowserIntent: CallbackAction<_ZoomInBrowserIntent>(
            onInvoke: (_) {
              _setBrowserZoom(_browserZoom + 0.1);
              return null;
            },
          ),
          _ZoomOutBrowserIntent: CallbackAction<_ZoomOutBrowserIntent>(
            onInvoke: (_) {
              _setBrowserZoom(_browserZoom - 0.1);
              return null;
            },
          ),
        },
        child: Focus(
          autofocus: true,
          child: Listener(
            onPointerPanZoomStart: (_) => _beginBrowserPinch(),
            onPointerPanZoomUpdate: (event) => _updateBrowserPinch(event.scale),
            onPointerPanZoomEnd: (_) => _endBrowserPinch(),
            child: DropTarget(
              onDragEntered: (_) {
                setState(() {
                  _isDraggingFiles = true;
                });
              },
              onDragExited: (_) {
                setState(() {
                  _isDraggingFiles = false;
                });
              },
              onDragDone: (detail) async {
                setState(() {
                  _isDraggingFiles = false;
                });
                final paths = detail.files
                    .map((file) => file.path)
                    .where((path) => path.isNotEmpty)
                    .toList();
                await _saveDroppedFiles(paths);
              },
              child: Column(
                children: [
                  if (_showBrowserToolbar)
                    _buildTabStrip()
                  else
                    _buildBrowserToggleBar(),
                  if (_showBrowserToolbar) _buildBrowserControls(),
                  if (_activeTab.isLoading)
                    const LinearProgressIndicator(
                      minHeight: 2,
                      color: Colors.blueAccent,
                    ),
                  Expanded(
                    child: Stack(
                      children: [
                        WebViewWidget(controller: _activeTab.controller),
                        if (_activeTab.loadError != null)
                          Positioned.fill(
                            child: Container(
                              color: const Color(
                                0xFF0F0F13,
                              ).withValues(alpha: 0.86),
                              padding: const EdgeInsets.all(24),
                              child: Center(
                                child: ConstrainedBox(
                                  constraints: const BoxConstraints(
                                    maxWidth: 420,
                                  ),
                                  child: Container(
                                    padding: const EdgeInsets.all(20),
                                    decoration: BoxDecoration(
                                      color: const Color(0xFF171B24),
                                      borderRadius: BorderRadius.circular(16),
                                      border: Border.all(
                                        color: Colors.white.withValues(
                                          alpha: 0.08,
                                        ),
                                      ),
                                    ),
                                    child: Column(
                                      mainAxisSize: MainAxisSize.min,
                                      children: [
                                        const Icon(
                                          Icons.cloud_off,
                                          color: Colors.orangeAccent,
                                          size: 42,
                                        ),
                                        const SizedBox(height: 12),
                                        const Text(
                                          'Page failed to load',
                                          textAlign: TextAlign.center,
                                          style: TextStyle(
                                            color: Colors.white,
                                            fontSize: 18,
                                            fontWeight: FontWeight.w700,
                                          ),
                                        ),
                                        const SizedBox(height: 8),
                                        Text(
                                          _activeTab.loadError!,
                                          textAlign: TextAlign.center,
                                          style: TextStyle(
                                            color: Colors.grey.shade300,
                                            fontSize: 13,
                                          ),
                                        ),
                                        const SizedBox(height: 16),
                                        FilledButton.icon(
                                          onPressed: () {
                                            setState(() {
                                              _activeTab.loadError = null;
                                              _activeTab.isLoading = true;
                                            });
                                            _activeTab.controller.reload();
                                          },
                                          icon: const Icon(Icons.refresh),
                                          label: const Text('Retry'),
                                        ),
                                      ],
                                    ),
                                  ),
                                ),
                              ),
                            ),
                          ),
                        if (_isDraggingFiles)
                          Positioned.fill(
                            child: IgnorePointer(
                              child: Container(
                                color: Colors.black.withValues(alpha: 0.38),
                                alignment: Alignment.center,
                                child: Container(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 22,
                                    vertical: 18,
                                  ),
                                  decoration: BoxDecoration(
                                    color: const Color(0xFF171B24),
                                    borderRadius: BorderRadius.circular(18),
                                    border: Border.all(
                                      color: Colors.white.withValues(
                                        alpha: 0.12,
                                      ),
                                    ),
                                  ),
                                  child: Column(
                                    mainAxisSize: MainAxisSize.min,
                                    children: [
                                      const Icon(
                                        Icons.cloud_upload,
                                        color: Colors.blueAccent,
                                        size: 40,
                                      ),
                                      const SizedBox(height: 10),
                                      const Text(
                                        'Drop files or images to save them',
                                        style: TextStyle(
                                          color: Colors.white,
                                          fontSize: 16,
                                          fontWeight: FontWeight.w700,
                                        ),
                                      ),
                                      const SizedBox(height: 4),
                                      Text(
                                        _dropStatus,
                                        style: TextStyle(
                                          color: Colors.grey.shade300,
                                          fontSize: 12,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              ),
                            ),
                          ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildHelperDashboard() {
    return Container(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFF0B1118), Color(0xFF101D2A), Color(0xFF17283B)],
        ),
      ),
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Container(
              padding: const EdgeInsets.all(18),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(18),
                border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
                gradient: LinearGradient(
                  colors: [
                    Colors.white.withValues(alpha: 0.07),
                    Colors.white.withValues(alpha: 0.03),
                  ],
                ),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Container(
                        width: 36,
                        height: 36,
                        decoration: BoxDecoration(
                          color: isConnected
                              ? const Color(0xFF1D5C3A)
                              : const Color(0xFF5C2D1D),
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: Icon(
                          isConnected ? Icons.wifi_tethering : Icons.wifi_off,
                          color: isConnected
                              ? const Color(0xFF7FFFD4)
                              : const Color(0xFFFFC38A),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(
                          isConnected
                              ? 'Helper link is live'
                              : 'Waiting for phone link',
                          style: const TextStyle(
                            color: Colors.white,
                            fontWeight: FontWeight.w700,
                            fontSize: 18,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10),
                  Text(
                    'Desktop: $deviceName',
                    style: TextStyle(color: Colors.grey.shade300, fontSize: 13),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Helper endpoint: $_helperIpAddress:8765',
                    style: TextStyle(color: Colors.grey.shade400, fontSize: 12),
                  ),
                  Text(
                    'Last phone IP: $lastClientIp',
                    style: TextStyle(color: Colors.grey.shade400, fontSize: 12),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 14),
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
                color: const Color(0xAA0D1622),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Send Files To Phone',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 16,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const SizedBox(height: 8),
                  TextFormField(
                    controller: _phoneEndpointController,
                    onChanged: (value) {
                      _phoneUploadEndpoint = value;
                    },
                    style: const TextStyle(color: Colors.white),
                    decoration: InputDecoration(
                      labelText: 'Phone upload endpoint',
                      labelStyle: TextStyle(color: Colors.grey.shade300),
                      hintText: 'http://<phone-ip>:8080',
                      hintStyle: TextStyle(color: Colors.grey.shade500),
                      filled: true,
                      fillColor: Colors.white.withValues(alpha: 0.04),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12),
                        borderSide: BorderSide(
                          color: Colors.white.withValues(alpha: 0.16),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(height: 8),
                  Align(
                    alignment: Alignment.centerRight,
                    child: TextButton.icon(
                      onPressed: () {
                        if (lastClientIp.isNotEmpty &&
                            lastClientIp != 'Unknown') {
                          setState(() {
                            _phoneUploadEndpoint = 'http://$lastClientIp:8080';
                            _phoneEndpointController.text =
                                _phoneUploadEndpoint;
                          });
                        }
                      },
                      icon: const Icon(Icons.auto_fix_high, size: 16),
                      label: const Text('Use last phone IP'),
                    ),
                  ),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      Expanded(
                        child: OutlinedButton.icon(
                          onPressed: _pickFilesAndSendToPhone,
                          icon: const Icon(Icons.folder_open),
                          label: const Text('Choose Files'),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: OutlinedButton.icon(
                          onPressed: () async {
                            final pickedFolder = await getDirectoryPath();
                            if (pickedFolder == null || pickedFolder.isEmpty) {
                              return;
                            }
                            final directory = Directory(pickedFolder);
                            if (!await directory.exists()) return;
                            final entries = directory
                                .listSync(recursive: true)
                                .whereType<File>()
                                .map((file) => file.path)
                                .toList();
                            if (entries.isEmpty) {
                              if (!mounted) return;
                              setState(() {
                                _phoneShareStatus =
                                    'No files found in selected folder';
                              });
                              return;
                            }
                            await _sendFilesToPhone(entries);
                          },
                          icon: const Icon(Icons.create_new_folder),
                          label: const Text('Choose Folder'),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  DropTarget(
                    onDragEntered: (_) {
                      setState(() {
                        _isDraggingFiles = true;
                      });
                    },
                    onDragExited: (_) {
                      setState(() {
                        _isDraggingFiles = false;
                      });
                    },
                    onDragDone: (detail) async {
                      setState(() {
                        _isDraggingFiles = false;
                      });
                      final paths = detail.files
                          .map((file) => file.path)
                          .where((path) => path.isNotEmpty)
                          .toList();
                      try {
                        await _sendFilesToPhone(paths);
                      } catch (e) {
                        if (!mounted) return;
                        setState(() {
                          _phoneShareStatus = 'Phone share failed: $e';
                        });
                      }
                    },
                    child: AnimatedContainer(
                      duration: const Duration(milliseconds: 180),
                      padding: const EdgeInsets.all(18),
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(14),
                        border: Border.all(
                          color: _isDraggingFiles
                              ? const Color(0xFF79D7FF)
                              : Colors.white.withValues(alpha: 0.14),
                          width: _isDraggingFiles ? 1.6 : 1,
                        ),
                        gradient: LinearGradient(
                          colors: _isDraggingFiles
                              ? [
                                  const Color(0x553A6D93),
                                  const Color(0x334AA3D6),
                                ]
                              : [
                                  Colors.white.withValues(alpha: 0.04),
                                  Colors.white.withValues(alpha: 0.02),
                                ],
                        ),
                      ),
                      child: Column(
                        children: [
                          Icon(
                            Icons.upload_file,
                            color: _isDraggingFiles
                                ? const Color(0xFF90E4FF)
                                : Colors.white70,
                            size: 34,
                          ),
                          const SizedBox(height: 10),
                          const Text(
                            'Drop files here to send to phone Downloads',
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              color: Colors.white,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                          const SizedBox(height: 6),
                          Text(
                            _phoneShareStatus,
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              color: Colors.grey.shade300,
                              fontSize: 12,
                            ),
                          ),
                          const SizedBox(height: 8),
                          Text(
                            'Supported: drag-and-drop, file chooser, folder chooser',
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              color: Colors.grey.shade500,
                              fontSize: 11,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 14),
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
                color: const Color(0xAA0D1622),
              ),
              child: Row(
                children: [
                  const Icon(Icons.sync_alt, color: Color(0xFF79D7FF)),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      'Clipboard sync is ${isConnected ? 'active' : 'waiting for device connection'}',
                      style: const TextStyle(color: Colors.white70),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _syncStatusFromRuntime() {
    if (!mounted) return;
    final status = _runtimeStatus.value;
    final now = DateTime.now();
    final recentlySeen =
        status.lastClientSeenAt != null &&
        now.difference(status.lastClientSeenAt!).inSeconds <= 12;

    setState(() {
      isConnected = status.serverOnline && recentlySeen;
      lastClientIp = status.lastClientIp;
      if (_phoneUploadEndpoint.isEmpty &&
          lastClientIp.isNotEmpty &&
          lastClientIp != 'Unknown') {
        _phoneUploadEndpoint = 'http://$lastClientIp:8080';
        _phoneEndpointController.text = _phoneUploadEndpoint;
      }
    });
  }

  Future<void> _checkConnection() async {
    final deviceInfo = DeviceInfoPlugin();
    String? deviceId;

    if (Platform.isAndroid) {
      AndroidDeviceInfo androidInfo = await deviceInfo.androidInfo;
      deviceId = androidInfo.id;
      deviceName = '${androidInfo.manufacturer} ${androidInfo.model}';
    } else if (Platform.isIOS) {
      IosDeviceInfo iosInfo = await deviceInfo.iosInfo;
      deviceId = iosInfo.identifierForVendor;
      deviceName = iosInfo.name;
    } else if (Platform.isMacOS) {
      MacOsDeviceInfo macInfo = await deviceInfo.macOsInfo;
      deviceId = macInfo.systemGUID ?? "desktop_mac";
      deviceName = macInfo.computerName;
    } else if (Platform.isWindows) {
      WindowsDeviceInfo winInfo = await deviceInfo.windowsInfo;
      deviceId = winInfo.deviceId;
      deviceName = winInfo.computerName;
    }

    if (deviceId != null && mounted) {
      _syncStatusFromRuntime();
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData.dark().copyWith(
        scaffoldBackgroundColor: const Color(0xFF0F0F13),
        appBarTheme: const AppBarTheme(
          backgroundColor: Color(0xFF0F0F13),
          elevation: 0,
        ),
      ),
      home: Scaffold(
        appBar: AppBar(
          title: Text(
            _showHelperDashboard ? 'Helper Dashboard' : 'Google Browser',
            style: const TextStyle(fontWeight: FontWeight.bold),
          ),
          centerTitle: true,
          actions: [
            IconButton(
              tooltip: _showHelperDashboard ? 'Show Browser' : 'Show Helper',
              icon: Icon(
                _showHelperDashboard ? Icons.travel_explore : Icons.tune,
              ),
              onPressed: () {
                setState(() {
                  _showHelperDashboard = !_showHelperDashboard;
                });
              },
            ),
            if (Platform.isMacOS || Platform.isWindows || Platform.isLinux)
              IconButton(
                icon: const Icon(Icons.close),
                onPressed: () {
                  windowManager.hide();
                },
              ),
          ],
        ),
        body: _showHelperDashboard
            ? _buildHelperDashboard()
            : _buildBrowserView(),
      ),
    );
  }
}
