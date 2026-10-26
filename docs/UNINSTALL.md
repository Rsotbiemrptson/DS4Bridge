# Remove Raven Gamepad completely

Do these steps **before uninstalling the APK**:

1. Open Raven Gamepad, then **three-dot menu > Settings > Bridge controls**.
2. Expand **Advanced** and tap **Uninstall Bridge Components**.
3. Confirm **Uninstall**, wait for completion, and verify **Status: Components not installed**.
4. Uninstall Raven Gamepad using Android's app settings or launcher.

Component removal stops the standalone bridge, removes `/data/local/ds4-bridge-app`, disables automatic startup and verifies cleanup of the saved Touch Mapping exclusion integration. App choices and mappings remain until APK uninstall. If removal fails, keep the app installed and review the error or Diagnostics rather than assuming cleanup succeeded.

Android APK uninstall alone cannot remove that external runtime folder. If you uninstalled the APK first, reinstall an official same-signed APK and run the component-removal steps. A firmware-baked bridge or legacy script installation is outside this action's scope. Downloaded APKs, ZIPs or extracted script kits in shared storage are not deleted automatically.
