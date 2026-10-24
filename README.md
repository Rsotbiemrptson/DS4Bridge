# DS4 Bridge

Use the AYN Odin 2's built-in controls as a virtual DualShock 4 controller in
selected apps. One APK includes the settings interface and bridge components;
no separate script kit or firmware flash is needed for the tested setup.

**Current release: 0.15.0 (version code 29).**
[Download the APK](https://github.com/Rsotbiemrptson/DS4Bridge/releases/download/v0.15.0/DS4-Bridge-0.15.0.apk).

Automatic Odin 2/Odin 3 Touch Mapping detection is now included. **Odin 3 and
Retroid support still need hardware testing. Retroid bridging is not enabled;
the inspected RP6 mapping version has read-only diagnostics only.**

## Compatibility and important limits

- Validated on Odin 2 stock firmware 1.0.0.318 / Android 13: installation,
  Remote Play gameplay, controller mapping and startup after reboot.
- Also tested on a second Odin 2 after its previous firmware-baked bridge was
  removed: Remote Play and automatic startup after reboot worked.
- This release's detection changes were tested as 0.15.0-test1 on Odin 2:
  verified legacy service connection, Remote Play controls, automatic startup
  after first unlock and working controls after reboot. 0.15.0 changes the
  release version metadata only, not the tested app functionality.
- Odin and Xbox controller modes are supported, with face-button positions
  handled for each mode. App selection controls forwarding.
- Odin 3 adapter: inspected mapping APK `com.odin.mapping`, version
  `3.0(20251128_112846)`. It is **not hardware-validated**; unknown mapping
  versions are reported rather than sent guessed commands.
- Retroid RP6 mapping APK `com.rp.mapping` version `3.0(20260615_164638)` has a
  verified offline read-only diagnostic path. Setup, bridge startup and exclusion
  writes are disabled on Retroid pending hardware testing. RP5, other versions
  and manufacturers are not claimed supported. Sharing a processor or interface
  name does not establish compatibility.
- No Magisk installation was required on the tested stock Odin 2. However,
  this is **not an unprivileged app**: it relies on AYN's `PServerBinder`
  service providing root execution, Linux input access and `/dev/uinput`.
  Devices without that permitted interface are unsupported by this release.
- This does not certify the device, change its fingerprint, fix Play Integrity,
  or guarantee that any particular app accepts the virtual controller.
- Not affiliated with AYN, Sony or PlayStation. Use at your own risk.

## Install and start

1. Download `DS4-Bridge-0.15.0.apk` to the Odin.
2. Open it and allow installation from that source when Android asks.
3. Open **DS4 Bridge**. In the top-right menu, open **Settings**.
4. Open **Bridge controls** and tap **Install/Update Components**. Wait for
   "App components installed. Start the bridge when ready."
5. Tap **Start** and wait for **Bridge running**.
6. Return to the app list, select PS Remote Play or your other installed target
   apps, and tap **Save selected apps**. Search and **Show system apps** help
   find installed apps; Remote Play is not preselected.
7. Fully close and reopen the target app, connect to your console if needed,
   and test buttons and both sticks in a game.
8. Optional: enable **Start bridge after reboot** in the app-list menu. Restart
   the Odin once and confirm the target app works without opening DS4 Bridge.
   Automatic startup waits for the first unlock after reboot.

No USB debugging or computer connection is needed for normal setup. Granting
permission to install an APK does not itself grant root; privileged access
depends on the OEM service described above.

### Mapping and settings

Under **Settings > Controller mapping**, hold a button row and follow the
learning prompt to press the desired physical input. Tap an axis to record it;
hold an axis to invert it. Cancel exits
learning. Assigning an input already used elsewhere clears its old assignment.
Use **Save and restart bridge** to apply the mapping. Labels adapt to Odin or
Xbox mode. Diagnostics shows the selected physical controller and live inputs;
technical details and bridge checks are available separately.
Technical details now includes the detected mapping service/version, exclusion
owner and last verification/action. Run bridge checks performs a verified
read-only list query, not an exclusion change.

## Remove completely — do this before uninstalling the APK

1. Keep DS4 Bridge installed while completing these steps.
2. Open **Settings > Bridge controls > Advanced**.
3. Tap **Uninstall Bridge Components**, then confirm **Uninstall**.
4. Wait for successful removal; **Components not installed** is the expected
   final status. Saved app selections and mappings remain until APK uninstall.
5. Uninstall **DS4 Bridge** using Android's normal app settings or launcher.

The component action stops the standalone runtime, removes its root-owned
files from `/data/local/ds4-bridge-app` and disables its automatic startup.
Normal supervisor shutdown requests removal of the virtual pad's AYN
touch-mapping exclusion. The removal action also explicitly verifies exclusion
cleanup using the saved integration, even if the supervisor was already stopped.
If removal fails, keep the APK installed and collect
the error/Diagnostics report rather than assuming cleanup succeeded.

**Uninstalling the APK first is not complete removal:** Android does not delete
that external runtime directory. Reinstall the same-signed APK and perform the
component-removal steps if you skipped them. Manually downloaded APKs, ZIPs or
old extracted script kits in shared storage are not removed automatically.

This action does **not** remove a firmware-baked bridge or legacy script-kit
installation. Firmware bridge detection deliberately disables standalone
controls. Do not install a second running bridge alongside an existing one.

## Updating

Install a newer APK signed by the same publisher over the existing app; saved
settings should remain. Follow release-specific component-update instructions
when runtime assets change. Updating from 0.14.5 or 0.15.0-test1 to 0.15.0 does
not require a firmware flash or replacement of the unchanged readiness-15 runtime.
Restart the bridge to use the updated integration. A differently signed community build cannot replace
the existing APK in place. Remove components and uninstall the old app first
before switching signing identities; that loses app-private settings.

## Troubleshooting

- **OEM privileged service unavailable:** the necessary OEM interface is missing or
  inaccessible. Installing Magisk is not a documented fix for this release.
- **Components not installed:** run Install/Update Components, then Start.
- **Firmware bridge detected:** migrate away from that older integration first;
  this app cannot remove system-partition files for you.
- **No input in the target app:** check Bridge running, select and save the
  app, fully reopen it and verify physical input in Diagnostics.
- **Sticks do not register:** check the Odin's own joystick calibration and
  Diagnostics before changing mappings. Test sticks in a game rather than
  relying on console home-screen behavior.
- **Reboot startup fails:** check the menu switch, do not force-stop the app,
  unlock once after reboot, open it once and try again. OEM/background restrictions can affect startup.
- **No supported mapping integration / unverified version:** share the device
  model, firmware and mapping-version details from Diagnostics after reviewing
  personal information. Do not bypass the checks or force an Odin command layout
  onto a Retroid service.

See [BUILDING.md](BUILDING.md) for source builds,
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for implementation limits, and
[CHANGELOG.md](CHANGELOG.md) for this release's history.

## APK verification

SHA-256:
`657F6201CE4E6239BCF256C7FA16D26F1D0743FAA5271D12B71492FDF20830CA`

Signing certificate SHA-256:
`e0ffee3b7f74a94a560c13284124e251d54470a173bb23de34e5b7c36256ed59`

## License

GPL-3.0-only. You can use and modify DS4 Bridge; distributed derivatives must
remain under the GPL and include corresponding source as required by the
license. Private modifications do not have to be published. See [LICENSE](LICENSE)
and [LICENSE-NOTICE.md](LICENSE-NOTICE.md). Private signing keys are never included.
