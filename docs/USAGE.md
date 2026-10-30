# Usage and limits

## Apps and navigation

Only installed apps are listed, with their icon, name and package ID. Check the apps that should receive controller input, then **Save selected apps**. The three-dot menu contains **Show system apps**, **Settings**, **About**, in that order.

Apps is the only main tab. Settings reveals Appearance, Bridge controls, Controller mapping and Diagnostics beneath it in landscape (bottom tabs in portrait). Apps stays available and hides the settings tabs when selected. The same sidebar/header stay attached while only the right pane changes.

## Appearance

Choose Follow system, Light or Dark. Accent color opens a hue/saturation wheel, RGB sliders, hex input and six circular presets: orange/red/green for the other Raven apps, then purple/yellow/white. **Apply** saves, **Cancel** preserves the saved color, and **Default** restores Gamepad blue `#0070CC`. Text contrast adjusts for readability. Sidebar/card colors follow Raven Backup; the icon has a solid background.

## Bridge controls

The main card shows the actual state as **Status: ...**, Start/Stop/Restart and **Start bridge after reboot**. Startup waits for the first unlock; do not force-stop the app if you expect its boot receiver to run.

Advanced starts collapsed. Expand it for Install/Update Components, Refresh bridge status, View technical status, Test virtual gamepad and Uninstall Bridge Components. Each action has an explanation. Scroll for lower rows when needed. Testing the virtual gamepad sends a short input; avoid doing that during an important game action.

By default, app selection controls forwarding. The virtual Sony controller remains registered while the bridge runs, even outside selected apps. The supervisor still checks mode approximately once per second. In selected-app mode it also queries foreground/selection state; all-app and paused modes skip those queries. This is not an accessibility watcher, and battery impact has not been measured.

## Quick Settings controller style

On the verified Odin 2 / Android 13, add the Raven controller style tile through Quick Settings edit. Its displayed title is Raven Gamepad. Each tap advances:

| Mode | Behavior |
| --- | --- |
| Odin | Stock Odin layout, bridge forwarding only to saved selected apps. |
| Xbox | Stock Xbox layout, bridge forwarding only to saved selected apps. |
| Raven Gamepad | Virtual DS4 forwarding to all apps; saved app list remains intact. |
| Disconnected | Stock controller buttons disabled and forwarding paused. Touchscreen remains usable; native bridge and supervisor stay running. |

The cycle returns to Odin after Disconnected, reconnecting the source automatically; no manual bridge Start is required. Enable Start bridge after reboot if automatic runtime startup is wanted. The current global/per-app preference and stock settings persist. After unlock the existing boot launcher starts the runtime; a disconnected controller stays paused.

Mode changes require revision-17 components. If the tile asks for an update, use Install/Update Components in Bridge controls. Style switching is intentionally unavailable on unverified models. A virtual gamepad is still enumerated during Disconnected; this mode suppresses controller input while keeping the bridge ready.

## Controller mapping and diagnostics

Hold a button row to learn, then press the physical input you want. Cancel exits learning. Reusing an input clears its previous assignment. Tap an axis to capture its movement; hold it to invert/clear it. **Save and restart bridge** applies the changes. Labels adapt to Odin/Xbox mode and physical face-button positions. Leaving unsaved edits prompts before discarding them; drafts survive screen recreation.

Diagnostics shows the selected physical controller and live inputs. Technical details include detected mapping integration, version, exclusion owner and last action. Bridge checks use the verified read-only interface. Review reports before sharing; they may include build information and package identifiers.

## Compatibility

- **Odin 2:** stock 318/Android 13 and a stock-318-based custom build have hardware testing. Odin/Xbox modes are supported by the existing mapping logic; the latest UI/reboot acceptance ran on the connected Odin 2.
- **Odin 3:** the inspected `com.odin.mapping` version `3.0(20251128_112846)` has an adapter; hardware validation is still required.
- **Retroid Pocket 6:** inspected `com.rp.mapping` version `3.0(20260615_164638)` has a pinned read-only diagnostic path. Bridging, setup and exclusion writes remain disabled pending hardware testing.
- Other models, firmware variants and unknown interfaces are not claimed supported. The same CPU, service name or Run as root option is not sufficient evidence.

## Troubleshooting

- No target input: check Status: Running, select/save the app, fully reopen it and confirm physical input in Diagnostics.
- Sticks missing: check Odin joystick calibration and Diagnostics; test in a game rather than only the console home screen.
- Components missing: expand Advanced, Install/Update Components, then Start.
- Firmware bridge detected: remove/migrate the older integration separately; standalone removal cannot edit system partitions.
- Startup missing: enable the switch under Bridge controls, unlock once after reboot, avoid force-stop and check OEM background restrictions.
- Unknown mapping version or missing privileged service: collect a reviewed Diagnostics report; do not force another device's protocol or guess Binder commands.
