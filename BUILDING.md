# Build from source (Windows)

Requirements: PowerShell 7, a JDK (17 or later), Android SDK platform 35,
Android SDK Build Tools 35.0.0 and Android NDK r30. The native output targets
arm64-v8a, Android API 26; the APK targets API 33 and requires API 26 or later.
Those API declarations do not imply support on non-AYN devices.

The repository does not include SDK/NDK/JDK downloads or private signing keys.
Install the tools separately and keep signing material outside the repository.
You need a PKCS#8 private key and its matching X.509 certificate, accepted by
Android's apksigner. A new signing identity produces your own fork, not an
update accepted over the published APK. Never commit private keys or passwords.

Example (replace the paths with your installed tools and your own key):

```powershell
./build.ps1 `
  -AndroidSdkRoot 'D:/Android/Sdk' `
  -NdkRoot 'D:/Android/android-ndk-r30' `
  -JavaDir 'D:/Java/jdk-17' `
  -SigningKey 'D:/PrivateKeys/ds4.pk8' `
  -SigningCertificate 'D:/PrivateKeys/ds4-cert.pem'
```

Use the actual installed NDK r30 directory, not necessarily the example.
If Build Tools are installed separately, pass `-BuildToolsDir` explicitly.

The script compiles the native bridge, copies runtime scripts into generated
APK assets, compiles the Java UI, packages, aligns and signs the APK, then
verifies its signature. Each invocation uses a fresh directory under `build/`.
The output path and SHA-256 are printed; the bundled release APK is not replaced.
No files are installed on a device and nothing is uploaded automatically.

## Source layout

Run the 20 host-only adapter checks (no device or root needed):

```powershell
./tests/run-integration-host-tests.ps1 -JavaDir 'D:/Java/jdk-17'
```

- `helper/src/`: Android UI, mappings, diagnostics, settings provider and boot.
- `helper/res/`, `helper/AndroidManifest.xml`: icon and app declarations.
- `odin_ds4_bridge.c`: physical-input reader and virtual controller.
- `app-runtime/`: privileged runtime launcher, supervisor and stop scripts.
- `tests/`: native mapping tests and Android instrumentation source/manifests.
- `releases/`: tested APK and checksum file, not private signing keys.

The test sources are supplied for development. Android instrumentation needs
its own same-signed test APK targeting `com.odin2.ds4bridgehelper`; this portable
build script builds only the main app. `RuntimeSmoke.java` is a historical
firmware-only test, not a valid standalone acceptance test. Native mapping tests
include the bridge C source and can be compiled with the matching NDK compiler.
Run device tests on a supported spare device and preserve user mappings first.

Manual acceptance: fresh install, component installation, selection/save,
Remote Play gameplay, both controller modes, mapping, restart, reboot startup,
and component removal followed by APK uninstall. Do not claim a new model is
supported from an offline firmware inspection alone.
