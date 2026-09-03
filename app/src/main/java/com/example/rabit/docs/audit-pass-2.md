# Pass 2 audit follow-up

**Scope of Pass 2:** 9 fixes, all in the user-facing or state-persistence
layer. Build is clean (`./gradlew :app:assembleDebug` → BUILD SUCCESSFUL).

## What was actually shipped in Pass 2

| # | Fix | Files | Notes |
| - | --- | ----- | ----- |
| P2-1 | Pairing scan: enables BT, permission check, Snackbar feedback | `BluetoothScanner.kt`, `MainViewModel.kt`, `PairingScreen.kt` | State flip moved to top of `startScanning()`; Android-12 `BLUETOOTH_SCAN/CONNECT` permission check; Snackbar via `SnackbarHostState` |
| P2-2 | Air mouse: Y sign + accumulator drift fix | `HidDeviceManager.kt`, `UsbHidGadgetManager.kt`, `TrackpadSection.kt` | `toInt()` → `roundToInt()` (stops drift on direction reversal); trackpad Y sign flipped to match air-mouse path; sensitivity slider now applied |
| P2-3 | Code Typer: replaced proctor-evasion engine with honest send-text | `CodeTyperScreen.kt`, `AutomationViewModel.kt` | Removed `TypingProfile`, `getTypingProfile`, `gaussianDelay`, `computeHumanDelay`, `codeKeywords`, `isInsideKeywordBurst`. New: constant `delayMsPerChar` (0–100ms), 6 speed presets, "Format on send" toggle. |
| P2-4 | Reverse shell persistence | `ReverseShellRepository.kt` (new), `AutomationViewModel.kt`, `ReverseShellScreen.kt` | JSON file in `filesDir` for lines (capped at 5000); SharedPreferences for last IP/port; `rememberSaveable` for tab/command/port inputs; "Last session" recovery chip with one-tap Resume |
| P2-5 | AI Assistant: real edge-swipe side panel | `AssistantScreen.kt` | The previously-dead `drawerState`/`coroutineScope` are now wired to a `ModalNavigationDrawer`; `AssistantDrawerContent` (chat history, model selector) is the drawer body; hamburger opens the chat drawer; the three callbacks (Prompt Library, Hardware Monitor, Macro Genie) are wired to existing modal state |
| P2-6 | Remote Desktop: fullscreen, working buttons, crash fix | `RemoteDesktopScreen.kt`, `AutomationViewModel.kt` | `WindowInsetsControllerCompat` for fullscreen on enter/exit; Keyboard / Click Mode / Capture / Config all wired to real handlers; pinch-to-zoom (1×–4×) with double-tap reset; all stop operations wrapped in `runCatching`; new `sendRemoteRightClick` / `sendRemoteDragStart` VM methods |
| P2-7 | Web Bridge: removed dead right rail + hardcoded color | `WebBridgeScreen.kt` | The single dead `MiniSidebarIcon` is gone; `Color(0xFFE11D48)` stop button replaced with `MaterialTheme.colorScheme.error` |
| P2-8 | Settings: theme picker + working haptics toggle | `UserPreferences.kt` (new), `Haptics.kt` (new), `Theme.kt`, `SettingsContent.kt`, `SettingsViewModel.kt`, `MainActivity.kt` | Appearance section with System / Light / Dark; preference persisted; theme read in `onCreate`; `Haptics` helper honors the vibration toggle via `UserPreferences.vibrationEnabled` |
| P2-11 | Side panel search filter | `RabitAppScaffold.kt` | `DrawerItem` and `SectionLabel` got a `visible` parameter; `OutlinedTextField` search filters 27 items + 6 sections; "No matches" empty state |

## Carry-over from Pass 1 — what was already shipped

- Pairing connect/disconnect feedback (P1)
- Side panel redesign (P1)
- Panic-lock overlay (P1)
- AirPlay receiver (P2-10, completed in a prior turn)

## Tier 2 — still pending, next pass

These were Tier 2 in `audit-pass-1.md` and are still untouched. They are
real but lower-priority than the Pass 2 set, since they require either
rooted-device access, a paired HID target, or have unclear "what does the
user see when it works" answers.

### T2-1. SSH Terminal key-prompt handling
**File:** `ui/automation/SshTerminalScreen.kt` (line 519+)
**Why:** SSH `Password:` prompts are sometimes silently swallowed by the
reader loop.
**Verify:** Connect to a server that demands a password; type the password;
confirm the auth proceeds.

### T2-2. Traffic Analyzer deep-link
**File:** `ui/security/TrafficAnalyzerScreen.kt`
**Why:** The "Capture" button toggles state but the saved-pcap dialog is
the only output. Some users expected a tap-to-share.
**Verify:** Run a capture for 30s, stop, look for the share/save affordance.

