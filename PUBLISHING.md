# Owner's publishing checklist

Repository: https://github.com/Rsotbiemrptson/DS4Bridge
Current release: v0.14.5. Follow this checklist for future releases.

1. Keep the selected GPL-3.0-only LICENSE and LICENSE-NOTICE.md. Review their
   terms and preserve them with the source and release.
2. Review README, compatibility limits, source and included APK. Only 0.14.5
   belongs in `releases/`; never include signing keys, build directories, OEM
   extracts, firmware images, device reports or your private workspace notes.
3. Commit the reviewed source/docs changes to this repository, including the
   dotfiles `.gitignore` and `.gitattributes`. Preserve the license and do not
   force-push over existing history.
4. Create release tag `v0.14.5`, title `DS4 Bridge 0.14.5`, and attach
   `releases/DS4-Bridge-0.14.5.apk` and `releases/SHA256SUMS.txt`.
5. Use the current CHANGELOG section as the release notes and link to README's
   install/removal instructions. State that Odin 3/Retroid support is unverified.
6. Download the uploaded APK and compare its SHA-256 with SHA256SUMS.txt.

Do not change the signing identity for future official updates. Keep the
publisher's private key backed up offline, never in Git history or public CI.
The public source can be built with a contributor's separate key; that APK
will not install as an in-place update over the publisher-signed release.
