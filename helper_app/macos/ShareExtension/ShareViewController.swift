import AppKit

final class ShareViewController: NSViewController {
  private let appGroupIdentifier = "group.com.sagar.hackie.share"
  private let callbackScheme = "hackiehelper"
  private let fileUrlTypeIdentifier = "public.file-url"
  private let urlTypeIdentifier = "public.url"

  override func loadView() {
    view = NSView(frame: .zero)
  }

  override func beginRequest(with context: NSExtensionContext) {
    Task {
      await handleRequest(context)
    }
  }

  private func handleRequest(_ context: NSExtensionContext) async {
    let providers = extractItemProviders(from: context.inputItems)
    let incomingUrls = await loadIncomingUrls(from: providers)
    let copiedPaths = copyFilesToSharedInbox(incomingUrls)

    if !copiedPaths.isEmpty {
      _ = writeManifestAndNotifyApp(filePaths: copiedPaths)
    }

    context.completeRequest(returningItems: nil, completionHandler: nil)
  }

  private func extractItemProviders(from inputItems: [Any]) -> [NSItemProvider] {
    var providers: [NSItemProvider] = []
    for item in inputItems {
      guard let extensionItem = item as? NSExtensionItem else { continue }
      providers.append(contentsOf: extensionItem.attachments ?? [])
    }
    return providers
  }

  private func loadIncomingUrls(from providers: [NSItemProvider]) async -> [URL] {
    var urls: [URL] = []

    for provider in providers {
      if provider.hasItemConformingToTypeIdentifier(fileUrlTypeIdentifier),
        let fileUrl = await loadUrl(from: provider, typeIdentifier: fileUrlTypeIdentifier) {
        urls.append(fileUrl)
        continue
      }

      if provider.hasItemConformingToTypeIdentifier(urlTypeIdentifier),
        let url = await loadUrl(from: provider, typeIdentifier: urlTypeIdentifier) {
        urls.append(url)
      }
    }

    return urls
  }

  private func loadUrl(from provider: NSItemProvider, typeIdentifier: String) async -> URL? {
    await withCheckedContinuation { continuation in
      provider.loadItem(forTypeIdentifier: typeIdentifier, options: nil) { item, _ in
        if let url = item as? URL {
          continuation.resume(returning: url)
          return
        }

        if let nsUrl = item as? NSURL, let casted = nsUrl as URL? {
          continuation.resume(returning: casted)
          return
        }

        if let data = item as? Data,
           let text = String(data: data, encoding: .utf8),
           let decodedUrl = URL(string: text) {
          continuation.resume(returning: decodedUrl)
          return
        }

        if let text = item as? String, let decodedUrl = URL(string: text) {
          continuation.resume(returning: decodedUrl)
          return
        }

        continuation.resume(returning: nil)
      }
    }
  }

  private func copyFilesToSharedInbox(_ urls: [URL]) -> [String] {
    guard let containerUrl = FileManager.default.containerURL(
      forSecurityApplicationGroupIdentifier: appGroupIdentifier
    ) else {
      return []
    }

    let inboxUrl = containerUrl.appendingPathComponent("SharedInbox", isDirectory: true)
    do {
      try FileManager.default.createDirectory(at: inboxUrl, withIntermediateDirectories: true)
    } catch {
      return []
    }

    var copiedPaths: [String] = []
    for sourceUrl in urls {
      if sourceUrl.isFileURL {
        let destinationUrl = uniqueDestination(in: inboxUrl, suggestedName: sourceUrl.lastPathComponent)
        do {
          try FileManager.default.copyItem(at: sourceUrl, to: destinationUrl)
          copiedPaths.append(destinationUrl.path)
        } catch {
          continue
        }
        continue
      }

      // Persist shared web links as .webloc files so the existing file relay
      // pipeline can forward them without introducing a new transport shape.
      let host = sourceUrl.host?.trimmingCharacters(in: .whitespacesAndNewlines)
      let suggestedName = (host?.isEmpty == false ? host! : "shared-link") + ".webloc"
      let destinationUrl = uniqueDestination(in: inboxUrl, suggestedName: suggestedName)
      do {
        try writeWebloc(url: sourceUrl, destination: destinationUrl)
        copiedPaths.append(destinationUrl.path)
      } catch {
        continue
      }
    }

    return copiedPaths
  }

  private func writeWebloc(url: URL, destination: URL) throws {
    let payload: [String: Any] = ["URL": url.absoluteString]
    let data = try PropertyListSerialization.data(
      fromPropertyList: payload,
      format: .xml,
      options: 0
    )
    try data.write(to: destination, options: .atomic)
  }

  private func uniqueDestination(in folder: URL, suggestedName: String) -> URL {
    var candidate = folder.appendingPathComponent(suggestedName)
    if !FileManager.default.fileExists(atPath: candidate.path) {
      return candidate
    }

    let ext = (suggestedName as NSString).pathExtension
    let base = (suggestedName as NSString).deletingPathExtension
    var index = 1

    while FileManager.default.fileExists(atPath: candidate.path) {
      let suffix = "-\(index)"
      let rebuilt = ext.isEmpty ? "\(base)\(suffix)" : "\(base)\(suffix).\(ext)"
      candidate = folder.appendingPathComponent(rebuilt)
      index += 1
    }

    return candidate
  }

  private func writeManifestAndNotifyApp(filePaths: [String]) -> Bool {
    guard let containerUrl = FileManager.default.containerURL(
      forSecurityApplicationGroupIdentifier: appGroupIdentifier
    ) else {
      return false
    }

    let inboxUrl = containerUrl.appendingPathComponent("SharedInbox", isDirectory: true)
    let formatter = ISO8601DateFormatter()
    let timestamp = formatter.string(from: Date())
    let token = UUID().uuidString

    let manifest: [String: Any] = [
      "createdAt": timestamp,
      "files": filePaths,
    ]

    let manifestName = "manifest-\(token).json"
    let manifestUrl = inboxUrl.appendingPathComponent(manifestName)

    do {
      let payload = try JSONSerialization.data(withJSONObject: manifest, options: [.prettyPrinted])
      try payload.write(to: manifestUrl, options: .atomic)
    } catch {
      return false
    }

    guard let encodedName = manifestName.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
          let callbackUrl = URL(string: "\(callbackScheme)://import-shared-files?manifest=\(encodedName)") else {
      return false
    }

    NSWorkspace.shared.open(callbackUrl)
    return true
  }
}
