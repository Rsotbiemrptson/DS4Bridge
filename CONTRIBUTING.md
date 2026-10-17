# Contributing and reporting problems

The project uses GPL-3.0-only. Submit contributions under that license and
preserve third-party notices. Do not contribute code you do not have permission
to share under these terms. See LICENSE and LICENSE-NOTICE.md.

For a bug report, include device model, actual Android/OEM firmware version,
DS4 Bridge version, controller mode, installation type (standalone or older
firmware/script integration), exact error, steps and whether reboot startup
works. Explain what responds in Diagnostics and in the target app. Review any
diagnostic report for personal identifiers/package lists before attaching it.
Do not share signing keys, console credentials, account tokens or complete
device backups. Do not publish extracted OEM applications with a report.

New-device support needs live physical input tests and confirmation of a
permitted virtual-controller/privileged interface. Prefer explicit supported
adapters and bounded commands over guessing Binder transactions. Preserve
the command-length guard and prevent grabbing another virtual input device
as physical input. Test both controller modes, mappings, release/reset behavior,
shutdown and removal. UI changes must remain usable in landscape and portrait.
