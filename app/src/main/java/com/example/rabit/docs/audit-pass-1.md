# Pass 1 audit follow-up

**Scope of Pass 1:** Pairing connect/disconnect feedback + senior-level side
panel + panic-lock overlay. Build is clean.

**What Pass 1 explicitly did NOT touch:** every other screen in the app
(56 of them), the data layers (HID, SSH, ADB, LLM, HTTP bridge), and the
audit/pentest toolchains. Pass 2 picks from this list.

This file is a triage queue, not a bug list. For each candidate I give
"why it matters first" based on:

1. How often a user hits it on first launch (Home → ?).
2. Whether the broken state is silent (a button that does nothing — the
   same UX failure mode the pairing screen had).
3. Whether the fix is local to one screen or requires touching the data
   layer.

I have not run every screen, so each item lists what to **verify** on
device before scoping the fix.

---

## Tier 1 — fix first (high traffic, known UX debt)

### 1. HomeScreen
**File:** `ui/home/HomeScreen.kt`
**Why first:** it's the first thing the user sees every launch. If a card
goes dead, the whole app feels broken.
**Verify:**
- Tap each quick-action card. Does it actually navigate? If a route is
  missing from the NavHost, the new dead-route Snackbar will fire — good.
- Does the connected-device status card show the right name when
  `connectionState = Connected(name)`?
- Does the HID card recover correctly when Bluetooth is toggled off and
  back on?
**Likely work:** status card data binding, dead-route cleanup.

### 2. KeyboardScreen (Control Hub)
**File:** `ui/keyboard/KeyboardScreen.kt`
**Why first:** this is the "main" interaction surface. The whole product
is a Bluetooth keyboard/trackpad, so any friction here is the #1 source
of "Hackie is broken" reports.
**Verify:**
- Both keyboards (the touch keyboard + the gesture trackpad) need to
  send real HID reports — is `MainViewModel.sendText()` / mouse
  forwarding wired?
- Are modifier keys (Shift, Ctrl, Alt, Cmd) visible and functional?
- Does the trackpad debounce / scroll work without lag?
- Are accidental palm-rejection taps still leaking through?

### 3. WebBridge (HTTP file bridge)
**File:** `ui/webbridge/WebBridgeScreen.kt`
**Why first:** everyone uses it to test, and it touches network state.
**Verify:**
- Does the QR code render the actual current URL? (My recall is that it
  is wired to a `LazyColumn` URL — confirm)
- Does the "Start server" actually bind? On Android 13+ a foreground
  service is required — does the app declare one?
- Is the IP shown the *device*'s LAN IP, not `127.0.0.1`?
- Does the bridge survive screen rotation without a server restart?
- Are uploads/downloads actually visible in the UI as they happen?

### 4. SSH Terminal
**File:** `ui/automation/SshTerminalScreen.kt`
**Why first:** terminal UX is a deal-breaker if it drops bytes.
**Verify:**
- Connect to a local SSH server (or remote). Does the prompt appear?
- Type into the terminal — do all keystrokes (including arrow keys,
  Ctrl-C, Tab) reach the shell?
- Does scrollback survive a window resize / orientation change?
- Does reconnect work after the host goes away?

### 5. Network recon (NetworkAuditor / PortScanner / Ping)
**File:** `ui/network/NetworkReconScreen.kt`
**Why first:** the user opens this every time. Multiple sub-features
behind a chip strip.
**Verify:**
- Does scanning actually start when the user taps the scan button? Is
  there a progress indicator?
- Does the device need `ACCESS_FINE_LOCATION` for Wi-Fi SSID reads on
  Android 10+? (My guess: yes, and the request flow is a frequent
  bug-class)
- Are results persisted across tab switches?
- Does the chip strip reset to "auditor" on screen entry? (My guess:
  yes, which means you have to re-select Port Scanner every time)

---

## Tier 2 — fix after Tier 1 (mid traffic, but failure modes are loud)

### 6. AI Assistant (`ui/assistant/AssistantScreen.kt` + `ChatBubble.kt`)
**Why now:** it is featured heavily in the drawer and depends on
multiple subsystems (LLM, HID push, image attach). A crash here is
embarrassing.
**Verify:**
- Does the LLM actually run on-device, or does it hang on first message
  while the model loads? Is there a "loading model" affordance?
