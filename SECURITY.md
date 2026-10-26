# Security and sensitive data

Raven Gamepad executes privileged scripts using a verified OEM service on supported devices and reads physical controller events. Install builds you trust. The observed OEM Binder protocol is not a portable Android root API, and arbitrary service probing is deliberately avoided.

Do not attach account/console credentials, signing keys, full device backups, extracted OEM apps or unredacted device logs to public issues. Diagnostics can include build/device details and installed package identifiers; review exports before sharing.

Report the app version, device model/firmware, relevant feature and minimal reproduction. Use GitHub private vulnerability reporting if enabled; otherwise ask the maintainer for a private route before disclosing sensitive details. The publisher's private signing key is not included.
