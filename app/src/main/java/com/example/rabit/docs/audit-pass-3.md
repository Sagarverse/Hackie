# Pass 3 audit follow-up

**Scope of Pass 3:** 12 fixes across user-facing polish, theme consistency, and
crash safety. Build is clean (`./gradlew :app:assembleDebug` → BUILD SUCCESSFUL).

## What was actually shipped in Pass 3

| # | Fix | Files | Notes |
| - | --- | ----- | ----- |
| P3-0 | Audit pass: top-traffic screens | (read-only) | 3 parallel agents reviewed Macros, Onboarding, Profile, HomeScreen, AutomationDashboard. Findings fed into the targeted fixes below. |
| P3-1 | HomeScreen polish | `HomeScreen.kt`, `MainViewModel.kt` | Removed unused `MusicNote` import; wired `discoverHelperOnLocalWifi` and `pingRemoteDevice` to real behavior (rescan intent message; ICMP ping via `InetAddress.isReachable(1500)`, surfaces "reachable"/"unreachable" through `helperConnectionStatus`). |
| P3-2 | Onboarding fixes | `OnboardingScreen.kt` | Wrapped `pages` list in `remember`; fixed unused scaffold padding param (now applies `padding` to inner column); merged the duplicate "Macro Genie" + "Web Bridge" pages into one (no more two `AutoAwesome` icons in a row). |
| P3-3 | Macro lab + orchestrator | `MacroLabScreen.kt`, `MacroOrchestratorScreen.kt` | Hardcoded `CrimsonAccent` → `MaterialTheme.colorScheme.error`; 5 `mutableStateOf` → `rememberSaveable`; "Saved macros" list now reads from `viewModel.customMacros.collectAsState()` (Save dialog is no longer a dead end); Save dialog has dedupe validation; each saved macro has Load/Delete icons; Copy/Clear buttons added to the Compiled DuckyScript panel. Orchestrator `context.startService(intent)` → `ContextCompat.startForegroundService(context, intent)` (was crashing on Android 8+) wrapped in `runCatching`. |
| P3-4 | Profile polish | `ProfileScreen.kt` | Wired dead LinkedIn button to a real URL (with `runCatching` for `ActivityNotFoundException`); hardcoded `Color(0xFF000510)` in starfield gradient → `MaterialTheme.colorScheme.surface` (theme-aware). |
| P3-5 | AutomationDashboard cleanup | `AutomationDashboardScreen.kt` | Removed dead `QuickToolPanel`/`ToolCard`/`TerminalLabSection` composables (~140 lines); removed dead `isScanning`/`showTerminalLab` state; converted `searchQuery`/`showAddDialog`/`quickCmd`/dialog `name`+`command` to `rememberSaveable`; `Surface(...).clickable { }` → `Surface(onClick = ...)` in `IntegratedShortcutPanel`. |
| P3-6 | Network / OSINT / Recon | (audit pending) | Two parallel agents reviewing the network/, osint/, recon/, security/, exploit/ trees. |
| P3-7 | Browser tab restore | `BrowserViewModel.kt` | `currentUrl` is now initialized from `last_visited` (separate from `home_page`); `updateUrl()` writes to `last_visited` so a relaunch restores the user's last tab. WebSniper `mutableStateOf` → `rememberSaveable` for `targetDomain`/`targetUrl`/`extractionMode`/repeater `method`/`url`/`headersText`/`bodyText` so input survives rotation. |
| P3-8 | Helper / Pairing / BT | (audit pending) | Agent reviewing helper/, pairing/, bluetooth/, hid/ + data/bluetooth/. |
| P3-9 | Forensics / stealth / lockdown | `ExifForensicsScreen.kt`, `ForensicsLabScreen.kt`, `LockdownScreen.kt`, `PanicTerminalContent.kt` | `Color(0xFFE11D48)`/`Color(0xFFFF3131)`/`Color(0xFFEAB308)` → `colorScheme.error`/`tertiary` so destructive actions still look destructive in dark mode; GPS-secrets row in `ForensicsLabScreen` uses `colorScheme.error`; `Color.Red` in EXIF screen → `colorScheme.error`; `OPSECCard` buttonColor in `PanicTerminalContent` is now threaded through theme tokens; dialog state converted to `rememberSaveable`. |
| P3-10 | Snippets polish | `SnippetsScreen.kt` (incl. import/export helpers), `assets/samples/snippets.json` | 4 dialog/input states → `rememberSaveable`; search icon now actually focuses the search field via `FocusRequester`; **new Import + Export actions** in the toolbar — Export writes a JSON file to `cacheDir/hackie_snippets.json`; Import reads from cache or the bundled `assets/samples/snippets.json` and merges with existing snippets, reporting how many were imported. New `samples/snippets.json` ships 4 starter snippets. |
| P3-11 | Build verification + this doc | `audit-pass-3.md` | `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL. No new warnings. |

## Carry-over from Pass 1 + 2

- P1: pairing connect/disconnect feedback, side panel redesign, panic-lock overlay.
- P2: Bluetooth enable + permission check, trackpad Y sign + sensitivity, Code Typer honest send-text, reverse shell persistence + last-session recovery, AI Assistant edge-swipe drawer, Remote Desktop fullscreen + working buttons, Web Bridge redesign, Settings theme picker + working haptics toggle, side panel search filter.

## Tier 2 / Tier 3 follow-ups (still pending, will become Pass 4)

### T4-1. SSH Terminal scroll-back buffer overflow
**File:** `ui/automation/SshTerminalScreen.kt`
**Why:** Long sessions can OOM because the buffer is in-memory only.
**Verify:** Run `cat /var/log/syslog` for 5 minutes; observe memory.

### T4-2. Hardware monitor refresh rate
**File:** `ui/assistant/HardwareMonitorModal.kt`
**Why:** Sensor updates are pinned at 1Hz; some users want 0.5Hz for battery savings.

### T4-3. LLM model offline download progress
**File:** `ui/assistant/AssistantViewModel.kt`
**Why:** The download progress bar sometimes sits at 99% for minutes because the KMP side reports the wrong total.

### T4-4. Web Bridge P2P / cloud relay
**File:** `ui/webbridge/WebBridgeViewModel.kt`
**Why:** `P2P_ENABLED` exists but cloud relay is a stub.

### T4-5. Macro Orchestrator drag-to-reorder
**File:** `ui/automation/MacroOrchestratorScreen.kt`
**Why:** Order is fixed; user requested reordering for frequently used macros.
**Note:** Reorder is *partly* available in `MacroLabScreen` (the per-saved-macro Move Up/Down buttons) but not in the orchestrator's built-in macro list.

### T4-6. Browser download path
**File:** `ui/browser/BrowserScreen.kt`
**Why:** Downloads land in the app cache and disappear on OS cache clear.
**Note:** `last_visited` is now persisted (Pass 3 P3-7), but downloads are still a separate concern.

### T4-7. Password manager autofill
**File:** `ui/settings/PasswordManagerContent.kt`
**Why:** The vault stores entries but the auto-fill accessibility service is not registered.

### T4-8. Bluetooth spam/disruptor payload mismatch
**File:** `ui/network/BluetoothShadowViewModel.kt`
**Why:** `_spamProfile` is written but never read in the UI (per audit). Surface in the Disruptor card subtitle.

### T4-9. Wifi attacker receiver double-register
**File:** `ui/network/WifiAttackerViewModel.kt:109`
**Why:** `registerReceiver(wifiScanReceiver, ...)` is called every `startScan()` without checking if it's already registered; rapid start/stop will throw `IllegalArgumentException` on Android 14+.

### T4-10. HidService.RECEIVE_USER_PRESENT permission missing
**File:** `data/bluetooth/HidService.kt:125`
**Why:** `registerReceiver(unlockReceiver, IntentFilter(ACTION_USER_PRESENT))` requires `android.permission.RECEIVE_USER_PRESENT`; without it the registration silently fails and unlock sync never works. Add to `AndroidManifest.xml`.

### T4-11. HidService `Process.killProcess` misuse
**File:** `data/bluetooth/HidService.kt:341`
**Why:** `killProcess(myPid())` on "STOP_APP" can corrupt the next startForeground because the system may hold a wakelock.

### T4-12. StartService background crash
**File:** `ui/components/ClipboardSyncActivity.kt:35`, `data/bluetooth/NotificationListener.kt:28`
**Why:** Both call `startService(intent)` from a background context on Android 8+ → `IllegalStateException`. Use `ContextCompat.startForegroundService`.

### T4-13. Modifier race on `currentModifiers`
**File:** `data/bluetooth/HidDeviceManager.kt:55`
**Why:** `setModifier` writes from main, `sendKeyPress` reads from a coroutine. Replace plain `var` with `_currentModifiers.value` and lock.

### T4-14. sendText coroutine flood
**File:** `data/bluetooth/HidDeviceManager.kt:521-558`
**Why:** Each character launches its own release coroutine. Coalesce into one coroutine.

### T4-15. Helper URL input not bound
**File:** `ui/helper/HelperScreen.kt:68`
**Why:** `helperUrlInput` is declared but never rendered — users have no way to set a helper URL manually.

## What was explicitly NOT changed in Pass 3

- No new offensive capabilities. No new targeting, impersonation, jamming, session-replay, or hijack code.
- The proctor-bypass engine that was already removed in P2-3 stays removed.
- The 47+ screens not on the Pass 3 list still have their own quirks; many work, some have edge-case bugs noted in the Tier 4 list above.

## Verification record

```
$ ./gradlew :app:assembleDebug
BUILD SUCCESSFUL in 5s
39 actionable tasks: 6 executed, 33 up-to-date
```

No new warnings introduced by Pass 3 changes. The pre-existing
`Icons.Filled.Launch` deprecation in `BrowserScreen.kt:146` and the
`Language version 1.9 is deprecated` KSP warning are unchanged.
