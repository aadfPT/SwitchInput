# SourcesSwitcher – "Change to <input>" as a launcher tile

Fire OS app for a rooted Fire TV Stick. Launching it does what "Alexa, change to HDMI 2"
does, without the voice round trip, and exits. The selectable inputs are the ones already
configured under Settings > Equipment Control on the stick.

## How it works
`com.amazon.tv.devicecontrol` (DCS) receives Alexa's cloud directives as a broadcast
(`com.amazon.speech.SmartCloud_Request`). The app injects the same broadcast as root
(`su -mm -c am broadcast …`) with an `Alexa.InputController / SelectInput` directive for the
TV endpoint. The input list, the TV endpoint id and the device id are read from DCS's own
SQLite database. Details: [re/NOTES.md](re/NOTES.md).

## Build / deploy (Windows, scoop toolchain)
```powershell
.\tools\deploy.ps1          # gradle assembleDebug + adb install -r + open the setup screen
.\tools\deploy.ps1 -NoLaunch
```
`tools/env.ps1` puts the JDK/SDK on PATH, `tools/fire.ps1` wraps adb for 192.168.1.141:5555.
Reinstalling with `-r` keeps the saved configuration (same signing key, same uid, so
Magisk's grant survives too).

## On the stick
- **Switch Input – Setup**: pick the input; "Test now" sends it immediately.
- **Switch Input** tile (Projectivy / any launcher): sends the saved input, shows a toast, exits.
  Launch it with nothing configured and it opens Setup.
- First run asks Magisk for root once.

## Layout
- `app/` – Kotlin app (`Switcher`, `InputRepository`, `Root`, `MainActivity`, `ConfigActivity`)
- `tools/` – env / adb helpers, deploy script, the old probe batch
- `re/` – notes; `re/apk`, `re/jadx`, pulled DB copies are git-ignored
- `diag/` – package dumps and screenshots captured during investigation
