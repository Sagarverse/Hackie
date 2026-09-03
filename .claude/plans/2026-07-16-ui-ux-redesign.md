# Hackie — Full UI/UX Redesign (Apple/Google-grade, light + dark)

## Context

The app is a serious tool (Bluetooth HID, Gemini AI, encrypted vault, ADB, AirPlay, automation, network scanning) but the current UI/UX is "tactical / mission control" themed dark with custom skeumorphic cards, six competing corner radii, a home screen that does four jobs at once, jargon copy ("MISSION CONTROL", "TACTICAL CONNECTION INTERFACE", "PULSE SCAN", "SIGNAL JAM", "DEPLOY IDENTITY", "UNIFIED CLOAKED REPOSITORY"), and no light mode. The user wants a full redesign to feel like a top-tier Apple/Google shipping app — fully consumer clean, light + dark, all top-level screens — without breaking any of the underlying features (BT HID, Gemini, vault, macros, etc.).

**User-confirmed scope:** fully consumer clean (drop the tactical/military voice), full redesign of all top-level screens, both light and dark themes.

This is a large change. We will:
1. Build a real, single-source-of-truth **design system** (light + dark, Material 3 expressive + Apple HIG polish).
2. Replace the current global drawer scaffold with a **clean Material 3 navigation pattern** (modal drawer on phone, navigation rail on tablet, optional bottom nav for the top-level shell).
3. Redesign every **top-level screen** using the new system.
4. Tighten **copy** across the app.
5. **Preserve every feature, route, viewmodel, and BLE / Gemini / vault implementation** — only the visual layer changes.

The change is staged so that after Phase 1 the app still builds and runs; Phase 2 turns it into a finished redesign.

---

## Design system decisions (the foundation)

These are the choices everything else is built on. Apple/Google-grade means: **Material 3 first, Apple HIG where Material is silent, both light and dark, restrained type, semantic color, motion that explains state.**

### 1. New theme (`ui/theme/`)

