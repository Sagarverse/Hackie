import Cocoa
import FlutterMacOS

@main
class AppDelegate: FlutterAppDelegate {
  private let shareAppGroupId = "group.com.sagar.hackie.share"
  private let shareInboxFolder = "SharedInbox"
  private var shareChannel: FlutterMethodChannel?
  private var pendingOpenPaths: [String] = []

  override func applicationDidFinishLaunching(_ notification: Notification) {
    super.applicationDidFinishLaunching(notification)
    NSApp.setActivationPolicy(.accessory)

    if let flutterViewController = mainFlutterWindow?.contentViewController as? FlutterViewController {
      let channel = FlutterMethodChannel(
        name: "hackie/share",
        binaryMessenger: flutterViewController.engine.binaryMessenger
      )
      shareChannel = channel
      pendingOpenPaths.append(contentsOf: consumeSharedInboxManifest(named: nil))
      flushPendingOpenPathsIfNeeded()
    }
  }

  override func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool {
    return false
  }

  override func applicationSupportsSecureRestorableState(_ app: NSApplication) -> Bool {
    return true
  }

  override func application(_ sender: NSApplication, openFiles filenames: [String]) {
    guard !filenames.isEmpty else {
      sender.reply(toOpenOrPrint: .success)
      return
    }

    pendingOpenPaths.append(contentsOf: filenames)
    flushPendingOpenPathsIfNeeded()
    sender.reply(toOpenOrPrint: .success)
  }

  override func application(_ application: NSApplication, open urls: [URL]) {
    for url in urls {
      guard let scheme = url.scheme?.lowercased(), scheme == "hackiehelper" else {
        continue
      }

      let importedPaths = consumeSharedInboxManifest(named: manifestName(from: url))
      if !importedPaths.isEmpty {
        pendingOpenPaths.append(contentsOf: importedPaths)
      }
    }

    flushPendingOpenPathsIfNeeded()
  }

  private func flushPendingOpenPathsIfNeeded() {
    guard let channel = shareChannel, !pendingOpenPaths.isEmpty else { return }
    let payload = pendingOpenPaths
    pendingOpenPaths.removeAll()
    channel.invokeMethod("openFiles", arguments: payload)
  }

  private func manifestName(from url: URL) -> String? {
    guard let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
          let queryItems = components.queryItems else {
      return nil
    }

    return queryItems.first(where: { $0.name == "manifest" })?.value
  }

  private func consumeSharedInboxManifest(named manifestName: String?) -> [String] {
    guard let groupUrl = FileManager.default.containerURL(
      forSecurityApplicationGroupIdentifier: shareAppGroupId
    ) else {
      return []
    }

    let inboxUrl = groupUrl.appendingPathComponent(shareInboxFolder, isDirectory: true)
    guard let entries = try? FileManager.default.contentsOfDirectory(
      at: inboxUrl,
      includingPropertiesForKeys: [.contentModificationDateKey],
      options: [.skipsHiddenFiles]
    ) else {
      return []
    }

    let manifestUrls: [URL]
    if let explicit = manifestName, !explicit.isEmpty {
      manifestUrls = [inboxUrl.appendingPathComponent(explicit)]
    } else {
      manifestUrls = entries
        .filter { $0.lastPathComponent.hasPrefix("manifest-") && $0.pathExtension == "json" }
        .sorted { lhs, rhs in
          let leftDate = (try? lhs.resourceValues(forKeys: [.contentModificationDateKey]))?.contentModificationDate ?? .distantPast
          let rightDate = (try? rhs.resourceValues(forKeys: [.contentModificationDateKey]))?.contentModificationDate ?? .distantPast
          return leftDate > rightDate
        }
    }

    var resolvedPaths: [String] = []

    for manifestUrl in manifestUrls {
      guard let data = try? Data(contentsOf: manifestUrl),
            let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
            let files = object["files"] as? [String] else {
        continue
      }

      resolvedPaths.append(contentsOf: files.filter { FileManager.default.fileExists(atPath: $0) })
      try? FileManager.default.removeItem(at: manifestUrl)
    }

    return resolvedPaths
  }
}
