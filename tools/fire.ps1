$script:FireAddr = if ($env:FIRE_TV_ADDR) { $env:FIRE_TV_ADDR } else { "192.168.1.141:5555" }

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
