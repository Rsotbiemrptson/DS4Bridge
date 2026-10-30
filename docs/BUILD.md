# Build from source (Windows)

Use PowerShell 7, JDK 17 or later, Android SDK platform 35, Build Tools 35.0.0 and Android NDK r30. Native output is arm64-v8a/API 26; the APK targets API 33. These SDK declarations do not establish hardware support on other devices.

SDK/NDK/JDK downloads and the publisher's signing key are not included. Supply a PKCS#8 key and matching X.509 certificate accepted by apksigner. A contributor-signed build cannot update the official APK in place; keep your key outside the repository.

```powershell
./build.ps1 `
  -AndroidSdkRoot 'D:/Android/Sdk' `
  -NdkRoot 'D:/Android/android-ndk-r30' `
  -JavaDir 'D:/Java/jdk-17' `
  -SigningKey 'D:/PrivateKeys/gamepad.pk8' `
  -SigningCertificate 'D:/PrivateKeys/gamepad-cert.pem'
```

Use the actual tool directories. Pass `-BuildToolsDir` if Build Tools are installed separately, and optionally `-OutputDirectory`. By default, the signed APK goes to `dist/Raven-Gamepad-0.16.3.apk`. Each build uses a fresh ignored `.build/` directory and does not replace the curated release APK.

The script compiles native C, stages the runtime scripts and GPL text in temporary assets, generates the build date in Eastern time, compiles Java, packages, aligns, signs and verifies the APK. It neither installs on a device nor uploads anything.

`-PublishFinal -FinalApkDirectory 'D:/Final apks'` additionally verifies/stages the new APK and archives superseded APKs of the same Android package to that folder's Archive subfolder. It refuses downgrades and preserves unrelated apps. Do not use that flag just to make a development build.

## Source and tests

- `src/`, `res/`, `AndroidManifest.xml`: Android app.
- `native/odin_ds4_bridge.c`: native reader and virtual pad.
- `runtime/`: privileged component scripts.
- `scripts/publish-apk.ps1`: verified final publication/archive helper.
- `tests/`: host, native and Android instrumentation source.

Run the 20 host adapter checks, without a device/root:

```powershell
./tests/run-integration-host-tests.ps1 -JavaDir 'D:/Java/jdk-17'
```

Android instrumentation sources and manifests are included for development; the main build script builds the app only. Build a same-signed helper APK targeting `com.odin2.ds4bridgehelper` to run them. Device scripts must be reviewed for your local tools and device, and user mappings preserved. `RuntimeSmoke.java` is a historical firmware-only test and is not a standalone-app acceptance test. `SettingsSmoke.java` performs real stop/start/restart operations; UI-only acceptance is in `RavenUiSmoke.java`.

The app's source at the release tag corresponds to its signed APK. APK ZIP/signature bytes may vary with packaging timestamps; compare extracted payloads when validating a local rebuild. Keep APKs, licenses and complete corresponding source together when distributing a modified GPL build.
