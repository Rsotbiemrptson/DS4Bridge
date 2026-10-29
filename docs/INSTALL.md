# Install and update

## Fresh installation on a supported Odin 2

1. Download the official `Raven-Gamepad-0.16.2.apk` from [Releases](https://github.com/Rsotbiemrptson/Raven-Gamepad/releases/tag/v0.16.2).
2. Open the APK and allow installation from that source when Android asks.
3. Open Raven Gamepad. Use the top-right three-dot menu to open **Settings**.
4. Select **Bridge controls**, expand **Advanced**, then tap **Install/Update Components**. Wait for the installation to finish.
5. Tap **Start** and wait for **Status: Running**.
6. Select **Apps**, check PS Remote Play or your other installed target apps, and tap **Save selected apps**. Search accepts names or package IDs. **Show system apps** is in the three-dot menu. Remote Play is not preselected or shown if it is absent.
7. Fully close and reopen the target app, connect to your console if needed, and test all buttons, both sticks, D-pad and triggers in a game.
8. Optional: under **Settings > Bridge controls**, enable **Start bridge after reboot**. Reboot, unlock once, and test without first opening Raven Gamepad.

Normal setup does not need USB debugging, a computer, a separate script kit or a firmware flash. The device must expose the supported OEM privileged interface described in the README; allowing APK installation does not itself grant that access.

## Update an existing official installation

Install the new APK over DS4 Bridge/Raven Gamepad. The package ID remains `com.odin2.ds4bridgehelper`, and the official signing identity is unchanged. Keep the old app installed so Android retains its app choices, mappings, theme and startup option.

Version 0.16.2 changes the interface and branding; native/runtime payloads remain unchanged from 0.15.0 (readiness 15). Existing working components do not need replacement just for this update. No firmware flash or factory reset is required. The 0.16.2 update changes the mapping editor, app filtering and interface feedback; existing runtime components do not need replacement.

Community builds signed with another key cannot update the official app in place. Complete component removal and uninstall before switching signing identities; uninstalling loses app-private settings.

## Older firmware/script integrations

The component controls manage this standalone app's runtime. They do not remove an older firmware-baked bridge or another script installation. Do not run two bridges simultaneously. Firmware bridge detection disables standalone controls; migrate that integration separately before proceeding.
