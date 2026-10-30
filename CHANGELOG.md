# Changelog

## 0.16.3 - 2026-10-30

- Rename the virtual Sony controller from Odin DS4 Bridge Test to Raven Gamepad, retaining Sony VID/PID and existing app/package identity.
- Update Touch Mapping exclusions and virtual-input filtering; clean up both current and legacy exclusion names, including duplicates, without changing unrelated exclusions.
- Add a Raven Gamepad Quick Settings tile cycling Odin, Xbox, Raven Gamepad and Disconnected. Odin/Xbox keep per-app forwarding; Raven forwards to all apps. Disconnected pauses forwarding and stock controller buttons without stopping the bridge; switching back reconnects automatically.
- Gate stock controller-style writes to the verified Odin 2 / Android 13 interface. Preserve saved app selections, mappings and boot preference.
- Add component revision 17 and the selected/all/off forwarding endpoint. Global/paused operation skips foreground-app queries; the supervisor still checks its mode every second.
- Wait for the old runtime and owned exclusions to stop before replacing components. Existing installations need Install/Update Components once for the new name and tile.
- Verify all four modes through instrumentation and actual tile taps, with the same native PID across the cycle. User confirmed all modes work, then automatic startup and all Remote Play controls after reboot.
- Keep Odin 3 hardware support unverified and Retroid bridging disabled pending hardware testing. The new tile's style switching is Odin 2 only. Touchpad emulation remains deferred.

## 0.16.2 — 2026-10-29

- Add larger, collapsible controller-mapping sections; preserve their state across recreation and pane navigation.
- Group analog triggers under Shoulders, stick clicks under Analog Sticks, and Share/Options/PS under Face buttons. Remove the separate Other buttons section and D-pad axis editor rows while retaining working D-pad handling.
- Label the Share input Select, matching the existing native mapping.
- Put Advanced's chevron on the right and center the three-dot menu artwork inside its button.
- Confirm exact Raven Backup sidebar/card colors and keep About focused on Raven Gamepad.
- Show a controller-count popup after Refresh controllers in Diagnostics.
- Treat Android Developer Verifier, SafetyCore and System Key Verifier as system components even when installed in the data partition.
- Verify mapping, app filtering, diagnostics and dark/light landscape/portrait UI on Odin 2; preserve saved mappings, app targets and automatic startup. Native/runtime payloads are unchanged. Gameplay/reboot acceptance is inherited from earlier runtime testing, not a fresh 0.16.2 gameplay test.
- Touchpad/DualSense overlay support is not included. Odin 3 and Retroid still require hardware testing; Retroid bridging remains disabled.

## 0.16.1 â€” 2026-10-26

- Replace the icon's shaded background and baked-in black corners with Raven Backup's exact solid #1F2228 background and rounded-corner transparency.
- Preserve the original raven, game controller and blue arrow artwork. The app uses a single flattened PNG, not separate artwork layers or regenerated artwork.
- Keep the package, signing identity, controller/runtime payloads, settings and UI behavior unchanged.
- Verify the packaged icon's visible pixels/alpha, matching public-source rebuild and icon-only preview installation. Existing Odin 2 gameplay/reboot acceptance remains applicable; no new Odin 3 or Retroid hardware claim is made.

## 0.16.0 â€” 2026-10-25

- Rename the app Raven Gamepad, with the blue Raven icon and solid background. Keep the package/signing identity and working bridge runtime unchanged.
- Add the Raven-family appearance card, light/dark/system theme, wheel/RGB/hex accent picker and six circular presets.
- Keep a persistent sidebar/header while switching the right pane. Apps is the only main tab; menu Settings reveals Appearance, Bridge controls, Controller mapping and Diagnostics.
- Move automatic startup into Bridge controls; make Advanced collapsible with themed buttons and individual explanations. Show actual Status: ... without routine status-check chatter.
- Accent mapping Cancel buttons and retain draft edits across screen rebuilds, including the embedded-pane restoration fix.
- Match the Raven About layout and provide the full GPL text offline.
- Verified Odin 2 UI, controller events, app selection and mapping draft handling. Remote Play controls worked before and after reboot; automatic startup after first unlock was observed without opening the app.
- Adopt Raven Backup's repository structure and separate installation, removal, usage, build and test guides. Odin 3/Retroid still need hardware tests; Retroid bridging remains disabled.

## 0.15.0 â€” 2026-10-24

- Detect known Odin 2 legacy/modern and inspected Odin 3 Touch Mapping services
  automatically; verify package/component, interface and applicable version.
- Remember the exclusion owner, verify removal explicitly and reject unknown
  protocols rather than guessing transaction codes.
- Diagnostics displays integration/version/action and can check the real
  read-only connection from the installed app UID.
- Add read-only diagnostics for the inspected RP6 June 2026 mapping version.
  **Odin 3 and Retroid still need hardware testing. Retroid setup/start/exclusion
  writes remain disabled; this is not a universal-device support announcement.**
- Odin 2 test1 passed detection, Remote Play gameplay and automatic startup plus
  gameplay after reboot/first unlock. Final version changes release metadata
  only relative to that tested APK. Existing readiness-15 runtime is unchanged.

## 0.14.5 â€” 2026-10-15

- Rename component actions to Install/Update Components and Uninstall Bridge
  Components; align the uninstall confirmation wording.
- Remote Play and reboot startup confirmed on a second Odin 2 after removal
  of its earlier firmware-baked bridge.

## 0.14.4

- Always-visible Advanced tools in two columns and three rows, equal rounded
  buttons and compact landscape layout.

## 0.14.3

- Main options: Show system apps, Start bridge after reboot, Settings.

## 0.14.2â€“0.14.0

- Consistent app-list styling, grouped Bridge controls, landscape sidebar,
  focused input mapping, themes and diagnostics layout.
- State-aware Start/Stop/Restart and Save and restart bridge.

## Earlier all-in-one development

- Bundled runtime installation and startup without a separate script kit.
- Odin/Xbox mode-aware labels, physical-input learning and exclusive assignment.
- Live input diagnostics and known AYN privileged-interface checks.

Older experimental APKs, script kits and firmware integrations are not supported
installation paths for this release and are not included in this repository.
