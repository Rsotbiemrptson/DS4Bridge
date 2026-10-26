param(
    [Parameter(Mandatory)][string]$Apk,
    [Parameter(Mandatory)][string]$FinalDirectory,
    [Parameter(Mandatory)][string]$BuildToolsDirectory,
    [Parameter(Mandatory)][string]$JavaHome
)
$ErrorActionPreference = 'Stop'

$Aapt = (Join-Path $BuildToolsDirectory 'aapt.exe')
$Java = (Join-Path $JavaHome 'bin/java.exe')
$Signer = (Join-Path $BuildToolsDirectory 'lib/apksigner.jar')
function Read-Apk([string]$Path) {
    $Badging = & $Aapt dump badging $Path 2>&1
    if ($LASTEXITCODE -ne 0) { throw "Cannot read APK metadata: $Path" }
    $Match = [regex]::Match(($Badging -join "`n"), "package: name='([^']+)' versionCode='(\d+)'")
    if (!$Match.Success) { throw "Missing package/version metadata: $Path" }
    [pscustomobject]@{ Path=$Path; Package=$Match.Groups[1].Value; Version=[long]$Match.Groups[2].Value }
}
$Source = (Resolve-Path -LiteralPath $Apk).Path
& $Java -jar $Signer verify $Source
if ($LASTEXITCODE -ne 0) { throw 'New APK signature verification failed; releases were not changed.' }
$New = Read-Apk $Source
New-Item -ItemType Directory -Force -Path $FinalDirectory | Out-Null
$FinalDirectory = (Resolve-Path -LiteralPath $FinalDirectory).Path
$Archive = Join-Path $FinalDirectory 'Archive'
$Destination = Join-Path $FinalDirectory ([IO.Path]::GetFileName($Source))
if ($Source -eq $Destination) { throw 'Publish from project Outputs, not from the final APK folder.' }
$Previous = @(Get-ChildItem -LiteralPath $FinalDirectory -File -Filter '*.apk' | ForEach-Object {
    try { Read-Apk $_.FullName } catch { Write-Warning "Leaving unrecognized APK untouched: $($_.Exception.Message)" }
} | Where-Object Package -eq $New.Package)
if (@($Previous | Where-Object Version -gt $New.Version).Count) {
    throw 'A newer version is already published. Refusing to replace it with an older build.'
}
if ((Test-Path -LiteralPath $Destination) -and $Destination -notin $Previous.Path) {
    throw 'The destination belongs to another application; refusing to overwrite it.'
}
# Verify the staged copy before moving any existing release.
$Stage = Join-Path $FinalDirectory ('.publish-' + [guid]::NewGuid().ToString('N') + '.pending')
Copy-Item -LiteralPath $Source -Destination $Stage
$ExpectedHash = (Get-FileHash -LiteralPath $Source -Algorithm SHA256).Hash
if ((Get-FileHash -LiteralPath $Stage -Algorithm SHA256).Hash -ne $ExpectedHash) {
    throw 'Published copy did not match the built APK; existing releases were not moved.'
}
New-Item -ItemType Directory -Force -Path $Archive | Out-Null
foreach ($Old in $Previous) {
    if ($Old.Path -eq $Destination -and (Get-FileHash -LiteralPath $Old.Path -Algorithm SHA256).Hash -eq $ExpectedHash) { continue }
    $ArchivedPath = Join-Path $Archive ([IO.Path]::GetFileName($Old.Path))
    if (Test-Path -LiteralPath $ArchivedPath) {
        $ConflictDirectory = Join-Path $Archive ((Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N'))
        New-Item -ItemType Directory -Path $ConflictDirectory | Out-Null
        $ArchivedPath = Join-Path $ConflictDirectory ([IO.Path]::GetFileName($Old.Path))
    }
    # These are individual files enumerated directly from FinalDirectory.
    Move-Item -LiteralPath $Old.Path -Destination $ArchivedPath
    if (!(Test-Path -LiteralPath $ArchivedPath) -or (Test-Path -LiteralPath $Old.Path)) { throw 'Archive move verification failed.' }
    Write-Output "Archived: $ArchivedPath"
}
Move-Item -LiteralPath $Stage -Destination $Destination -Force
if ((Get-FileHash -LiteralPath $Destination -Algorithm SHA256).Hash -ne $ExpectedHash) { throw 'Final APK verification failed.' }
$Remaining = @(Get-ChildItem -LiteralPath $FinalDirectory -File -Filter '*.apk' | ForEach-Object { try { Read-Apk $_.FullName } catch { Write-Warning "Leaving unrecognized APK untouched: $($_.Exception.Message)" } } | Where-Object Package -eq $New.Package)
if ($Remaining.Count -ne 1 -or $Remaining[0].Version -ne $New.Version) { throw 'Expected exactly one current APK for this application.' }
Write-Output "Published: $Destination"
