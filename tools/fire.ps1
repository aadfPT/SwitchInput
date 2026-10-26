if (-not $env:FIRE_TV_ADDR) { throw "Set FIRE_TV_ADDR to the stick's adb address, e.g. 192.168.1.50:5555" }
$script:FireAddr = $env:FIRE_TV_ADDR

function Connect-Fire {
    adb start-server | Out-Null
    Write-Host (adb connect $script:FireAddr 2>&1)
    adb -s $script:FireAddr wait-for-device
    adb -s $script:FireAddr get-state
}

function fsh([string]$cmd) {
    adb -s $script:FireAddr shell $cmd
}

function fsu([string]$cmd) {
    adb -s $script:FireAddr shell "su -mm -c '$cmd'"
}
