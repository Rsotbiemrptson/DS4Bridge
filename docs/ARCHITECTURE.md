# Architecture and safety notes

Raven Gamepad (formerly DS4 Bridge) is not a Bluetooth pairing utility. A native process reads physical
Linux input events and creates an Android-visible virtual Sony pad through
uinput (VID 054c, PID 09cc), named Raven Gamepad. Current known physical modes are vendor 2020,
product 0111 (Odin) and 0112 (Xbox). Face buttons use physical positions, not
identical A/B/X/Y names across the two layouts.

The APK uses the known AYN PServerBinder root-command interface to install and
control its runtime. Commands have a 192-byte safety limit because the OEM
service's command handling is constrained. Unknown Binder services are listed
by diagnostics, not invoked in an automatic search for root access.

MappingProfiles keeps component-specific command layouts: Odin 2 legacy uses
33/34, modern Odin 2 and the pinned Odin 3 mapping version use 34/35. Retroid's
inspected RP6 version has the same modern descriptor but uses 37/38 for its
blacklist API. Only its read-only 37 query is enabled in explicit diagnostics;
Retroid mutations/setup/start are disabled. Candidates must be enabled/exported
system apps and known components. Unknown versions/interfaces are not invoked.
The exclusion adapter ID is persisted before writes so removal can use the
same integration even if a reply is lost. Failed cleanup does not fall back to
another service. A failed/timeout Binder operation may have uncertain outcome;
the diagnostic result must be checked, not treated as confirmed cleanup.

Runtime files are staged, hash-checked, promoted to the root-owned
`/data/local/ds4-bridge-app` directory and executed there. The boot receiver
starts the same runtime only when the user enables automatic startup. No
firmware partition patch, boot image patch or Magisk module is installed.

The supervisor asks AYN Touch Mapping to exclude the virtual pad, then starts
the native bridge. Revision 17 reads a read-only selected/all/off mode endpoint once per second. Selected mode queries the foreground app and saved-package provider; all/off modes signal directly without those queries. The virtual pad remains registered while the bridge runs, including outside selected apps and while paused. Global mode does not overwrite the saved selected-app list. The Odin 2 tile writes only the verified stock style settings and keeps the same bridge process running through source disconnect/reconnect. A temporary transition pause prevents forwarding during changes; failed mode changes attempt to restore prior settings. Model/API/interface guards reject unknown devices. Battery impact remains unmeasured. Normal shutdown requests removal of the Touch Mapping
exclusion; abnormal termination may skip that cleanup.

Mappings and selected packages are app-private, device-protected settings;
runtime configuration is also copied outside app-private storage. Ordinary APK
uninstall therefore cannot provide complete cleanup. The current removal flow
must run before uninstall. A root service, script or baked-in firmware bridge
from another installation is outside that removal scope.

Diagnostics can include device/build properties, installed package identifiers,
input devices and privileged-service information. Review reports before sharing
them publicly. This distribution excludes development device logs and extracted
OEM APKs. No PlayStation/Netflix firmware, ROM images or third-party applications
are bundled. Access to the OEM privileged interface is security-sensitive; do
not treat this design as a general-purpose safe command-execution API.

## UI shell

A single top-level activity retains the app list, header and sidebar. Settings tool activities are embedded in the right pane with lifecycle and controller event forwarding. The host asks the mapping pane about unsaved edits before navigation and explicitly stores/restores pane draft state on recreation. This UI redesign does not change the native controller identity, package/provider IDs or runtime protocol.

## Controller name migration

Exclusion creation uses Raven Gamepad. Removal verifies cleanup of both that name and legacy Odin DS4 Bridge Test entries, including duplicates; other names are left intact. Component setup waits for shutdown and owned exclusion cleanup before replacing the root-owned payload. Application package/provider names, runtime paths, executable name and Sony VID/PID remain stable.
