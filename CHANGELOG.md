# Changelog

## 0.15.0 — 2026-10-24

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

## 0.14.5 — 2026-10-15

- Rename component actions to Install/Update Components and Uninstall Bridge
  Components; align the uninstall confirmation wording.
- Remote Play and reboot startup confirmed on a second Odin 2 after removal
  of its earlier firmware-baked bridge.

## 0.14.4

- Always-visible Advanced tools in two columns and three rows, equal rounded
  buttons and compact landscape layout.

## 0.14.3

- Main options: Show system apps, Start bridge after reboot, Settings.

## 0.14.2–0.14.0

- Consistent app-list styling, grouped Bridge controls, landscape sidebar,
  focused input mapping, themes and diagnostics layout.
- State-aware Start/Stop/Restart and Save and restart bridge.

## Earlier all-in-one development

- Bundled runtime installation and startup without a separate script kit.
- Odin/Xbox mode-aware labels, physical-input learning and exclusive assignment.
- Live input diagnostics and known AYN privileged-interface checks.

Older experimental APKs, script kits and firmware integrations are not supported
installation paths for this release and are not included in this repository.