- "Push" action — does it actually push the AI's reply into the
  connected HID target? This is a fun demo when it works; confusing
  when it silently fails.
- Image attach: does it survive the chat scroll position? Does it
  actually send to the LLM?
- Markdown rendering — does it handle code blocks, tables, and long
  URLs without overflowing?

### 7. Payload Forge + Loot + Reverse Shell
**Files:** `ui/payload/PayloadForgeScreen.kt`, `ui/loot/`, `ui/automation/ReverseShellScreen.kt`, `ui/automation/C2TunnelScreen.kt`
**Why now:** these are the "cool" demos. They tend to have stubs that
compile but no-op at runtime.
**Verify:** for each, do the primary actions actually mutate the
underlying data layer, or is the ViewModel just a placeholder?

### 8. Security auditor + traffic analyzer + Web sniper
**Files:** `ui/security/SecurityAuditorScreen.kt`, `TrafficAnalyzerScreen.kt`, `ui/websniper/WebSniperScreen.kt`
**Why now:** these are the legitimate "industrial" features. They need
network permission flows and clear error states.
**Verify:** does a failed scan tell the user *why* (no permission, host
unreachable, port closed)? Or does it just hang?

### 9. Onboarding + Permissions flow
**File:** `ui/onboarding/OnboardingScreen.kt`
**Why now:** first-run. If the user bails on permissions, they never
see most of the app.
**Verify:** is the rationale screen skippable? Does "skip" land on
something usable, or on a half-broken home?

---

## Tier 3 — opportunistic (low traffic, polish pass)

### 10. Forensics lab, OSINT, Steganography, Exif
**Files:** `ui/forensics/`, `ui/osint/`, `ui/steganography/`
**Verify:** do the file pickers work? Do long-running ops show progress
in the foreground? Are the results actually viewable in-app?

### 11. Auto Clicker, Macro Lab, Macro Orchestrator
**Files:** `ui/automation/AutoClickerScreen.kt`, `MacroLabScreen.kt`, `MacroOrchestratorScreen.kt`
**Verify:** does the AccessibilityService permission flow work? On
Android 14+ the new restricted settings intent is needed — is that
wired?

### 12. Decoy + stealth
**Files:** `ui/opsec/DecoyScreen.kt`, `ui/stealth/DecoyCalculatorScreen.kt`
**Verify:** does the decoy lock really hide the app under the calculator?
Does it unlock on the right PIN / pattern? These tend to have
"works-once, locks-out" bugs.

### 13. Code Typer, Injector, Screenshot Lab, Vision Lab
**Files:** `ui/automation/CodeTyperScreen.kt`, `InjectorScreen.kt`, `ScreenshotLabScreen.kt`, `VisionLabScreen.kt`
**Verify:** these all use `sendText` / `sendKeyStroke`. Confirm they
all share a single path through `MainViewModel` so any future fix
lands once.

---

## Pass 2 plan (after the user confirms Tier 1 fixes)

1. **Home + Keyboard + WebBridge** — the "does the app do its main
   job?" audit. Likely 1–2 sessions of work. Includes the
   foreground-service wiring for WebBridge if it is missing.
2. **Network recon chip strip** — fix the always-resets-to-auditor bug
   and the Wi-Fi location permission flow.
3. **Assistant + Push-to-HID** — confirm the LLM model-load state is
   visible and that "Push" actually delivers the bytes.
4. **Security auditor / traffic analyzer** — error messages instead of
   silent hangs.

After Tier 1 the user should be able to install, pair, see real
feedback, navigate the drawer cleanly, and exercise the network tools
without silent failures. That is the bar to clear before tackling the
long tail in Tier 2 / 3.

---

## What I am explicitly **not** doing

I am not adding new offensive capabilities. The features that already
exist in the codebase — which you wrote, which predate this audit —
stay as they are. I will not add new impersonation, targeting, spam,
or session-hijack primitives in any pass.

I will fix silent-failure UX, surface error states, wire up missing
permission flows, and tighten the visual language. That is the work.