**Files to create / replace:**
- `ui/theme/Color.kt` — replace the manual hex list with proper Material 3 `lightColorScheme()` / `darkColorScheme()` and a small set of semantic tokens (`BrandPrimary`, `BrandOnPrimary`, `Surface`, `SurfaceContainer`, `SurfaceContainerHigh`, `Outline`, `OnSurface`, `OnSurfaceVariant`, `Success`, `Warning`, `Error`, `Info`).
- `ui/theme/Type.kt` — **new** file. Define a clean type ramp:
  - System default font (no custom family — matches Google first-party apps; Apple's San Francisco look on iOS is approximated by system).
  - Display 28/36 W700, Headline 22/28 W600, Title L/M/S 18/16/14 W600, Body L/M/S 16/14/13 W400, Label L/M/S 14/12/11 W500.
  - Drop the per-pixel letter-spacing tricks; use Material defaults.
  - Negative tracking on display only.
- `ui/theme/Shapes.kt` — **new** file. Reduce to 4 corner radii: `xs=4.dp`, `sm=8.dp`, `md=12.dp`, `lg=16.dp`, `xl=24.dp`. Use Material 3 `RoundedCornerShape` consistently.
- `ui/theme/Spacing.kt` — **new** file. 4dp grid: `xxs=4, xs=8, sm=12, md=16, lg=20, xl=24, xxl=32`. Use these tokens everywhere; stop hardcoding 10/14/20/etc.
- `ui/theme/Motion.kt` — **new** file. 3 easings (`standard`, `emphasized`, `decelerated`) and 4 durations (150, 250, 400, 600) from Material 3 motion spec.
- `ui/theme/Theme.kt` — replace `RabitTheme` with `HackieTheme(darkTheme: Boolean, content)`. Sets `colorScheme`, `typography`, `shapes`, and configures `WindowCompat` for light/dark system bars.
- `res/values/themes.xml` + `res/values-night/themes.xml` — minimal XML theme to remove `windowBackground = black` so light mode actually works.

### 2. New shared component library (`ui/components/`)

**Goal:** one canonical widget for every recurring UI element. No more `InfoChip` reinvented in 3 files, no more two different "section header" recipes.

- `AppCard(modifier, onClick, content)` — single card style: 12dp radius, 1dp `outlineVariant` border, `surfaceContainer` color, content padding 16dp. Material 3 `Card` with `CardDefaults.elevatedCardColors` for emphasis. (Replaces `PremiumGlassCard`, `DarkSkeuoCard`, `GlassCard`, and the inline `Surface(shape=..., border=...)` pattern used in HomeScreen, PairingScreen, VaultScreen, etc.)
- `ScreenScaffold(title, subtitle, actions, onBack, content)` — single source of truth for the per-screen top bar. Used by every leaf screen. Subtitle is optional, small, sentence case, not a marketing tagline. Title uses `headlineSmall` (22sp W600).
- `SettingsListItem(title, subtitle, leading, trailing, onClick)` — Material 3 `ListItem` wrapper. Covers toggle, click, and value rows. **Pulls the `SettingsToggleItem` / `SettingsClickItem` from `SettingsContent.kt` into the shared module** so other screens can use them too.
- `StatusDot(color, size=8.dp)` — single dot primitive. No more 4dp/8dp/10dp and CircleShape/RoundedCornerShape variants.
- `InfoPill(label, value)` — replaces `InfoChip`/`StatusPill`. Used in Home for IP/MAC/P2P, in Vault for status, in Snippets for tags.
- `PrimaryButton(text, onClick, icon, enabled)` — one `Button` style. Replaces the four button styles currently used across dialogs (`AccentBlue Button`, `SuccessGreen Button`, `TextButton "Done"`, `VibrantGradientButton`).
- `IconTile(icon, color, size=40.dp)` — replaces the 30/40/44dp custom icon-in-tinted-box pattern.
- `MediaMiniPlayer(state, onPlayPause, onNext, onPrevious)` — extracted from HomeScreen, used on the home and the assistant.
- `EmptyState(icon, title, body, action?)` — used everywhere there's "no data" (Vault, Network, Snippets). Replaces the two competing empty-state layouts.
- `SectionHeader(title, action?)` — single recipe. Material 3 `titleSmall` (14sp W600), `onSurfaceVariant` color, no all-caps. Replaces `PremiumSectionHeader` (9sp tracked), `SectionHeader` (labelSmall tracked), and the inline all-caps titles.
- Keep `DeviceTypeBadge`, `ConnectionQualityIndicator`, `ConnectionStepIndicator`, `AnimatedDeviceCard`, `QuickConnectCard`, `BluetoothDisabledBanner` from `Components.kt` but update their visuals to match the new system (border, radius, color, type).

### 3. New global scaffold

`ui/components/RabitAppScaffold.kt` → rename to `HackieAppScaffold.kt`. Keep the same `LocalOpenGlobalDrawer` CompositionLocal so all the existing call sites keep working, but rebuild the look:
- ModalNavigationDrawer with **24dp corner radius** on the drawer sheet (Material 3 expressive).
- Drawer header: simple — Hackie logo + name + small subtitle "Pro" — no "PRO" badge stamped twice.
- Drawer body: **Material 3 `NavigationDrawerItems`** grouped by section label, no all-caps section headers, no jargon ("HACKIE", "TACTICAL RESEARCH LABS" → "Research", "ETHICAL HACKING" → "Testing", "WEB SNIPER (DAST)" → "Web security").
- Bottom: account row + theme toggle (light/dark/system) + a quieter "Lock vault" action. Replace the in-drawer "Decoy Protocol" toggle with a small footer link to settings (decoy is a power feature; it belongs in settings, not the drawer).
- Top app bar: **disabled** (keeps today's `showTopBar = false` behavior in `MainActivity`) so each screen keeps its own `TopAppBar`. We standardize the per-screen bar via `ScreenScaffold` instead of inventing a new global one.

### 4. Home screen redesign (the most visible change)

`ui/home/HomeScreen.kt` rewrite. The new home is **three things** instead of five:

1. **Hero status card** at the top: device name (large, 28sp), connection state ("Connected to MacBook Pro" / "Not connected"), a single big primary button that **promotes the most likely next action** ("Open Keyboard" when connected, "Connect a device" when not).
2. **Now Playing mini-bar**: persistent, compact, single row (album art 40dp + title/artist + play/pause). Tapping it opens the existing media controls in a bottom sheet (we can use `ModalBottomSheet`).
3. **Quick actions grid**: 4 large buttons — Keyboard, AI Assistant, Vault, Web Bridge — each with a 24dp icon and a label. Tapping the unconnected ones when offline shows a helpful "Connect first" snackbar instead of dead-ending. Plus a smaller row of: Helper, Refresh, Network (the diagnostic stuff, demoted).

Remove from the home: the "MISSION CONTROL" branding line, the IP/MAC/P2P chip card (move into a "Connection details" sheet triggered by tapping the hero card), and the cramped vertical scroll of stacked cards.

### 5. Per-screen redesigns (top-level)

For each, keep the existing `viewModel` / route / data flow; rewrite the visual layer to use the new system.

- `ui/pairing/PairingScreen.kt` — rename top bar from "CONTROL HUB / TACTICAL CONNECTION INTERFACE" to "Connect a device". Replace `Surface(onClick=...)` segmented control with Material 3 `SingleChoiceSegmentedButtonRow`. Replace `ROOT ✓ / NO ROOT` 9sp black badge with a small `FilterChip` that says "USB" / "Bluetooth" plainly. Drop "PULSE SCAN", "SIGNAL JAM", "DEPLOY IDENTITY", "IDENTITY LAB", "EMULATED" jargon. Keep the device list, the identity lab functionality, and the pairing flow. Use `AnimatedDeviceCard` and `QuickConnectCard` from the new component library.
- `ui/assistant/AssistantScreen.kt` (and the `assistant/*` helpers) — rename the "SMART MACRO GENIE" modal to "Generate macro". Replace "AI is brewing your macro..." with "Generating macro...". Replace "ABORT SEQUENCE" with "Cancel". Replace "Summon Genie" with "Generate". Drop the "Hackie" stamp from the drawer item. Use Material 3 expressive chat bubble shapes. Replace the custom `PremiumChatTopBar` with a normal `TopAppBar` driven by `ScreenScaffold`.
- `ui/settings/SettingsContent.kt` — replace the long `Column { ... }` scroll with a `LazyColumn` of sections. Each section header is a `SectionHeader` (14sp, sentence case). Each row uses `SettingsListItem`. Replace the inline `Tactile Engine` segmented control with Material 3 `SingleChoiceSegmentedButtonRow`. Replace `Toast.makeText(...)` with `Snackbar` calls. Drop the "ABOUT THE DEVELOPER" self-promotion block — keep a small "About" entry that goes to a simple screen (a separate `AboutScreen` is **out of scope**; we just strip the long bio block to "Hackie" + version + a single "Made by Sagar M" line). Drop "FEATURE VISIBILITY" jargon — keep the toggles, rename section to "Modules".
- `ui/security/VaultScreen.kt` — rename "FORENSIC VAULT / UNIFIED CLOAKED REPOSITORY" to "Saved activity". Tab labels become "Security audits" / "Traffic logs" in sentence case at proper Material 3 tab size. Use `AppCard` for records. Use `EmptyState` for the empty case. Drop "TARGET:" / "FINDINGS:" / "PACKETS:" inline all-caps labels; use proper ListItem rows.
- `ui/snippets/SnippetsScreen.kt` — top app bar "Snippets". Use `LazyColumn` of `ListItem` rows with a trailing chevron, plus a single `PrimaryButton` (FAB or top-bar action) for "New snippet".
- `ui/automation/AutomationDashboardScreen.kt` — top app bar "Macros". Search field at top, `LazyColumn` of macro cards, single FAB for "Add macro". Remove "QUICK ACTIONS"-style marketing headers.
- `ui/crypto/CryptoToolkitScreen.kt` — replace the two tab labels with sentence case. Use `AppCard` for panels. Keep the encoder/decoder and stego functionality.
- `ui/network/NetworkReconScreen.kt` — Material 3 `TabRow` (not `ScrollableTabRow`; the tabs fit). Each tab is a `LazyColumn` of `ListItem` rows. Use `EmptyState` for no-results.
- `ui/websniper/WebSniperScreen.kt`, `ui/security/SecurityAuditorScreen.kt`, `ui/security/TrafficAnalyzerScreen.kt`, `ui/payload/PayloadForgeScreen.kt`, `ui/pentest/PentestToolkitScreen.kt` — apply the same look: `ScreenScaffold` + `LazyColumn` of `ListItem` rows + `EmptyState`. These are "system-styled" rather than fully bespoke (time budget), but they all inherit the new theme + component library.
- `ui/assistant/WelcomeScreen.kt`, `ui/assistant/ChatBubble.kt`, `ui/assistant/ChatTopBar.kt`, `ui/assistant/ChatDrawer.kt` — update the chat visuals: rounded Material 3 chat bubbles (user filled with `primary`, AI with `surfaceContainerHigh`), proper `TopAppBar`, drop the "Premium" naming.
- `ui/components/MarkdownText.kt`, `PulsingVoiceButton.kt`, `BiometricGuard.kt`, `BridgeBiometricAuth.kt`, `SkeuoCard.kt` — keep but restyle to match. `SkeuoCard` becomes `AppCard`; remove the gradient skeumorphism.
- `MainActivity.kt` — minimal changes. The `AppNavigation` block stays. Update `RabitAppScaffold` → `HackieAppScaffold`. Update `themes.xml` so the Activity can pick up the system light/dark correctly. Confirm `enableEdgeToEdge()` is set so the safe area handles properly. Update window background so the launcher splash isn't a black box on a light device.

### 6. Copy cleanup (sweep across the app)

Concrete renames, applied as we touch each file:

| Old | New |
|---|---|
| HACKIE PRO / MISSION CONTROL | Hackie |
| CONTROL HUB / TACTICAL CONNECTION INTERFACE | Connect a device |
| FORENSIC VAULT / UNIFIED CLOAKED REPOSITORY | Saved activity |
| SECURITY AUDITS / TRAFFIC LOGS | Security audits / Traffic logs |
| QUICK ACTIONS | Shortcuts |
| PULSE SCAN / SIGNAL JAM | (delete; this feature stays but copy becomes neutral) |
| DEPLOY IDENTITY | Save |
| ABORT SEQUENCE | Cancel |
| SMART MACRO GENIE | Generate macro |
| AI is brewing your macro... | Generating macro… |
| Summon Genie | Generate |
| Target / Node / Signature / Emulated / Deploy | (use plain device-words: device, address, identifier, custom, save) |
| ROOT ✓ / NO ROOT | USB (or "Not available" / "Available") |
| STEALTH HISTORY | Clear session on exit |
| SMART AUTOMATION | Automation |
| VOICE & SPEECH ENGINE | Voice |
| TACTILE ENGINE | Haptic style |
| FEATURE VISIBILITY | Modules |
| ABOUT THE DEVELOPER (with bio) | About |
| HACKIE PRO / Hackie PRO | Hackie |

### 7. Accessibility & motion (light pass)

- Every icon-only `IconButton` already has a `contentDescription`; double-check new screens do too.
- Settings list items: ensure `Role.Switch` and `Role.Button` are set where they aren't already.
- Color contrast: the new color scheme passes WCAG AA for body text in both modes.
- Motion: replace the loud "pulsing green dot" indicators with subtle Material 3 `LoadingIndicator` / `LinearProgressIndicator` where appropriate. Keep `PushControlBar`'s pulse (it's load-bearing — it tells the user a push is in flight).

---

## Files this plan touches (the long list)

New files:
- `ui/theme/Type.kt`
- `ui/theme/Shapes.kt`
- `ui/theme/Spacing.kt`
- `ui/theme/Motion.kt`
- `ui/components/AppCard.kt`
- `ui/components/ScreenScaffold.kt`
- `ui/components/SettingsListItem.kt`
- `ui/components/StatusDot.kt`
- `ui/components/InfoPill.kt`
- `ui/components/PrimaryButton.kt`
- `ui/components/IconTile.kt`
- `ui/components/MediaMiniPlayer.kt`
- `ui/components/EmptyState.kt`
- `ui/components/SectionHeader.kt`
- `ui/components/HackieAppScaffold.kt` (replaces the body of `RabitAppScaffold.kt`)

Heavily modified:
- `ui/theme/Color.kt`
- `ui/theme/Theme.kt`
- `res/values/themes.xml`
- `res/values-night/themes.xml`
- `MainActivity.kt` (scaffold call + light-mode plumbing)
- `ui/home/HomeScreen.kt`
- `ui/pairing/PairingScreen.kt`
- `ui/assistant/AssistantScreen.kt` + `assistant/WelcomeScreen.kt`, `ChatBubble.kt`, `ChatTopBar.kt`, `ChatDrawer.kt`, `InputArea.kt`
- `ui/settings/SettingsContent.kt` (incl. moving `SettingsToggleItem` / `SettingsClickItem` out into `ui/components/`)
- `ui/security/VaultScreen.kt`
- `ui/components/Components.kt` (restyle existing widgets, delete `DarkSkeuoCard` body)
- `ui/components/SkeuoCard.kt` (delete or repoint to `AppCard`)
- `ui/snippets/SnippetsScreen.kt`
- `ui/automation/AutomationDashboardScreen.kt`
- `ui/crypto/CryptoToolkitScreen.kt`
- `ui/network/NetworkReconScreen.kt` (just restyle the tab host)
- `ui/components/BiometricGuard.kt` (update colors)
- `ui/components/MarkdownText.kt` (update bubble colors)

Lightly modified (theming + section header / screen scaffold only):
- `ui/websniper/WebSniperScreen.kt`
- `ui/security/SecurityAuditorScreen.kt`
- `ui/security/TrafficAnalyzerScreen.kt`
- `ui/payload/PayloadForgeScreen.kt`
- `ui/pentest/PentestToolkitScreen.kt`
- `ui/osint/IntelOsintScreen.kt`
- `ui/forensics/ForensicsLabScreen.kt`
- `ui/loot/LootViewerScreen.kt`
- `ui/qa/NeuralLabScreen.kt`
- `ui/keyboard/KeyboardScreen.kt` (keyboard visuals themselves stay; the screen top bar uses `ScreenScaffold`)
- `ui/webbridge/WebBridgeScreen.kt`
- `ui/helper/HelperScreen.kt`
- `ui/airplay/AirPlayReceiverScreen.kt`
- `ui/lockdown/LockdownScreen.kt`
- `ui/onboarding/OnboardingScreen.kt`
- `ui/automation/*` (terminal screens, ssh screens, etc. — the terminal UI is mostly its own thing; we only touch the top bar + the launch state)
- `ui/profile/ProfileScreen.kt`
- `ui/search/GlobalSearchScreen.kt`
- `ui/browser/BrowserScreen.kt`

Untouched (no UI surface or just service code):
- `data/**`, `domain/**`, `RabitApp.kt`, the `*ViewModel.kt` files, `HidService`, `HidDeviceManager`, etc. **All features continue to work.**

---

## Execution order (staged so it stays buildable)

**Stage 1 — Design system (no visible changes to the app, but the foundation is in place).**
1. Create `Type.kt`, `Shapes.kt`, `Spacing.kt`, `Motion.kt`.
2. Rewrite `Color.kt` + `Theme.kt` with light + dark schemes. Old `RabitTheme` and old `Color.*` constants stay as deprecated aliases (re-exported from `Color.kt` pointing at the new tokens) so the rest of the app still compiles. Add a `HackieTheme` Composable that wraps `MaterialTheme` with the new scheme.
3. Update `res/values/themes.xml` + `values-night/themes.xml` so the host window picks light vs dark.

**Stage 2 — Shared components.**
4. Create the new `ui/components/*` widgets listed above. They live alongside the existing ones; nothing in the existing screens breaks yet.
5. Rewrite the body of `RabitAppScaffold.kt` to use the new visual language. Add a `HackieAppScaffold` Composable with the same signature.

**Stage 3 — Top-level screens (the visible change).**
6. Rewrite `ui/home/HomeScreen.kt` to the three-card layout.
7. Rewrite `ui/pairing/PairingScreen.kt` (drop jargon, segmented buttons, list items).
8. Rewrite the `assistant/*` chat surfaces.
9. Rewrite `ui/security/VaultScreen.kt`.
10. Rewrite `ui/settings/SettingsContent.kt` to use the new shared `SettingsListItem`; move `SettingsToggleItem` / `SettingsClickItem` into `ui/components/SettingsListItem.kt`.
11. Apply the `ScreenScaffold` + `SectionHeader` + `EmptyState` pattern to the remaining top-level screens (Snippets, Automation, Crypto, Network, etc.).
12. Copy sweep (the table in §6) applied during each screen pass.
13. Wire `MainActivity` to call `HackieAppScaffold` and ensure the system theme (light / dark / system) flows through.

**Stage 4 — Verification.**
14. `./gradlew assembleDebug` — must succeed.
15. Manual smoke: launch the app, walk Home → Pairing → Keyboard → Assistant → Settings → Vault → Snippets. Confirm BT HID, Gemini call, and vault still work end-to-end (we cannot pair to a real Mac in this environment; we verify the code paths still compile and the existing view-model surfaces still feed the screens).
16. Visual pass: confirm light + dark, contrast, no jitter, no jarring colors.

---

## Verification

**End-to-end checks before we call this done:**

1. **Build:** `./gradlew assembleDebug` completes. We will not merge code that doesn't compile.
2. **Routes preserved:** every existing route in `MainActivity` still resolves to a Composable and that Composable still calls the same viewmodel APIs. Routes touched: `home`, `pairing`, `keyboard` (= `main`), `assistant`, `browser`, `settings`, `helper`, `web_bridge`, `vault` (no route; still via decoy), `snippets`, `automation`, `network_auditor`, `crypto_toolkit`, `security_auditor`, `traffic_analyzer`, `web_sniper`, `forensics_lab`, `payload_forge`, `pentest_toolkit`, `key_macros`, `profile`, `global_search`, etc.
3. **Feature call sites preserved:** all `viewModel.xxx` calls in each screen still compile and run. We are not changing any viewmodel signature, repository signature, service signature, or `HidService` interface. This is the "don't break the features" guarantee.
4. **Light + dark:** the same screen renders correctly under both `isSystemInDarkTheme()` paths. Confirmed by setting the system theme to Light and Dark on the emulator and screenshotting.
5. **No stale references:** the deprecated `RabitTheme` and old `Color.*` constants in `Color.kt` are either deleted or kept as aliases. No file outside `ui/theme/` imports a removed symbol. The build proves this.
6. **Component inventory:** every screen that used to render an inline `Surface(shape=..., border=...)` for "card" now uses `AppCard` or `ListItem`. Every section header uses `SectionHeader`. Every top bar uses `ScreenScaffold`. Every primary button uses `PrimaryButton`. Every empty state uses `EmptyState`.

---

## Out of scope (intentionally)

- Adding **bottom navigation** at the bottom of the screen. The user has a global drawer; we keep the architecture. (If you want bottom nav, that's a separate change.)
- Rewriting **`strings.xml` / i18n**. The app is English-only today; we keep that. (A future pass should externalize strings.)
- Adding a **separate `AboutScreen`**. The bio block goes away; the "About" entry in settings just shows app name + version.
- Changing the **Gemini model or any AI prompt**; only the UI changes.
- Any new features (e.g. dark-light animated theme switcher, gesture nav, etc.). This is purely a visual + copy redesign.
- **Tablet / foldable** layout work. The current modal drawer scales acceptably; the new one will too. The `WindowSizeClass` adapt-to-rail pass is a future task.
