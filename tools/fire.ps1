# Helper for talking to the Fire Stick over wireless adb.
#   . .\tools\fire.ps1          -> dot-source to get the functions
#   Connect-Fire                -> adb connect + wait for device
#   fsh "getprop ro.product.model"   -> adb shell on the stick
#   fsu "id"                    -> same, but through su (root)
$script:FireAddr = "192.168.1.141:5555"

function Connect-Fire {
    adb start-server | Out-Null
    $out = adb connect $script:FireAddr 2>&1
    Write-Host $out
    adb -s $script:FireAddr wait-for-device
    adb -s $script:FireAddr get-state
}

function fsh([string]$cmd) {
    adb -s $script:FireAddr shell $cmd
}

function fsu([string]$cmd) {
    # Fire OS su accepts "su -c" (Magisk / SuperSU style). Quote once for the remote shell.
    adb -s $script:FireAddr shell "su -c '$cmd'"
}
