# Publishing

Repository: https://github.com/Rsotbiemrptson/Raven-Gamepad
Current final release: v0.16.1.

1. Review the source, compatibility limits, install/removal guides, notices and license. Keep private signing material and device data outside Git.
2. Increase versionName/versionCode, build with the existing official signing identity and verify signature/alignment. Use a new versionCode for official updates.
3. Rebuild the curated source layout and compare payloads. Run the appropriate tests and target-game/reboot acceptance. Record remaining hardware limits.
4. Put the signed APK and SHA256SUMS.txt in `releases/v0.16.1/` (the next release uses its own version folder). Publish to the owner's Final apks with the verified publisher so superseded APKs are archived safely.
5. Commit reviewed source/docs/artifacts, tag that exact commit `v0.16.1`, and push without rewriting existing history.
6. Create the GitHub release from that tag, with the changelog and links to installation/removal. Attach the signed APK, checksum and corresponding-source ZIP. The automatic GitHub source archive is also available.
7. Download the uploaded APK and source ZIP, compare checksums, verify the source tag and confirm the public download links.

GPL-3.0-only applies to source, original scripts and artwork. Distributed derivatives must provide corresponding source under the license. Preserve the release signing identity for official in-place updates; keep its private key backed up securely and never put it in Git or public CI.
