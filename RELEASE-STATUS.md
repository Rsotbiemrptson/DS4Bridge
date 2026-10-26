# Release status

Current final milestone: **Raven Gamepad 0.16.0**, version code **31**, dated 2026-10-25.
Previously DS4 Bridge; package/signing identity unchanged.

The 0.16.0-test1 build passed Odin 2 UI/input checks and user-confirmed Remote Play gameplay before and after reboot. After first unlock, the boot launcher, new bridge process and virtual Sony controller were observed without opening Raven Gamepad. The user then confirmed all controls still worked in-game.

The final APK changes release metadata only relative to that accepted preview. All 14 APK payload entries outside the manifest/signature were compared and matched exactly. Public-layout source rebuilding is separately checked before publishing.

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