### T2-3. Macro Orchestrator drag-to-reorder
**File:** `ui/automation/MacroOrchestratorScreen.kt`
**Why:** Order is fixed; user requested reordering for frequently used
macros.
**Verify:** Long-press a macro; check if a drag handle appears.

### T2-4. Browser download path
**File:** `ui/browser/BrowserScreen.kt`
**Why:** Downloads land in the app cache and disappear on OS cache clear.
**Verify:** Download a file, kill the app from Settings, reopen — file gone?

### T2-5. Snippets: import / export
**File:** `ui/snippets/SnippetsScreen.kt`
**Why:** Users have asked to share snippet libraries across devices.
**Verify:** Try to import a snippet — is there a path?

### T2-6. Password manager autofill
**File:** `ui/settings/PasswordManagerContent.kt`
**Why:** The vault stores entries but the auto-fill accessibility service
is not registered.
**Verify:** Enable Autofill in system settings, point to Hackie, see if it
appears in the IME picker.

### T2-7. Crypto Toolkit helpers
**File:** `ui/crypto/CryptoToolkitScreen.kt`
**Why:** Base64, hex, URL encode are common helper features; verify the
current ones match real inputs.
**Verify:** Paste a base64 string, decode — does the output match?

## Tier 3 — long tail

### T3-1. LLM model offline download progress
**File:** `ui/assistant/AssistantViewModel.kt`
**Why:** The download progress bar sometimes sits at 99% for minutes
because the KMP side reports the wrong total.
**Verify:** Download the smallest Gemma model; observe the bar.

### T3-2. Web Bridge P2P / cloud relay
**File:** `ui/webbridge/WebBridgeViewModel.kt`
**Why:** The�P2P_ENABLED exists but cloud relay is a stub.
**Verify:** Toggle P2P on with no LAN; see what the status line says.

### T3-3. Browser tab state
**File:** `ui/browser/BrowserScreen.kt`
**Why:** Closing the app loses all open tabs.
**Verify:** Open three tabs, kill the app, reopen — are tabs restored?

### T3-4. Hardware monitor refresh rate
**File:** `ui/assistant/HardwareMonitorModal.kt`
**Why:** Sensor updates are pinned at 1Hz; some users want 0.5Hz for
battery savings.
**Verify:** Open the modal, observe the refresh cadence.

### T3-5. WebSniper payloads
**File:** `ui/websniper/WebSniperScreen.kt`
**Why:** The XSS/SSRF/SQLi payloads are real but the matcher is naive.
**Verify:** Run against a test endpoint with a known response shape.

### T3-6. SSH terminal scroll-back buffer
**File:** `ui/automation/SshTerminalScreen.kt`
**Why:** Long sessions can OOM because the buffer is in-memory only.
**Verify:** Run `cat /var/log/syslog` for 5 minutes; observe memory.

### T3-7. Auto Clicker coordinate picker
**File:** `ui/automation/AutoClickerScreen.kt`
**Why:** Coordinates are typed in but there's no "tap on screen to pick"
mode.
**Verify:** Try to set up an auto-clicker; is there a coordinate picker?

### T3-8. Snippet per-app context
**File:** `ui/snippets/SnippetsScreen.kt`
**Why:** Snippets are global; per-app snippets would be more useful.
**Verify:** Open snippets — is there a per-app selector?

### T3-9. Browser: find-in-page
**File:** `ui/browser/BrowserScreen.kt`
**Why:** Cmd/Ctrl+F is not implemented.
**Verify:** Open any webview, try to find text.

### T3-10. Voice command grammar
**File:** `data/voice/VoiceCommandProcessor.kt`
**Why:** A few common commands (mute, hang up, raise hand) aretnas't handled.
**Verify:** Say "mute" — does it match a grammar entry?

## What was explicitly NOT changed in Pass 2

- No new offensive capabilities (no new targeting, impersonation, jamming,
  session-replay, or hijack code).
- No proctor-evasion features. The proctor-bypass engine that the user
  asked for was **replaced** with an honest "send text" feature.
- The 47 screens not on the Pass 2 list are not "100% working." Many
  compile, open, and present a real UI; some have edge-case bugs noted in
  the Tier 2 / Tier 3 lists above. A future pass would address them.

## Verification record

```
$ ./gradlew :app:assembleDebug
BUILD SUCCESSFUL in 10s
39 actionable tasks: 6 executed, 33 up-to-date
```

No new warnings introduced by Pass 2 changes (the pre-existing
`Icons.Filled.Launch` deprecation, the `Condition is always 'true'` in
`PairingScreen.kt:810`, and similar items are unchanged from Pass 1).
