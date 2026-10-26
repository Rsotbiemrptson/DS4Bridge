# Architecture and safety notes

Raven Gamepad (formerly DS4 Bridge) is not a Bluetooth pairing utility. A native process reads physical
Linux input events and creates an Android-visible virtual Sony pad through
uinput (VID 054c, PID 09cc). Current known physical modes are vendor 2020,
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
the native bridge. It queries the foreground app and selected-package provider
approximately once per second and signals forwarding on/off. The virtual pad
remains registered while the bridge runs, including outside selected apps;
app selection gates input forwarding, not virtual-device enumeration. This
release is not an event-driven foreground watcher, and battery impact has not
been quantified. Normal shutdown requests removal of the Touch Mapping
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
