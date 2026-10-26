# Validation

## 0.16.1 icon correction — 2026-10-26

The approved flattened PNG replaces the original shaded backdrop and black corners with Backup's exact solid #1F2228 background and rounded-corner alpha. Pixel checks preserved the original raven/controller/arrow artwork and verified the APK-packed visible pixels and alpha. The icon-only preview installed over the official 0.16.0 APK, preserving the running bridge process.

This patch changes the icon and build/version metadata only. Final-to-preview payload and public-layout rebuild comparisons are required for publication. No additional gameplay, reboot or new-model acceptance is claimed beyond the working 0.16.0 milestone below.

## 0.16.0 milestone — 2026-10-25

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
