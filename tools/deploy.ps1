# Build the debug APK, (re)install it on the Fire Stick and open the setup screen.
#   .\tools\deploy.ps1            -> build + install -r + launch setup
#   .\tools\deploy.ps1 -NoLaunch  -> build + install only
#   .\tools\deploy.ps1 -SkipBuild -> install the existing APK
param([switch]$NoLaunch, [switch]$SkipBuild)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
. "$PSScriptRoot\env.ps1"
. "$PSScriptRoot\fire.ps1"

if (-not $SkipBuild) {
    Push-Location $root
    try { & .\gradlew.bat assembleDebug --no-daemon -q; if ($LASTEXITCODE -ne 0) { throw "gradle failed ($LASTEXITCODE)" } }
    finally { Pop-Location }
}
$apk = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apk)) { throw "APK not found: $apk" }

Connect-Fire | Out-Null
# -r keeps the app's saved configuration across reinstalls; -t allows debug builds.
adb -s $script:FireAddr install -r -t $apk
if ($LASTEXITCODE -ne 0) { throw "adb install failed" }
if (-not $NoLaunch) {
    adb -s $script:FireAddr shell am start -n pt.aadf.sourcesswitcher/.ConfigActivity | Out-Null
}
Write-Host "Deployed $(Split-Path -Leaf $apk) to $script:FireAddr"
