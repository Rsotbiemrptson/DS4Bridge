# Validation

## 0.16.3 rename and controller-style tile - 2026-10-30

Odin 2 component-upgrade checks passed current/legacy/duplicate exclusion cleanup, unchanged Sony IDs, virtual-input filtering and native start/stop/restart while retaining mappings, app targets, preferences and unrelated exclusions. Four-mode instrumentation checked real stock Odin/Xbox identity, selected-app enable for Remote Play and disable outside selections, all-app forwarding, Disconnected pause and reconnect. The native PID remained constant across the complete mode cycle. Actual Quick Settings tile taps independently verified the cycle and the same running PID.

The user confirmed all four modes work. A subsequent reboot was followed by first unlock without opening Raven Gamepad: a new bridge process, Raven Gamepad virtual controller and verified OEM exclusion appeared automatically, retaining Odin/per-app mode. The user confirmed all Remote Play controls still worked after reboot. Global/disconnected policy and settings were exercised live; reboot was specifically tested from Odin/per-app mode, not every tile mode separately.

Final publication compares native/resources/runtime against the accepted test and rebuilds the public source independently. Build/version metadata can differ from the preview. No raw device logs or identifying screenshots are published. Odin 3 and Retroid still need hardware testing; the tile's stock style writes are restricted to Odin 2 / API 33. Touchpad support is not included.

## 0.16.2 UI and mapping update — 2026-10-29

The preview builds passed Odin 2 mapping learning and exclusive assignment, section grouping/collapse, D-pad key and HAT handling, draft recreation, installed-app filtering/search/selection and diagnostics/root-check feedback. The controller-refresh popup was captured and inspected. Light/dark landscape/portrait screenshots confirmed the right-aligned chevron, centered dots and exact Backup palette. Saved mappings, targets, boot preferences and runtime PID were preserved by UI acceptance checks.

The final build is compared with 0.16.2-test2 outside version metadata/signature, and independently rebuilt through the portable public build. Native/runtime assets remain unchanged from 0.16.1. No fresh in-game/reboot acceptance is claimed for this UI-only update. Touchpad support is not included; Odin 3 and Retroid still require hardware testing.

## 0.16.1 icon correction â€” 2026-10-26

The approved flattened PNG replaces the original shaded backdrop and black corners with Backup's exact solid #1F2228 background and rounded-corner alpha. Pixel checks preserved the original raven/controller/arrow artwork and verified the APK-packed visible pixels and alpha. The icon-only preview installed over the official 0.16.0 APK, preserving the running bridge process.

This patch changes the icon and build/version metadata only. Final-to-preview payload and public-layout rebuild comparisons are required for publication. No additional gameplay, reboot or new-model acceptance is claimed beyond the working 0.16.0 milestone below.

## 0.16.0 milestone â€” 2026-10-25

The tested 0.16.0-test1 preview ran on the owner's Odin 2. The final 0.16.0 APK changes version metadata only; every payload entry outside the manifest/signature matched the accepted preview. The source was then organized into the public flat layout and rebuilt separately.

- Passed signed-APK build, app search/installed-only selection/system filter, theme recreation and unchanged saved app choices.
- Passed both themes and landscape/portrait layouts; only Apps is visible initially. Settings reveals Appearance first and the other three tools, keeping the same sidebar/header while replacing the right pane.
- Passed embedded controller button delivery to Diagnostics and mapping, Cancel without edits, unsaved-navigation prompts and mapping-draft recreation. The pane-state issue found during testing was fixed before acceptance.
- Passed initially collapsed Advanced, expansion/collapse, equal rounded buttons, descriptions and accessible lower rows.
- Passed six circular presets, RGB/hex synchronization, Cancel/Apply/Default, About and offline license.
- User confirmed all Remote Play face buttons, D-pad, both sticks and triggers in a game before reboot.
- After reboot/first unlock, the boot launcher, new native process and virtual Sony 054c:09cc controller were observed without opening Raven Gamepad. User confirmed all controls still worked in-game.

Automated UI checks preserved app selection, mapping files, startup setting and native process. Original Follow system appearance was restored after testing. Device logs, serials, private reports and account information are intentionally excluded.

## Hardware limits

Odin 3 has an inspected adapter but no hardware acceptance. Retroid Pocket 6 has only pinned read-only diagnostics and still needs hardware testing; component setup/start/exclusion writes remain disabled. RP5 and other manufacturers/versions are not claimed supported. See [Usage](USAGE.md#compatibility).

## Future release checks

Run host adapter tests, build/signature/alignment checks, UI checks in both orientations/themes, input/mapping/draft checks, install/update/removal checks on a supported spare device, then target-game controls before and after reboot. Preserve original user files and restore test preferences. Never claim a new model works from firmware inspection alone. Battery cost of the once-per-second watcher remains unmeasured.
