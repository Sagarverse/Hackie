import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:math';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:device_info_plus/device_info_plus.dart';
import 'package:clipboard/clipboard.dart';
import 'package:file_selector/file_selector.dart';
import 'package:tray_manager/tray_manager.dart';
import 'package:desktop_drop/desktop_drop.dart';
import 'package:window_manager/window_manager.dart';
import 'package:get_mac_address/get_mac_address.dart';
import 'package:launch_at_startup/launch_at_startup.dart';
import 'package:webview_flutter/webview_flutter.dart';
import 'package:nsd/nsd.dart';
import 'package:http/http.dart' as http;
import 'package:cryptography/cryptography.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  bool isMobile = false;
  bool isDesktop = false;

  if (!kIsWeb) {
    isMobile = Platform.isAndroid || Platform.isIOS;
    isDesktop = Platform.isMacOS || Platform.isWindows || Platform.isLinux;
  }

  if (isMobile) {
    await initializeService();
  } else if (isDesktop) {
    await windowManager.ensureInitialized();

    try {
      launchAtStartup.setup(
        appName: 'Hackie Helper',
        appPath: Platform.resolvedExecutable,
      );
      final isEnabled = await launchAtStartup.isEnabled();
      if (!isEnabled) {
        await launchAtStartup.enable();
      }
    } catch (e) {
      debugPrint('Launch-at-startup setup failed: $e');
    }

    final WindowOptions windowOptions = WindowOptions(
      size: Size(400, 600),
      center: true,
      skipTaskbar: !kDebugMode,
      titleBarStyle: TitleBarStyle.hidden,
    );
    await windowManager.waitUntilReadyToShow(windowOptions, () async {
      await windowManager.setAlwaysOnTop(true);

      // Keep window visible during debug launches so macOS foregrounding can
      // complete cleanly. Hide at startup for non-debug builds.
      if (!kDebugMode) {
        await windowManager.hide(); // Start hidden, accessible via tray
      }
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
const String _settingsStoreFileName = '.hackie_helper_settings.json';

class _HelperFeatureSettings {
  final bool clipboardSyncEnabled;
  final bool remoteFileTransferEnabled;
  final bool remoteCommandEnabled;
  final bool browserHandoffEnabled;
  final bool noteSyncEnabled;
  final String defaultHomeUrl;
  final String authPin;

  const _HelperFeatureSettings({
    this.clipboardSyncEnabled = true,
    this.remoteFileTransferEnabled = true,
    this.remoteCommandEnabled = true,
    this.browserHandoffEnabled = true,
    this.noteSyncEnabled = true,
    this.defaultHomeUrl = 'https://www.google.com/?igu=1',
    this.authPin = '',
  });

  _HelperFeatureSettings copyWith({
    bool? clipboardSyncEnabled,
    bool? remoteFileTransferEnabled,
    bool? remoteCommandEnabled,
    bool? browserHandoffEnabled,
    bool? noteSyncEnabled,
    String? defaultHomeUrl,
    String? authPin,
  }) {
    return _HelperFeatureSettings(
      clipboardSyncEnabled: clipboardSyncEnabled ?? this.clipboardSyncEnabled,
      remoteFileTransferEnabled:
          remoteFileTransferEnabled ?? this.remoteFileTransferEnabled,
      remoteCommandEnabled: remoteCommandEnabled ?? this.remoteCommandEnabled,
      browserHandoffEnabled:
          browserHandoffEnabled ?? this.browserHandoffEnabled,
      noteSyncEnabled: noteSyncEnabled ?? this.noteSyncEnabled,
      defaultHomeUrl: defaultHomeUrl ?? this.defaultHomeUrl,
      authPin: authPin ?? this.authPin,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'clipboardSyncEnabled': clipboardSyncEnabled,
      'remoteFileTransferEnabled': remoteFileTransferEnabled,
      'remoteCommandEnabled': remoteCommandEnabled,
      'browserHandoffEnabled': browserHandoffEnabled,
      'noteSyncEnabled': noteSyncEnabled,
      'defaultHomeUrl': defaultHomeUrl,
      'authPin': authPin,
    };
  }

  static _HelperFeatureSettings fromJson(Map<String, dynamic> json) {
    return _HelperFeatureSettings(
      clipboardSyncEnabled: json['clipboardSyncEnabled'] != false,
      remoteFileTransferEnabled: json['remoteFileTransferEnabled'] != false,
      remoteCommandEnabled: json['remoteCommandEnabled'] != false,
      browserHandoffEnabled: json['browserHandoffEnabled'] != false,
      noteSyncEnabled: json['noteSyncEnabled'] != false,
      defaultHomeUrl:
          (json['defaultHomeUrl'] as String?)?.trim().isNotEmpty == true
          ? (json['defaultHomeUrl'] as String).trim()
          : 'https://www.google.com/?igu=1',
      authPin: (json['authPin'] as String?) ?? '',
    );
  }
}


_HelperFeatureSettings _helperSettings = const _HelperFeatureSettings();

File _settingsStoreFile() {
  final home = Platform.environment['HOME'];
  final base = (home != null && home.isNotEmpty)
      ? Directory(home)
      : Directory.systemTemp;
  return File('${base.path}${Platform.pathSeparator}$_settingsStoreFileName');
}

Future<void> _loadHelperSettingsFromDisk() async {
  try {
    final file = _settingsStoreFile();
    if (await file.exists()) {
      final raw = await file.readAsString();
      final decoded = jsonDecode(raw);
      if (decoded is Map<String, dynamic>) {
        _helperSettings = _HelperFeatureSettings.fromJson(decoded);
      }
    }
  } catch (e) {
    debugPrint('Failed to load helper settings: $e');
  }

  if (_helperSettings.authPin.isEmpty) {
    final rand = Random();
    final newPin = List.generate(4, (_) => rand.nextInt(10).toString()).join();
    _helperSettings = _helperSettings.copyWith(authPin: newPin);
    await _persistHelperSettings();
  }
}

Future<void> _persistHelperSettings() async {
  try {
    final file = _settingsStoreFile();
    await file.writeAsString(jsonEncode(_helperSettings.toJson()), flush: true);
  } catch (e) {
    debugPrint('Failed to persist helper settings: $e');
  }
}

bool _isShellCommandAllowed(String shellCmd) {
  final cmd = shellCmd.trim();
  if (cmd.isEmpty || cmd.length > 240) return false;

  // Block obvious shell chaining/injection characters.
  if (RegExp(r'[;&|><`$]').hasMatch(cmd)) return false;

  final lowered = cmd.toLowerCase();
  const blockedKeywords = <String>[
    ' rm ',
    'sudo',
    ' shutdown',
    ' reboot',
    ' mkfs',
    ' dd ',
    ' chown',
    ' chmod',
    ' mv ',
    ' kill ',
    ' pkill ',
  ];
  for (final keyword in blockedKeywords) {
    if ((' $lowered ').contains(keyword)) return false;
  }

  final firstToken = cmd.split(RegExp(r'\s+')).first.toLowerCase();
  const allowList = <String>{
    'ls',
    'pwd',
    'whoami',
    'date',
    'uname',
    'id',
    'echo',
    'cat',
    'head',
    'tail',
    'wc',
    'ps',
    'df',
    'du',
    'uptime',
    'sw_vers',
    'system_profiler',
  };
  return allowList.contains(firstToken);
}

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
    if (!_helperSettings.browserHandoffEnabled) {
      return {'success': false, 'error': 'Browser handoff is disabled'};
    }
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

  if (command == 'set_volume') {
    if (Platform.isMacOS) {
      final volume = (data['value'] as int?) ?? 50;
      await Process.run('osascript', ['-e', 'set volume output volume $volume']);
      return {'success': true};
    }
  }

  if (command == 'set_brightness') {
    if (Platform.isMacOS) {
      final brightness = ((data['value'] as int?) ?? 50) / 100.0;
      await Process.run('osascript', ['-e', 'tell application "System Events" to set value of attribute "AXValue" of slider 1 of group 1 of window 1 of process "ControlCenter" to $brightness']);
      // Fallback for some systems
      await Process.run('osascript', ['-e', 'do shell script "brightness $brightness"']);
      return {'success': true};
    }
  }

  if (command == 'run_shell') {
    final String shellCmd = (data['cmd'] as String?) ?? '';
    if (shellCmd.isNotEmpty) {
      if (!_isShellCommandAllowed(shellCmd)) {
        return {
          'success': false,
          'error':
              'Command blocked by policy. Only safe read-only commands are allowed.',
        };
      }
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

      final isPublicEndpoint = path == '/health' || path == '/info';
      if (!isPublicEndpoint && _helperSettings.authPin.isNotEmpty) {
        final providedPin = request.headers.value('x-auth-pin') ?? '';
        if (providedPin != _helperSettings.authPin) {
          request.response.statusCode = HttpStatus.forbidden;
          request.response.write(jsonEncode({'success': false, 'error': 'Invalid Auth PIN'}));
          await request.response.close();
          return;
        }
      }

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
        if (!_helperSettings.clipboardSyncEnabled) {
          request.response.statusCode = HttpStatus.forbidden;
          request.response.write(
            jsonEncode({'success': false, 'error': 'Clipboard sync disabled'}),
          );
          await request.response.close();
          return;
        }
        final currentClipboard = await FlutterClipboard.paste();
        request.response.headers.contentType = ContentType.json;
        request.response.write(jsonEncode({'content': currentClipboard}));
        await request.response.close();
        return;
      }

      if (request.method == 'POST' && path == '/clipboard') {
        if (!_helperSettings.clipboardSyncEnabled) {
          request.response.statusCode = HttpStatus.forbidden;
          request.response.write(
            jsonEncode({'success': false, 'error': 'Clipboard sync disabled'}),
          );
          await request.response.close();
          return;
        }
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

      if (request.method == 'POST' && path == '/open') {
        final payload =
            jsonDecode(await utf8.decoder.bind(request).join()) as Map;
        final targetPath = (payload['path'] as String?)?.trim() ?? '';
        final reveal = payload['reveal'] == true;
        if (targetPath.isEmpty) {
          request.response.statusCode = HttpStatus.badRequest;
          request.response.write(
            jsonEncode({'success': false, 'error': 'Missing path'}),
          );
          await request.response.close();
          return;
        }

        if (Platform.isMacOS) {
          if (reveal) {
            await Process.run('open', ['-R', targetPath]);
          } else {
            await Process.run('open', [targetPath]);
          }
        } else if (Platform.isWindows) {
          if (reveal) {
            await Process.run('explorer', ['/select,', targetPath]);
          } else {
            await Process.run('cmd', ['/c', 'start', '', targetPath]);
          }
        } else if (Platform.isLinux) {
          await Process.run('xdg-open', [
            reveal ? File(targetPath).parent.path : targetPath,
          ]);
        }

        request.response.headers.contentType = ContentType.json;
        request.response.write(jsonEncode({'success': true}));
        await request.response.close();
        return;
      }

      if (request.method == 'POST' && path == '/upload') {
        if (!_helperSettings.remoteFileTransferEnabled) {
          request.response.statusCode = HttpStatus.forbidden;
          request.response.write(
            jsonEncode({
              'success': false,
              'error': 'Remote file transfer is disabled',
            }),
          );
          await request.response.close();
          return;
        }
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
        if (!_helperSettings.remoteCommandEnabled) {
          request.response.statusCode = HttpStatus.forbidden;
          request.response.write(
            jsonEncode({
              'success': false,
              'error': 'Remote command execution is disabled',
            }),
          );
          await request.response.close();
          return;
        }
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
  await _loadHelperSettingsFromDisk();
  await _startDirectHelperServer();
  await _startLanBeacon();
  await _startPhoneDiscovery();
}

class CryptoHelper {
  static final _algorithm = AesGcm.with256bits();
  
  static Future<SecretKey> deriveKey(String pin, List<int> salt) async {
    final pbkdf2 = Pbkdf2(
      macAlgorithm: Hmac.sha256(),
      iterations: 10000,
      bits: 256,
    );
    return await pbkdf2.deriveKeyFromPassword(
      password: pin,
      nonce: salt,
    );
  }

  static Future<String?> encrypt(String text, String pin) async {
    try {
      final salt = List<int>.generate(16, (i) => DateTime.now().millisecond % 256); // Simple random for helper
      final secretKey = await deriveKey(pin, salt);
      final secretBox = await _algorithm.encrypt(
        utf8.encode(text),
        secretKey: secretKey,
      );
      final combined = [...salt, ...secretBox.nonce, ...secretBox.cipherText];
      return base64.encode(combined);
    } catch (e) { return null; }
  }

  static Future<String?> decrypt(String base64Ciphertext, String pin) async {
    try {
      final combined = base64.decode(base64Ciphertext);
      final salt = combined.sublist(0, 16);
      final iv = combined.sublist(16, 28);
      final ciphertext = combined.sublist(28);
      final secretKey = await deriveKey(pin, salt);
      // Note: cryptography package might handle MAC differently, adjusting for standard GCM
      final decrypted = await _algorithm.decrypt(
        SecretBox(ciphertext.sublist(0, ciphertext.length - 16), 
                  nonce: iv, 
                  mac: Mac(ciphertext.sublist(ciphertext.length - 16))),
        secretKey: secretKey,
      );
      return utf8.decode(decrypted);
    } catch (e) { return null; }
  }
}

Discovery? _nsdDiscovery;
final ValueNotifier<List<Service>> _foundPhones = ValueNotifier([]);

Future<void> _startPhoneDiscovery() async {
  _nsdDiscovery = await startDiscovery('_hackie-hub._tcp');
  _nsdDiscovery?.addListener(() {
    _foundPhones.value = _nsdDiscovery?.services.toList() ?? [];
    if (_foundPhones.value.isNotEmpty) {
      // Auto-connect to the first one for pro experience
      final service = _foundPhones.value.first;
      if (service.host != null && service.port != null) {
        debugPrint('Found Hackie Hub at ${service.host}:${service.port}');
      }
    }
  });
}

class GoogleServiceApp extends StatefulWidget {
  const GoogleServiceApp({super.key});

  @override
  State<GoogleServiceApp> createState() => _GoogleServiceAppState();
}

class _GoogleServiceAppState extends State<GoogleServiceApp>
    with TrayListener, WindowListener {
  static const MethodChannel _nativeShareChannel = MethodChannel(
    'hackie/share',
  );
  static const String _fallbackBrowserUrl = 'https://www.google.com/?igu=1';
  static const String _modernDesktopUserAgent =
      'Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) AppleWebKit/537.36 '
      '(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36';
  final GlobalKey<NavigatorState> _appNavigatorKey =
      GlobalKey<NavigatorState>();

  int _tabIdCounter = 0;

  final TextEditingController _addressController = TextEditingController(
    text: _fallbackBrowserUrl,
  );
  final TextEditingController _phoneEndpointController =
      TextEditingController();

  bool isConnected = false;
  bool _showHelperDashboard = false;
  bool _dashboardUnlocked = false;
  final TextEditingController _dashboardPasswordController =
      TextEditingController();
  String? _dashboardUnlockError;
  bool _showBrowserToolbar = false;
  bool _isAlwaysOnTop = true;
  bool _isDraggingFiles = false;
  bool _isPinchingBrowser = false;
  double _browserZoom = 1.0;
  double _pinchStartZoom = 1.0;
  final TextEditingController _homeUrlController = TextEditingController();
  String _dropStatus = 'Drop files or images here to upload';
  String _phoneUploadEndpoint = '';
  String _phoneShareStatus =
      'Drop files in the Helper dashboard to send them to phone Downloads';
  String deviceName = "Unknown Device";
  String lastClientIp = "Unknown";
  Timer? _statusRefreshTimer;
  Timer? _nowPlayingPollTimer;
  bool _nowPlayingSyncInFlight = false;
  String? _lastNowPlayingSignature;
  // ignore: unused_field
  String _nowPlayingLastDetected = 'No metadata detected yet';
  // ignore: unused_field
  String _nowPlayingLastPushStatus = 'No push attempted yet';
  // ignore: unused_field
  String _nowPlayingLastError = '';
  final List<_BrowserTab> _tabs = [];
  int _activeTabIndex = 0;
  Timer? _clipboardWatcherTimer;
  String _lastLocalClipboard = '';

  _BrowserTab get _activeTab => _tabs[_activeTabIndex];

  Future<void> _setAlwaysOnTop(bool value) async {
    _isAlwaysOnTop = value;
    await windowManager.setAlwaysOnTop(value);
    if (mounted) setState(() {});
  }

  String get _defaultBrowserUrl {
    final saved = _helperSettings.defaultHomeUrl.trim();
    if (saved.isEmpty) return _fallbackBrowserUrl;
    final withScheme =
        saved.startsWith('http://') || saved.startsWith('https://')
        ? saved
        : 'https://$saved';
    final parsed = Uri.tryParse(withScheme);
    return (parsed == null || parsed.host.isEmpty)
        ? _fallbackBrowserUrl
        : withScheme;
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
      ..setUserAgent(_modernDesktopUserAgent)
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

    _nativeShareChannel.setMethodCallHandler((call) async {
      if (call.method != 'openFiles') return;
      final dynamic args = call.arguments;
      final paths = (args is List)
          ? args.whereType<String>().toList()
          : <String>[];
      await _handleIncomingSharedFiles(paths);
    });

    _tabs.add(_createTab(_defaultBrowserUrl));
    _addressController.text = _tabs.first.url;
    _homeUrlController.text = _helperSettings.defaultHomeUrl;

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
    _startNowPlayingPolling();
    _startClipboardWatcher();
  }

  void _startClipboardWatcher() {
    _clipboardWatcherTimer?.cancel();
    _clipboardWatcherTimer = Timer.periodic(const Duration(milliseconds: 1500), (_) async {
      if (!_helperSettings.clipboardSyncEnabled || _showHelperDashboard) return;
      
      try {
        final current = await FlutterClipboard.paste();
        if (current.isNotEmpty && current != _lastLocalClipboard) {
          _lastLocalClipboard = current;
          await _pushClipboardToPhone(current);
        }
        await _pullClipboardFromPhone();
      } catch (_) {}
    });
  }

  String get _currentPin => _dashboardPasswordController.text.trim().isEmpty ? "0000" : _dashboardPasswordController.text.trim();

  Future<void> _pullClipboardFromPhone() async {
    final endpoint = _normalizePhoneUploadEndpoint(_phoneUploadEndpoint);
    if (endpoint.isEmpty) return;
    
    try {
      final response = await http.get(
        Uri.parse('$endpoint/clipboard'),
        headers: {'X-Session-Token': _dashboardPasswordController.text},
      ).timeout(const Duration(seconds: 2));
      
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        final encrypted = data['text'] as String?;
        if (encrypted != null) {
          final decrypted = await CryptoHelper.decrypt(encrypted, _currentPin);
          if (decrypted != null && decrypted != _lastLocalClipboard) {
            await FlutterClipboard.copy(decrypted);
            _lastLocalClipboard = decrypted;
          }
        }
      }
    } catch (_) {}
  }

  Future<void> _pushClipboardToPhone(String text) async {
    final endpoint = _normalizePhoneUploadEndpoint(_phoneUploadEndpoint);
    if (endpoint.isEmpty) return;

    try {
      final encrypted = await CryptoHelper.encrypt(text, _currentPin);
      if (encrypted == null) return;

      await http.post(
        Uri.parse('$endpoint/clipboard'),
        headers: {
          'Content-Type': 'application/json',
          'X-Session-Token': _dashboardPasswordController.text,
        },
        body: jsonEncode({'text': encrypted}),
      ).timeout(const Duration(seconds: 2));
    } catch (_) {}
  }

  Future<void> _initTray() async {
    await trayManager.setIcon(
      Platform.isWindows ? 'web/icons/Icon-192.png' : 'web/icons/Icon-192.png',
    );
    await trayManager.setToolTip('Hackie Helper');
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

  Future<void> _showHelperWindow() async {
    await windowManager.show();
    await windowManager.setAlwaysOnTop(_isAlwaysOnTop);
    // Give the host a short moment to complete window activation before
    // requesting focus to reduce flaky foreground failures on macOS.
    await Future.delayed(const Duration(milliseconds: 120));
    await windowManager.focus();
  }

  Future<String> _runAppleScript(String script) async {
    if (!Platform.isMacOS) return '';
    try {
      final result = await Process.run('osascript', ['-e', script]);
      if (result.exitCode != 0) {
        final stderr = (result.stderr?.toString() ?? '').trim();
        if (stderr.isNotEmpty && mounted) {
          setState(() {
            _nowPlayingLastError = stderr;
          });
        }
        return '';
      }
      return (result.stdout?.toString() ?? '').trim();
    } catch (_) {
      if (mounted) {
        setState(() {
          _nowPlayingLastError = 'osascript invocation failed';
        });
      }
      return '';
    }
  }

  Future<bool> _isMacAppRunning(String appName) async {
    final value = await _runAppleScript(
      'tell application "System Events" to (name of processes) contains "$appName"',
    );
    return value.toLowerCase() == 'true';
  }

  Future<Map<String, String>?> _readNowPlayingFromMac() async {
    if (!Platform.isMacOS) return null;

    final isMusicRunning = await _isMacAppRunning('Music');
    final musicState = isMusicRunning
        ? await _runAppleScript('tell application "Music" to player state')
        : '';
    if (musicState.toLowerCase() == 'playing') {
      final title = await _runAppleScript(
        'tell application "Music" to name of current track',
      );
      final artist = await _runAppleScript(
        'tell application "Music" to artist of current track',
      );
      final album = await _runAppleScript(
        'tell application "Music" to album of current track',
      );
      if (title.isNotEmpty) {
        return {
          'title': title,
          'artist': artist,
          'album': album,
          'source': 'music',
        };
      }
    }

    final isSpotifyRunning = await _isMacAppRunning('Spotify');
    final spotifyState = isSpotifyRunning
        ? await _runAppleScript('tell application "Spotify" to player state')
        : '';
    if (spotifyState.toLowerCase() == 'playing') {
      final title = await _runAppleScript(
        'tell application "Spotify" to name of current track',
      );
      final artist = await _runAppleScript(
        'tell application "Spotify" to artist of current track',
      );
      final album = await _runAppleScript(
        'tell application "Spotify" to album of current track',
      );
      if (title.isNotEmpty) {
        return {
          'title': title,
          'artist': artist,
          'album': album,
          'source': 'spotify',
        };
      }
    }

    final systemBrowserNowPlaying = await _readNowPlayingFromSystemBrowsers();
    if (systemBrowserNowPlaying != null) {
      return systemBrowserNowPlaying;
    }

    final browserNowPlaying = await _readNowPlayingFromBrowser();
    if (browserNowPlaying != null) {
      return browserNowPlaying;
    }

    return null;
  }

  Future<Map<String, String>?> _readNowPlayingFromSystemBrowsers() async {
    if (!Platform.isMacOS) return null;

    final browserScripts = <Map<String, String>>[
      {
        'app': 'Google Chrome',
        'script':
            'tell application "Google Chrome"\n'
            'if (count of windows) = 0 then return ""\n'
            'set t to title of active tab of front window\n'
            'set u to URL of active tab of front window\n'
            'return t & linefeed & u\n'
            'end tell',
      },
      {
        'app': 'Microsoft Edge',
        'script':
            'tell application "Microsoft Edge"\n'
            'if (count of windows) = 0 then return ""\n'
            'set t to title of active tab of front window\n'
            'set u to URL of active tab of front window\n'
            'return t & linefeed & u\n'
            'end tell',
      },
      {
        'app': 'Safari',
        'script':
            'tell application "Safari"\n'
            'if (count of windows) = 0 then return ""\n'
            'set t to name of current tab of front window\n'
            'set u to URL of current tab of front window\n'
            'return t & linefeed & u\n'
            'end tell',
      },
    ];

    for (final browser in browserScripts) {
      final appName = browser['app']!;
      if (!await _isMacAppRunning(appName)) continue;

      final result = await _runAppleScript(browser['script']!);
      if (result.isEmpty) continue;

      final lines = result
          .split('\n')
          .map((line) => line.trim())
          .where((line) => line.isNotEmpty)
          .toList();
      if (lines.length < 2) continue;

      final title = lines.first;
      final url = lines.last;
      final host = Uri.tryParse(url)?.host ?? '';
      if (host.isEmpty) continue;

      final parsed = _parseNowPlayingFromBrowser(host, title);
      if (parsed != null) {
        return parsed;
      }
    }

    return null;
  }

  String _normalizeJavaScriptResult(Object? value) {
    var text = value?.toString() ?? '';
    text = text.trim();
    if (text.length >= 2 && text.startsWith('"') && text.endsWith('"')) {
      text = text.substring(1, text.length - 1);
    }
    return text
        .replaceAll(r'\n', ' ')
        .replaceAll(r'\t', ' ')
        .replaceAll(r'\"', '"')
        .trim();
  }

  Map<String, String>? _parseNowPlayingFromBrowser(String host, String title) {
    final normalizedHost = host.toLowerCase();
    final rawTitle = title.trim();
    if (rawTitle.isEmpty) return null;

    if (normalizedHost.contains('spotify')) {
      final cleaned = rawTitle
          .replaceAll(RegExp(r'\s*\|\s*Spotify\s*$'), '')
          .trim();
      final songByParts = cleaned.split(' - song by ');
      if (songByParts.length == 2) {
        return {
          'title': songByParts[0].trim(),
          'artist': songByParts[1].trim(),
          'album': '',
          'source': 'spotify-web',
        };
      }
      final hyphenParts = cleaned.split(' - ');
      if (hyphenParts.length >= 2) {
        return {
          'title': hyphenParts.sublist(1).join(' - ').trim(),
          'artist': hyphenParts.first.trim(),
          'album': '',
          'source': 'spotify-web',
        };
      }
    }

    if (normalizedHost.contains('music.youtube.com') ||
        rawTitle.toLowerCase().contains('youtube music')) {
      final cleaned = rawTitle
          .replaceAll(RegExp(r'\s*-\s*YouTube Music\s*$'), '')
          .trim();
      final parts = cleaned.split(' - ');
      if (parts.length >= 2) {
        return {
          'title': parts.first.trim(),
          'artist': parts.sublist(1).join(' - ').trim(),
          'album': '',
          'source': 'youtube-music-web',
        };
      }
      return {
        'title': cleaned,
        'artist': 'Unknown artist',
        'album': '',
        'source': 'youtube-music-web',
      };
    }

    if (normalizedHost.contains('music.apple.com') ||
        rawTitle.contains('Apple Music')) {
      final cleaned = rawTitle
          .replaceAll(RegExp(r'\s*[|\-]\s*Apple Music\s*$'), '')
          .trim();
      final parts = cleaned.split(' - ');
      if (parts.length >= 2) {
        return {
          'title': parts.first.trim(),
          'artist': parts.sublist(1).join(' - ').trim(),
          'album': '',
          'source': 'apple-music-web',
        };
      }
    }

    return null;
  }

  Future<Map<String, String>?> _readNowPlayingFromBrowser() async {
    if (_tabs.isEmpty) return null;

    final tab = _tabs[_activeTabIndex];
    final host = Uri.tryParse(tab.url)?.host ?? '';
    if (host.isEmpty) return null;

    try {
      final titleRaw = await tab.controller.runJavaScriptReturningResult(
        'document.title || ""',
      );
      final title = _normalizeJavaScriptResult(titleRaw);
      if (title.isEmpty) return null;
      return _parseNowPlayingFromBrowser(host, title);
    } catch (_) {
      return null;
    }
  }

  Future<bool> _postNowPlayingToPhone(Map<String, String> metadata) async {
    final endpoint = _normalizePhoneUploadEndpoint(_phoneUploadEndpoint);
    if (endpoint.isEmpty) return false;

    final payload = <String, dynamic>{
      'title': metadata['title'] ?? 'No track',
      'artist': metadata['artist'] ?? 'Unknown artist',
      'album': metadata['album'] ?? '',
      'artworkBase64': null,
      'source': metadata['source'] ?? 'helper_app',
    };

    final urls = <String>[
      '$endpoint/helper/now-playing',
      '$endpoint/now-playing',
    ];

    for (final url in urls) {
      try {
        final client = HttpClient();
        final request = await client.postUrl(Uri.parse(url));
        request.headers.contentType = ContentType.json;
        request.add(utf8.encode(jsonEncode(payload)));
        final response = await request.close();
        await response.drain();
        client.close(force: true);
        if (response.statusCode >= 200 && response.statusCode < 300) {
          if (mounted) {
            setState(() {
              _nowPlayingLastPushStatus =
                  'Pushed to $url (${response.statusCode}) at ${DateTime.now().toLocal()}';
              _nowPlayingLastError = '';
            });
          }
          return true;
        }
        if (mounted) {
          setState(() {
            _nowPlayingLastPushStatus =
                'Push failed to $url (${response.statusCode})';
          });
        }
      } catch (_) {
        if (mounted) {
          setState(() {
            _nowPlayingLastPushStatus = 'Push error to $url';
          });
        }
        continue;
      }
    }

    return false;
  }

  Future<void> _syncNowPlayingTick() async {
    if (!Platform.isMacOS || _nowPlayingSyncInFlight) return;

    _nowPlayingSyncInFlight = true;
    try {
      final metadata = await _readNowPlayingFromMac();
      if (metadata == null) {
        if (mounted) {
          setState(() {
            _nowPlayingLastDetected =
                'No active song detected from Music/Spotify/browser';
          });
        }
        return;
      }

      if (mounted) {
        final summary =
            '${metadata['title'] ?? 'No track'} — ${metadata['artist'] ?? 'Unknown'} [${metadata['source'] ?? 'unknown'}]';
        setState(() {
          _nowPlayingLastDetected = summary;
        });
      }

      final signature =
          '${metadata['source']}|${metadata['title']}|${metadata['artist']}|${metadata['album']}';
      if (signature == _lastNowPlayingSignature) return;

      final pushed = await _postNowPlayingToPhone(metadata);
      if (pushed) {
        _lastNowPlayingSignature = signature;
      } else if (mounted) {
        setState(() {
          _nowPlayingLastPushStatus = 'Detected metadata but push failed';
        });
      }
    } finally {
      _nowPlayingSyncInFlight = false;
    }
  }

  void _startNowPlayingPolling() {
    if (!Platform.isMacOS) return;
    _nowPlayingPollTimer?.cancel();
    _syncNowPlayingTick();
    _nowPlayingPollTimer = Timer.periodic(const Duration(seconds: 3), (_) {
      _syncNowPlayingTick();
    });
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
          _showHelperDashboard = true;
          _addressController.text = _activeTab.url;
        });
      }
      unawaited(_showHelperWindow());
    } else if (menuItem.key == 'exit_app') {
      exit(0);
    }
  }

  @override
  void dispose() {
    _nativeShareChannel.setMethodCallHandler(null);
    _addressController.dispose();
    _phoneEndpointController.dispose();
    _homeUrlController.dispose();
    _dashboardPasswordController.dispose();
    _statusRefreshTimer?.cancel();
    _nowPlayingPollTimer?.cancel();
    _clipboardWatcherTimer?.cancel();
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

  Future<void> _ensureRemoteFileTransferEnabled() async {
    if (_helperSettings.remoteFileTransferEnabled) return;
    await _updateFeatureSetting(
      _helperSettings.copyWith(remoteFileTransferEnabled: true),
    );
    _showInfoSnack('Remote File Transfer was disabled. Enabled now.');
  }

  Future<void> _pickFolderAndSendToPhone() async {
    try {
      await _ensureRemoteFileTransferEnabled();
      final pickedFolder = await getDirectoryPath();
      if (pickedFolder == null || pickedFolder.isEmpty) return;

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
          _phoneShareStatus = 'No files found in selected folder';
        });
        return;
      }

      await _sendFilesToPhone(entries);
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _phoneShareStatus = 'Folder picker failed: $e';
      });
      _showInfoSnack('Folder upload failed: $e', isError: true);
    }
  }

  Future<void> _pickFilesAndSendToPhone() async {
    try {
      await _ensureRemoteFileTransferEnabled();
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
      _showInfoSnack('File upload failed: $e', isError: true);
    }
  }

  Future<void> _handleIncomingSharedFiles(List<String> incomingPaths) async {
    final paths = incomingPaths
        .map((path) => path.trim())
        .where((path) => path.isNotEmpty)
        .toList();
    if (paths.isEmpty || !mounted) return;

    setState(() {
      _showHelperDashboard = true;
      _phoneShareStatus = 'Ready to send ${paths.length} file(s) to phone';
      _dropStatus = 'Received ${paths.length} file(s) via Share menu';
    });

    if (Platform.isMacOS || Platform.isWindows || Platform.isLinux) {
      await _showHelperWindow();
    }

    try {
      // Auto-enable remote transfer for incoming manual shares
      await _ensureRemoteFileTransferEnabled();
      
      final endpoint = _normalizePhoneUploadEndpoint(_phoneUploadEndpoint);
      if (endpoint.isNotEmpty) {
        await _sendFilesToPhone(paths);
      } else {
        await _saveDroppedFiles(paths);
        if (!mounted) return;
        setState(() {
          _phoneShareStatus =
              'Files staged. Provide phone endpoint to finish transfer.';
        });
      }
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _phoneShareStatus = 'Share failed: $e';
      });
      _showInfoSnack('Share failed: $e', isError: true);
    }
  }

  Future<void> _updateFeatureSetting(_HelperFeatureSettings next) async {
    setState(() {
      _helperSettings = next;
    });
    await _persistHelperSettings();
  }

  Future<void> _saveDefaultHomeUrl() async {
    final raw = _homeUrlController.text.trim();
    if (raw.isEmpty) {
      _showInfoSnack('Default home URL cannot be empty', isError: true);
      return;
    }

    final withScheme = raw.startsWith('http://') || raw.startsWith('https://')
        ? raw
        : 'https://$raw';
    final parsed = Uri.tryParse(withScheme);
    if (parsed == null || parsed.host.isEmpty) {
      _showInfoSnack('Enter a valid URL, e.g. google.com', isError: true);
      return;
    }

    await _updateFeatureSetting(
      _helperSettings.copyWith(defaultHomeUrl: withScheme),
    );
    _showInfoSnack('Default home URL saved');
  }

  void _tryUnlockDashboard() {
    if (_dashboardPasswordController.text.trim() == '2005') {
      setState(() {
        _dashboardUnlocked = true;
        _dashboardUnlockError = null;
      });
      _showInfoSnack('Dashboard unlocked');
      return;
    }
    setState(() {
      _dashboardUnlockError = 'Wrong password';
    });
  }

  void _lockDashboard() {
    setState(() {
      _dashboardUnlocked = false;
      _showHelperDashboard = false;
      _dashboardUnlockError = null;
      _dashboardPasswordController.clear();
    });
  }

  void _showInfoSnack(String message, {bool isError = false}) {
    if (!mounted) return;
    final messenger = ScaffoldMessenger.of(context);
    messenger.hideCurrentSnackBar();
    messenger.showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: isError
            ? const Color(0xFF8A2C2C)
            : const Color(0xFF1E2B3C),
        behavior: SnackBarBehavior.floating,
        duration: const Duration(seconds: 2),
      ),
    );
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
      final encoded = Uri.encodeComponent(query);
      targetUri = Uri.parse('https://www.google.com/search?q=$encoded');
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
      margin: const EdgeInsets.fromLTRB(10, 8, 10, 10),
      padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
      decoration: BoxDecoration(
        color: const Color(0xFF0D1522),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.24),
            blurRadius: 20,
            offset: const Offset(0, 8),
          ),
        ],
      ),
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
                    hintText: 'Search or enter URL',
                    hintStyle: TextStyle(color: Colors.grey.shade500),
                    filled: true,
                    fillColor: const Color(0xFF050B14),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(16),
                      borderSide: BorderSide.none,
                    ),
                    prefixIcon: const Icon(Icons.search, color: Colors.white70),
                  ),
                ),
              ),
              const SizedBox(width: 8),
              FilledButton(
                onPressed: () => _submitAddress(_addressController.text),
                style: FilledButton.styleFrom(
                  backgroundColor: const Color(0xFF5EEAD4),
                  foregroundColor: const Color(0xFF03131A),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(14),
                  ),
                  padding: const EdgeInsets.symmetric(
                    horizontal: 14,
                    vertical: 14,
                  ),
                ),
                child: const Icon(Icons.arrow_forward, size: 18),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            crossAxisAlignment: WrapCrossAlignment.center,
            children: [
              _buildMiniToolbarButton(
                icon: Icons.restart_alt,
                label: '${(_browserZoom * 100).round()}%',
                onPressed: () {
                  setState(() {
                    _browserZoom = 1.0;
                  });
                  _applyBrowserZoom(_activeTab);
                },
              ),
              _buildMiniToolbarButton(
                icon: Icons.remove,
                label: 'Zoom out',
                onPressed: () => _setBrowserZoom(_browserZoom - 0.1),
              ),
              _buildMiniToolbarButton(
                icon: Icons.add,
                label: 'Zoom in',
                onPressed: () => _setBrowserZoom(_browserZoom + 0.1),
              ),
              _buildMiniToolbarButton(
                icon: _isAlwaysOnTop ? Icons.push_pin : Icons.push_pin_outlined,
                label: _isAlwaysOnTop ? 'Pinned' : 'Pin',
                onPressed: () => _setAlwaysOnTop(!_isAlwaysOnTop),
              ),
              _buildMiniToolbarButton(
                icon: Icons.add,
                label: 'New tab',
                onPressed: _openNewTab,
              ),
              _buildMiniToolbarButton(
                icon: _showBrowserToolbar
                    ? Icons.keyboard_arrow_down
                    : Icons.keyboard_arrow_up,
                label: _showBrowserToolbar ? 'Hide controls' : 'Show controls',
                onPressed: () {
                  setState(() {
                    _showBrowserToolbar = !_showBrowserToolbar;
                  });
                },
              ),
              _buildMiniIconButton(
                icon: Icons.arrow_back,
                tooltip: 'Back',
                onPressed: () async {
                  if (await _activeTab.controller.canGoBack()) {
                    await _activeTab.controller.goBack();
                  }
                },
              ),
              _buildMiniIconButton(
                icon: Icons.arrow_forward,
                tooltip: 'Forward',
                onPressed: () async {
                  if (await _activeTab.controller.canGoForward()) {
                    await _activeTab.controller.goForward();
                  }
                },
              ),
              _buildMiniIconButton(
                icon: Icons.refresh,
                tooltip: 'Reload',
                onPressed: () => _activeTab.controller.reload(),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildMiniToolbarButton({
    required IconData icon,
    required String label,
    required VoidCallback onPressed,
  }) {
    return OutlinedButton.icon(
      onPressed: onPressed,
      style: OutlinedButton.styleFrom(
        foregroundColor: Colors.white,
        side: BorderSide(color: Colors.white.withValues(alpha: 0.16)),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        backgroundColor: Colors.white.withValues(alpha: 0.02),
      ),
      icon: Icon(icon, size: 16),
      label: Text(label),
    );
  }

  Widget _buildMiniIconButton({
    required IconData icon,
    required String tooltip,
    required VoidCallback onPressed,
  }) {
    return Tooltip(
      message: tooltip,
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: onPressed,
        child: Container(
          width: 38,
          height: 38,
          decoration: BoxDecoration(
            color: Colors.white.withValues(alpha: 0.04),
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: Colors.white.withValues(alpha: 0.12)),
          ),
          child: Icon(icon, size: 18, color: Colors.white70),
        ),
      ),
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
            tooltip: 'Back',
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints.tightFor(width: 32, height: 32),
            onPressed: () async {
              if (await _activeTab.controller.canGoBack()) {
                await _activeTab.controller.goBack();
              }
            },
            icon: const Icon(Icons.arrow_back, color: Colors.white70, size: 18),
          ),
          IconButton(
            tooltip: 'Forward',
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints.tightFor(width: 32, height: 32),
            onPressed: () async {
              if (await _activeTab.controller.canGoForward()) {
                await _activeTab.controller.goForward();
              }
            },
            icon: const Icon(
              Icons.arrow_forward,
              color: Colors.white70,
              size: 18,
            ),
          ),
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
    if (!_dashboardUnlocked) {
      return Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [Color(0xFF060D1A), Color(0xFF0F172A), Color(0xFF1E293B)],
          ),
        ),
        child: Center(
          child: Container(
            width: 340,
            padding: const EdgeInsets.all(24),
            decoration: BoxDecoration(
              color: const Color(0xC20F172A),
              borderRadius: BorderRadius.circular(24),
              border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.3),
                  blurRadius: 30,
                  offset: const Offset(0, 10),
                ),
              ],
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: const Color(0xFF5EEAD4).withValues(alpha: 0.1),
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(
                    Icons.lock_person,
                    color: Color(0xFF5EEAD4),
                    size: 40,
                  ),
                ),
                const SizedBox(height: 16),
                const Text(
                  'Hackie Secured',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 22,
                    fontWeight: FontWeight.w800,
                    letterSpacing: -0.5,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  'Enter terminal key to unlock dashboard',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    color: Colors.grey.shade400,
                    fontSize: 13,
                    height: 1.4,
                  ),
                ),
                const SizedBox(height: 24),
                TextField(
                  controller: _dashboardPasswordController,
                  obscureText: true,
                  autofocus: true,
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 18,
                    letterSpacing: 4,
                  ),
                  decoration: InputDecoration(
                    hintText: '••••',
                    hintStyle: TextStyle(
                      color: Colors.grey.shade600,
                      letterSpacing: 4,
                    ),
                    filled: true,
                    fillColor: Colors.black.withValues(alpha: 0.3),
                    contentPadding: const EdgeInsets.symmetric(vertical: 16),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(16),
                      borderSide: BorderSide.none,
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(16),
                      borderSide: const BorderSide(
                        color: Color(0xFF5EEAD4),
                        width: 1.5,
                      ),
                    ),
                  ),
                  onSubmitted: (_) => _tryUnlockDashboard(),
                ),
                if (_dashboardUnlockError != null) ...[
                  const SizedBox(height: 12),
                  Text(
                    _dashboardUnlockError!,
                    style: const TextStyle(
                      color: Color(0xFFFF6B6B),
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
                const SizedBox(height: 24),
                SizedBox(
                  width: double.infinity,
                  height: 52,
                  child: FilledButton(
                    onPressed: _tryUnlockDashboard,
                    style: FilledButton.styleFrom(
                      backgroundColor: const Color(0xFF5EEAD4),
                      foregroundColor: const Color(0xFF0F172A),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(16),
                      ),
                      elevation: 0,
                    ),
                    child: const Text(
                      'Unlock Dashboard',
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 15,
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      );
    }

    return Container(
      decoration: const BoxDecoration(
        color: Color(0xFF0F172A),
      ),
      child: Stack(
        children: [
          // Background Mesh Decoration
          Positioned(
            top: -100,
            right: -100,
            child: Container(
              width: 300,
              height: 300,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: const Color(0xFF5EEAD4).withValues(alpha: 0.05),
              ),
            ),
          ),
          SingleChildScrollView(
            physics: const BouncingScrollPhysics(),
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                // Connection Status Card
                _buildSkeuoCard(
                  child: Column(
                    children: [
                      Row(
                        children: [
                          Container(
                            width: 52,
                            height: 52,
                            decoration: BoxDecoration(
                              color: isConnected
                                  ? const Color(0xFF10B981).withValues(alpha: 0.15)
                                  : const Color(0xFFF59E0B).withValues(alpha: 0.15),
                              borderRadius: BorderRadius.circular(16),
                              border: Border.all(
                                color: isConnected
                                    ? const Color(0xFF10B981).withValues(alpha: 0.3)
                                    : const Color(0xFFF59E0B).withValues(alpha: 0.3),
                              ),
                            ),
                            child: Icon(
                              isConnected ? Icons.connected_tv : Icons.wifi_off,
                              color: isConnected
                                  ? const Color(0xFF10B981)
                                  : const Color(0xFFF59E0B),
                              size: 26,
                            ),
                          ),
                          const SizedBox(width: 16),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  isConnected ? 'System Linked' : 'System Standby',
                                  style: const TextStyle(
                                    color: Colors.white,
                                    fontWeight: FontWeight.w800,
                                    fontSize: 18,
                                    letterSpacing: -0.5,
                                  ),
                                ),
                                const SizedBox(height: 4),
                                Row(
                                  children: [
                                    Container(
                                      width: 8,
                                      height: 8,
                                      decoration: BoxDecoration(
                                        shape: BoxShape.circle,
                                        color: isConnected
                                            ? const Color(0xFF10B981)
                                            : Colors.grey,
                                      ),
                                    ),
                                    const SizedBox(width: 6),
                                    Text(
                                      isConnected ? 'Online: $lastClientIp' : 'Awaiting Peer Connection',
                                      style: TextStyle(
                                        color: Colors.grey.shade400,
                                        fontSize: 12,
                                        fontWeight: FontWeight.w500,
                                      ),
                                    ),
                                  ],
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                      const Divider(height: 32, color: Colors.white10),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          _buildStatusTag('Desktop', _helperIpAddress),
                          _buildStatusTag('Port', '8765'),
                        ],
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 20),

                // Auth PIN Card
                _buildSkeuoCard(
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Row(
                        children: [
                          Container(
                            width: 48,
                            height: 48,
                            decoration: BoxDecoration(
                              color: const Color(0xFF6366F1).withAlpha(40),
                              borderRadius: BorderRadius.circular(14),
                              border: Border.all(color: const Color(0xFF6366F1).withAlpha(80)),
                            ),
                            child: const Icon(Icons.password, color: Color(0xFF818CF8), size: 24),
                          ),
                          const SizedBox(width: 16),
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text(
                                'Pairing Auth PIN',
                                style: TextStyle(
                                  color: Colors.white,
                                  fontWeight: FontWeight.w700,
                                  fontSize: 16,
                                ),
                              ),
                              const SizedBox(height: 2),
                              Text(
                                'Enter on phone to connect',
                                style: TextStyle(color: Colors.grey.shade400, fontSize: 12),
                              ),
                            ],
                          ),
                        ],
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                        decoration: BoxDecoration(
                          color: const Color(0xFF1E293B),
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(color: Colors.white12),
                        ),
                        child: Text(
                          _helperSettings.authPin,
                          style: const TextStyle(
                            color: Color(0xFF5EEAD4),
                            fontWeight: FontWeight.w900,
                            fontSize: 20,
                            letterSpacing: 4,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 20),
                
                // File Transfer Card
                _buildSkeuoCard(
                  title: 'File Bridge',
                  icon: Icons.unarchive_outlined,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Fast local transfer to phone Downloads',
                        style: TextStyle(color: Colors.grey.shade400, fontSize: 13),
                      ),
                      const SizedBox(height: 20),
                      TextFormField(
                        controller: _phoneEndpointController,
                        onChanged: (v) => _phoneUploadEndpoint = v,
                        style: const TextStyle(color: Colors.white, fontSize: 13),
                        decoration: _buildInputDecoration('Phone Host', 'http://ip:8080'),
                      ),
                      const SizedBox(height: 12),
                      Row(
                        children: [
                          Expanded(
                            child: _buildActionButton(
                              onPressed: _pickFilesAndSendToPhone,
                              icon: Icons.file_present_rounded,
                              label: 'Pick Files',
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: _buildActionButton(
                              onPressed: _pickFolderAndSendToPhone,
                              icon: Icons.folder_zip_rounded,
                              label: 'Pick Folder',
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 16),
                      DropTarget(
                        onDragEntered: (_) => setState(() => _isDraggingFiles = true),
                        onDragExited: (_) => setState(() => _isDraggingFiles = false),
                        onDragDone: (detail) async {
                          await _ensureRemoteFileTransferEnabled();
                          setState(() => _isDraggingFiles = false);
                          final paths = detail.files.map((f) => f.path).toList();
                          await _sendFilesToPhone(paths);
                        },
                        child: AnimatedContainer(
                          duration: const Duration(milliseconds: 200),
                          padding: const EdgeInsets.symmetric(vertical: 32, horizontal: 16),
                          decoration: BoxDecoration(
                            color: _isDraggingFiles 
                                ? const Color(0xFF5EEAD4).withValues(alpha: 0.1)
                                : Colors.black.withValues(alpha: 0.2),
                            borderRadius: BorderRadius.circular(20),
                            border: Border.all(
                              color: _isDraggingFiles 
                                  ? const Color(0xFF5EEAD4)
                                  : Colors.white10,
                              width: 1.5,
                              style: BorderStyle.solid,
                            ),
                          ),
                          child: Column(
                            children: [
                              Icon(
                                Icons.cloud_upload_outlined,
                                color: _isDraggingFiles ? const Color(0xFF5EEAD4) : Colors.white38,
                                size: 36,
                              ),
                              const SizedBox(height: 12),
                              Text(
                                _isDraggingFiles ? 'Release to Share' : 'Drop fragments here to sync',
                                style: TextStyle(
                                  color: _isDraggingFiles ? const Color(0xFF5EEAD4) : Colors.white54,
                                  fontWeight: FontWeight.w600,
                                ),
                              ),
                              if (_phoneShareStatus.isNotEmpty) ...[
                                const SizedBox(height: 8),
                                Text(
                                  _phoneShareStatus,
                                  textAlign: TextAlign.center,
                                  style: TextStyle(color: Colors.grey.shade500, fontSize: 11),
                                ),
                              ],
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 20),

                // Clipboard & Features Card
                _buildSkeuoCard(
                  title: 'Core Services',
                  icon: Icons.settings_input_component_outlined,
                  child: Column(
                    children: [
                      _buildFeatureToggle(
                        'Clipboard Relay',
                        'Proactive bi-directional sync',
                        _helperSettings.clipboardSyncEnabled,
                        (v) => _updateFeatureSetting(_helperSettings.copyWith(clipboardSyncEnabled: v)),
                        Icons.content_paste_go_rounded,
                      ),
                      _buildFeatureToggle(
                        'Remote Shell',
                        'Execute safe terminal operations',
                        _helperSettings.remoteCommandEnabled,
                        (v) => _updateFeatureSetting(_helperSettings.copyWith(remoteCommandEnabled: v)),
                        Icons.terminal_rounded,
                      ),
                      _buildFeatureToggle(
                        'Browser Link',
                        'Accept URL handoffs from peer',
                        _helperSettings.browserHandoffEnabled,
                        (v) => _updateFeatureSetting(_helperSettings.copyWith(browserHandoffEnabled: v)),
                        Icons.open_in_new_rounded,
                      ),
                      const SizedBox(height: 16),
                      TextField(
                        controller: _homeUrlController,
                        style: const TextStyle(color: Colors.white, fontSize: 13),
                        decoration: _buildInputDecoration('Portal Homepage', 'google.com'),
                      ),
                      const SizedBox(height: 12),
                      _buildActionButton(
                        onPressed: _saveDefaultHomeUrl,
                        icon: Icons.save_alt_rounded,
                        label: 'Save Configuration',
                        isPrimary: true,
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 40),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSkeuoCard({String? title, IconData? icon, required Widget child}) {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: const Color(0xFF1E293B).withValues(alpha: 0.6),
        borderRadius: BorderRadius.circular(28),
        border: Border.all(color: Colors.white.withValues(alpha: 0.05)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.2),
            blurRadius: 20,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (title != null) ...[
            Row(
              children: [
                if (icon != null) ...[
                  Icon(icon, color: const Color(0xFF5EEAD4), size: 20),
                  const SizedBox(width: 10),
                ],
                Text(
                  title,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 16,
                    fontWeight: FontWeight.w800,
                    letterSpacing: 0.5,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
          ],
          child,
        ],
      ),
    );
  }

  Widget _buildStatusTag(String label, String value) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: Colors.black.withValues(alpha: 0.2),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            '$label: ',
            style: TextStyle(color: Colors.grey.shade500, fontSize: 11, fontWeight: FontWeight.bold),
          ),
          Text(
            value,
            style: const TextStyle(color: Colors.white70, fontSize: 11, fontWeight: FontWeight.w600),
          ),
        ],
      ),
    );
  }

  InputDecoration _buildInputDecoration(String label, String hint) {
    return InputDecoration(
      labelText: label,
      labelStyle: const TextStyle(color: Color(0xFF5EEAD4), fontSize: 12, fontWeight: FontWeight.w600),
      hintText: hint,
      hintStyle: TextStyle(color: Colors.grey.shade600, fontSize: 13),
      filled: true,
      fillColor: Colors.black.withValues(alpha: 0.2),
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: BorderSide.none,
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: const BorderSide(color: Color(0xFF5EEAD4), width: 1),
      ),
    );
  }

  Widget _buildActionButton({
    required VoidCallback onPressed,
    required IconData icon,
    required String label,
    bool isPrimary = false,
  }) {
    return SizedBox(
      height: 48,
      child: FilledButton.icon(
        onPressed: onPressed,
        style: FilledButton.styleFrom(
          backgroundColor: isPrimary ? const Color(0xFF5EEAD4) : Colors.white.withValues(alpha: 0.05),
          foregroundColor: isPrimary ? const Color(0xFF0F172A) : Colors.white,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          elevation: 0,
        ),
        icon: Icon(icon, size: 18),
        label: Text(label, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
      ),
    );
  }

  Widget _buildFeatureToggle(String title, String subtitle, bool value, Function(bool) onChanged, IconData icon) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: Colors.white.withValues(alpha: 0.05),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(icon, color: Colors.white70, size: 20),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: const TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.bold)),
                Text(subtitle, style: TextStyle(color: Colors.grey.shade500, fontSize: 11)),
              ],
            ),
          ),
          Switch.adaptive(
            value: value,
            onChanged: onChanged,
            activeTrackColor: const Color(0xFF5EEAD4),
          ),
        ],
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
      navigatorKey: _appNavigatorKey,
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
            _showHelperDashboard ? 'Hackie Dashboard' : 'Hackie Browser',
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
                if (_showHelperDashboard) {
                  setState(() {
                    _showHelperDashboard = false;
                  });
                  return;
                }
                setState(() {
                  _showHelperDashboard = true;
                });
              },
            ),
            if (_showHelperDashboard)
              IconButton(
                tooltip: 'Lock dashboard',
                icon: const Icon(Icons.lock_outline),
                onPressed: _lockDashboard,
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
