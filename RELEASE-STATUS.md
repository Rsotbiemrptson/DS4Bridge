# Release status

Current final milestone: **Raven Gamepad 0.16.3**, version code **39**, dated 2026-10-30.
Previously DS4 Bridge; package/signing identity unchanged. Virtual controller now named Raven Gamepad; Sony IDs unchanged.

All four Quick Settings modes passed live checks and user testing: Odin/Xbox retain per-app forwarding, Raven forwards globally, Disconnected pauses without stopping the runtime. Component revision 17 migrates the name/exclusion and supports selected/all/off forwarding. Existing published installations need Install/Update Components once.

Automatic startup after reboot/first unlock was observed without opening the app, retaining Odin/per-app mode; the user confirmed all Remote Play controls worked afterward. Reboot was tested from Odin/per-app mode. Final/source rebuild and preview-payload checks accompany publication. Touchpad support remains deferred.

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
