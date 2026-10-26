param([Parameter(Mandatory=$true)][string]$JavaDir)
$ErrorActionPreference='Stop'
$repo=Split-Path $PSScriptRoot -Parent
$classes=Join-Path $repo ('.build/adapter-tests-'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $classes -Force | Out-Null
& (Join-Path $JavaDir 'bin/javac.exe') -encoding UTF-8 -source 8 -target 8 -d $classes (Join-Path $repo 'src/com/odin2/ds4bridgehelper/MappingProfiles.java') (Join-Path $PSScriptRoot 'host/MappingProfilesTest.java')
if($LASTEXITCODE -ne 0){throw 'Adapter tests failed to compile'}
& (Join-Path $JavaDir 'bin/java.exe') -cp $classes com.odin2.ds4bridgehelper.MappingProfilesTest
if($LASTEXITCODE -ne 0){throw 'Adapter tests failed'}
