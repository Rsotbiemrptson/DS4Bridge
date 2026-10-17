param(
    [Parameter(Mandatory=$true)][string]$AndroidSdkRoot,
    [Parameter(Mandatory=$true)][string]$NdkRoot,
    [Parameter(Mandatory=$true)][string]$JavaDir,
    [Parameter(Mandatory=$true)][string]$SigningKey,
    [Parameter(Mandatory=$true)][string]$SigningCertificate,
    [string]$BuildToolsDir
)
$ErrorActionPreference='Stop'
$repo=$PSScriptRoot
if (!$BuildToolsDir) { $BuildToolsDir=Join-Path $AndroidSdkRoot 'build-tools/35.0.0' }
$androidJar=Join-Path $AndroidSdkRoot 'platforms/android-35/android.jar'
$llvm=Join-Path $NdkRoot 'toolchains/llvm/prebuilt/windows-x86_64'
$java=Join-Path $JavaDir 'bin/java.exe'
$javac=Join-Path $JavaDir 'bin/javac.exe'
$aapt=Join-Path $BuildToolsDir 'aapt.exe'
$align=Join-Path $BuildToolsDir 'zipalign.exe'
$d8=Join-Path $BuildToolsDir 'lib/d8.jar'
$signer=Join-Path $BuildToolsDir 'lib/apksigner.jar'
$clang=Join-Path $llvm 'bin/clang.exe'
foreach ($file in @($androidJar,$java,$javac,$aapt,$align,$d8,$signer,$clang,$SigningKey,$SigningCertificate)) {
    if (!(Test-Path -LiteralPath $file -PathType Leaf)) { throw "Missing build input: $file" }
}
[xml]$manifest=Get-Content -LiteralPath (Join-Path $repo 'helper/AndroidManifest.xml') -Raw
$version=$manifest.manifest.GetAttribute('versionName','http://schemas.android.com/apk/res/android')
$build=Join-Path $repo ('build/'+$version+'-'+[guid]::NewGuid().ToString('N'))
$classes=Join-Path $build 'classes'
$dex=Join-Path $build 'dex'
$assets=Join-Path $repo 'helper/assets/runtime'
New-Item -ItemType Directory -Force -Path $classes,$dex,$assets | Out-Null
& $clang --target=aarch64-linux-android26 "--sysroot=$(Join-Path $llvm 'sysroot')" -O2 -Wall -Wextra -Werror (Join-Path $repo 'odin_ds4_bridge.c') -o (Join-Path $assets 'odin_ds4_bridge')
if ($LASTEXITCODE -ne 0) { throw 'Native compilation failed' }
foreach ($name in @('start.sh','stop.sh','supervisor.sh')) {
    Copy-Item -LiteralPath (Join-Path $repo "app-runtime/$name") -Destination (Join-Path $assets $name) -Force
}
$sources=Get-ChildItem (Join-Path $repo 'helper/src') -Recurse -Filter '*.java' | ForEach-Object FullName
& $javac -encoding UTF-8 -source 8 -target 8 -cp $androidJar -d $classes $sources
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed' }
$compiled=Get-ChildItem $classes -Recurse -Filter '*.class' | ForEach-Object FullName
& $java -cp $d8 com.android.tools.r8.D8 --min-api 26 --lib $androidJar --output $dex $compiled
if ($LASTEXITCODE -ne 0) { throw 'DEX compilation failed' }
$unsigned=Join-Path $build 'unsigned.apk'
$aligned=Join-Path $build 'aligned.apk'
$signed=Join-Path $build "DS4-Bridge-$version.apk"
& $aapt package -f -M (Join-Path $repo 'helper/AndroidManifest.xml') -S (Join-Path $repo 'helper/res') -A (Join-Path $repo 'helper/assets') -I $androidJar -F $unsigned
if ($LASTEXITCODE -ne 0) { throw 'Resource packaging failed' }
Push-Location $dex
try { & $aapt add $unsigned classes.dex; if ($LASTEXITCODE -ne 0) { throw 'DEX packaging failed' } }
finally { Pop-Location }
& $align -f 4 $unsigned $aligned
if ($LASTEXITCODE -ne 0) { throw 'Alignment failed' }
& $java -jar $signer sign --key $SigningKey --cert $SigningCertificate --out $signed $aligned
if ($LASTEXITCODE -ne 0) { throw 'Signing failed' }
& $java -jar $signer verify $signed
if ($LASTEXITCODE -ne 0) { throw 'Signature verification failed' }
Get-FileHash -LiteralPath $signed -Algorithm SHA256
Write-Output $signed
