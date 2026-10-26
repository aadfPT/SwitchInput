$env:JAVA_HOME    = "$env:USERPROFILE\scoop\apps\temurin17-jdk\current"
$env:ANDROID_HOME = "$env:USERPROFILE\scoop\apps\android-clt\current"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:ANDROID_HOME\platform-tools;$env:USERPROFILE\scoop\shims;$env:PATH"
