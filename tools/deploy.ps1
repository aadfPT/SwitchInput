param([switch]$NoLaunch, [switch]$SkipBuild, [switch]$Release, [switch]$Replace)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
. "$PSScriptRoot\env.ps1"
. "$PSScriptRoot\fire.ps1"

$variant = if ($Release) { "release" } else { "debug" }

if (-not $SkipBuild) {
    Push-Location $root
    try {
        & python -I tools\build_stubs.py; if ($LASTEXITCODE -ne 0) { throw "build_stubs failed ($LASTEXITCODE)" }
        & .\gradlew.bat "assemble$variant" --no-daemon -q; if ($LASTEXITCODE -ne 0) { throw "gradle failed ($LASTEXITCODE)" }
    }
    finally { Pop-Location }
}
$apk = Join-Path $root "app\build\outputs\apk\$variant\app-$variant.apk"
if (-not (Test-Path $apk)) { throw "APK not found: $apk" }

Connect-Fire | Out-Null
if ($Replace) { adb -s $script:FireAddr uninstall pt.aadf.sourcesswitcher | Out-Null }
adb -s $script:FireAddr install -r -t $apk
if ($LASTEXITCODE -ne 0) { throw "adb install failed (use -Replace when the signing key changed)" }
if (-not $NoLaunch) {
    adb -s $script:FireAddr shell am start -n pt.aadf.sourcesswitcher/.MainActivity | Out-Null
}
Write-Host "Deployed $(Split-Path -Leaf $apk) to $script:FireAddr"
