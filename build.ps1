# SPDX-License-Identifier: GPL-3.0-only
[CmdletBinding()]
param(
 [Parameter(Mandatory)][string]$AndroidSdkRoot,
 [Parameter(Mandatory)][string]$NdkRoot,
 [Parameter(Mandatory)][string]$JavaDir,
 [Parameter(Mandatory)][string]$SigningKey,
 [Parameter(Mandatory)][string]$SigningCertificate,
 [string]$BuildToolsDir,
 [string]$OutputDirectory,
 [switch]$PublishFinal,
 [string]$FinalApkDirectory
)
$ErrorActionPreference='Stop'
$repo=$PSScriptRoot
if(!$BuildToolsDir){$BuildToolsDir=Join-Path $AndroidSdkRoot 'build-tools/35.0.0'}
if(!$OutputDirectory){$OutputDirectory=Join-Path $repo 'dist'}
if($PublishFinal -and !$FinalApkDirectory){throw 'Pass -FinalApkDirectory when publishing.'}
$androidJar=Join-Path $AndroidSdkRoot 'platforms/android-35/android.jar'
$llvm=Join-Path $NdkRoot 'toolchains/llvm/prebuilt/windows-x86_64'
$java=Join-Path $JavaDir 'bin/java.exe'
$javac=Join-Path $JavaDir 'bin/javac.exe'
$aapt=Join-Path $BuildToolsDir 'aapt.exe'
$align=Join-Path $BuildToolsDir 'zipalign.exe'
$d8=Join-Path $BuildToolsDir 'lib/d8.jar'
$signer=Join-Path $BuildToolsDir 'lib/apksigner.jar'
$clang=Join-Path $llvm 'bin/clang.exe'
foreach($file in @($androidJar,$java,$javac,$aapt,$align,$d8,$signer,$clang,$SigningKey,$SigningCertificate)){
 if(!(Test-Path -LiteralPath $file -PathType Leaf)){throw "Missing build input: $file"}
}
function Run-Tool([string]$Program,[string[]]$ToolArguments){& $Program @ToolArguments;if($LASTEXITCODE -ne 0){throw "Build tool failed: $([IO.Path]::GetFileName($Program)) ($LASTEXITCODE)"}}
[xml]$manifest=Get-Content -LiteralPath (Join-Path $repo 'AndroidManifest.xml') -Raw
$version=$manifest.manifest.GetAttribute('versionName','http://schemas.android.com/apk/res/android')
$build=Join-Path $repo ('.build/'+$version+'-'+[guid]::NewGuid().ToString('N'))
$classes=Join-Path $build 'classes';$dex=Join-Path $build 'dex';$assets=Join-Path $build 'assets';$runtime=Join-Path $assets 'runtime'
New-Item -ItemType Directory -Force -Path $classes,$dex,$runtime,$OutputDirectory | Out-Null
Run-Tool $clang @('--target=aarch64-linux-android26',"--sysroot=$(Join-Path $llvm 'sysroot')",'-O2','-Wall','-Wextra','-Werror',(Join-Path $repo 'native/odin_ds4_bridge.c'),'-o',(Join-Path $runtime 'odin_ds4_bridge'))
foreach($name in @('start.sh','stop.sh','supervisor.sh')){Copy-Item -LiteralPath (Join-Path $repo "runtime/$name") -Destination (Join-Path $runtime $name)}
Copy-Item -LiteralPath (Join-Path $repo 'LICENSE') -Destination (Join-Path $assets 'LICENSE.txt')
$date=[TimeZoneInfo]::ConvertTimeFromUtc([DateTime]::UtcNow,[TimeZoneInfo]::FindSystemTimeZoneById('Eastern Standard Time')).ToString('yyyy-MM-dd')
$generated=Join-Path $build 'BuildInfo.java'
"package com.odin2.ds4bridgehelper; final class BuildInfo { static final String BUILD_DATE = `"$date`"; }" | Set-Content -LiteralPath $generated -Encoding utf8
$sources=@(Get-ChildItem (Join-Path $repo 'src') -Recurse -Filter '*.java' | Where-Object Name -ne 'BuildInfo.java' | ForEach-Object FullName)+@($generated)
Run-Tool $javac (@('-encoding','UTF-8','-source','8','-target','8','-cp',$androidJar,'-d',$classes)+$sources)
$compiled=@(Get-ChildItem $classes -Recurse -Filter '*.class' | ForEach-Object FullName)
Run-Tool $java (@('-cp',$d8,'com.android.tools.r8.D8','--min-api','26','--lib',$androidJar,'--output',$dex)+$compiled)
$unsigned=Join-Path $build 'unsigned.apk';$aligned=Join-Path $build 'aligned.apk';$signed=Join-Path $OutputDirectory "Raven-Gamepad-$version.apk"
Run-Tool $aapt @('package','-f','-M',(Join-Path $repo 'AndroidManifest.xml'),'-S',(Join-Path $repo 'res'),'-A',$assets,'-I',$androidJar,'-F',$unsigned)
Push-Location $dex
try{Run-Tool $aapt @('add',$unsigned,'classes.dex')}finally{Pop-Location}
Run-Tool $align @('-f','4',$unsigned,$aligned)
Run-Tool $java @('-jar',$signer,'sign','--key',$SigningKey,'--cert',$SigningCertificate,'--out',$signed,$aligned)
Run-Tool $java @('-jar',$signer,'verify',$signed)
Run-Tool $align @('-c','4',$signed)
Get-FileHash -LiteralPath $signed -Algorithm SHA256
Write-Output $signed
if($PublishFinal){& (Join-Path $repo 'scripts/publish-apk.ps1') -Apk $signed -FinalDirectory $FinalApkDirectory -BuildToolsDirectory $BuildToolsDir -JavaHome $JavaDir}
