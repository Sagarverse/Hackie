import Cocoa
import FlutterMacOS

@main
class AppDelegate: FlutterAppDelegate {
  private var statusItem: NSStatusItem?

  override func applicationDidFinishLaunching(_ notification: Notification) {
    super.applicationDidFinishLaunching(notification)

    // Hide the default Flutter window to make this app tray-only on macOS.
    NSApp.windows.forEach { window in
      window.orderOut(nil)
    }

    let item = NSStatusBar.system.statusItem(withLength: NSStatusItem.variableLength)
    self.statusItem = item

    if let button = item.button {
      let attrs: [NSAttributedString.Key: Any] = [
        .font: NSFont.systemFont(ofSize: 13, weight: .bold),
        .foregroundColor: NSColor.systemBlue
      ]
      button.attributedTitle = NSAttributedString(string: "G", attributes: attrs)
      button.image = nil
      button.imagePosition = .noImage
      button.toolTip = "Google"
    }

    let menu = NSMenu()
    menu.addItem(
      NSMenuItem(
        title: "Quit",
        action: #selector(quitApp),
        keyEquivalent: "q"
      )
    )
    menu.items.last?.target = self
    item.menu = menu

    NSApp.setActivationPolicy(.accessory)
  }

  @objc private func quitApp() {
    NSApp.terminate(nil)
  }

  override func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool {
    return false
  }

  override func applicationSupportsSecureRestorableState(_ app: NSApplication) -> Bool {
    return true
  }
}
