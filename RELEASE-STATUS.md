# Release status

Current final milestone: **Raven Gamepad 0.16.1**, version code **33**, dated 2026-10-26.
Previously DS4 Bridge; package/signing identity unchanged.

The 0.16.0-test1 build passed Odin 2 UI/input checks and user-confirmed Remote Play gameplay before and after reboot. After first unlock, the boot launcher, new bridge process and virtual Sony controller were observed without opening Raven Gamepad. The user then confirmed all controls still worked in-game.

For 0.16.0, the final APK matched all 14 payload entries outside the manifest/signature of the accepted gameplay preview. Version 0.16.1 changes only the icon and build/version metadata: its flattened PNG uses Backup's solid #1F2228 background and rounded transparency, with the original emblem preserved. The icon-only preview installed successfully and the bridge process stayed running. The new final payload and public-layout rebuild are verified before publication; controller/runtime behavior is unchanged.

| Area | Status |
| --- | --- |
| Odin 2 installation/runtime, app selection and mapping | Hardware-tested in this project |
| Latest UI, both orientations/themes, persistent pane navigation | Passed |
| Mapping input forwarding, accented Cancel, unsaved guard and draft recreation | Passed |
| Color presets/RGB/hex, Apply/Cancel/Default | Passed |
| Remote Play full controls before and after reboot | User-confirmed |
| Automatic startup after first unlock, without opening the app | Observed |
| Odin 3 | Adapter inspected; hardware testing still needed |
| Retroid Pocket 6 | Read-only diagnostics only; bridging disabled; hardware testing needed |
| Battery consumption / accessibility-based watcher | Not measured / not implemented |

No device logs, serials, account credentials, OEM APKs or firmware are included in this repository.
