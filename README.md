# SourcesSwitcher – TV inputs as launcher apps

Fire OS app for a rooted Fire TV Stick. It turns each input configured under
Settings > Equipment Control into its own app in the launcher (Projectivy, Fire TV home, …).
Opening one does what "Alexa, change to HDMI 2" does, without the voice round trip, and the
app closes itself so the launcher is back on screen.

## How it works
`com.amazon.tv.devicecontrol` (DCS) receives Alexa's cloud directives as a broadcast
(`com.amazon.speech.SmartCloud_Request`). The app injects the same broadcast as root
(`su -mm -c am broadcast …`) with an `Alexa.InputController / SelectInput` directive for the
TV endpoint. The input list, the TV endpoint id and the device id are read from DCS's own
SQLite database. Details: [re/NOTES.md](re/NOTES.md).

Launcher shortcuts are separate tiny packages, one per input type Fire TV knows
(48, from DCS's `Definitions.ActivityType`): Projectivy and the Fire TV launcher show one
card per *package*, so activity-aliases inside the main app never show up as their own TV
apps. `tools/gen_inputs.py` generates labels and banner/icon PNGs; `tools/build_stubs.py`
builds a ~20 KB APK per type (shared `tools/stub/Trampoline.java`, own package name
`pt.aadf.sourcesswitcher.input.<type>`, label, banner, and a meta-data entry naming the
input) and drops them in `app/src/main/assets/stubs`. Adding a shortcut on the main screen
installs the stub as root (`pm install`), removing it uninstalls the package. A stub's only
activity forwards the input to the main app's windowless `SwitchActivity`, which sends the
directive and finishes, so the launcher stays on screen.

## Build / deploy (Windows, scoop toolchain)
```powershell
.\tools\deploy.ps1          # gradle assembleDebug + adb install -r + open the app
.\tools\deploy.ps1 -NoLaunch
python -I tools\gen_inputs.py   # only after editing the type list / palette in the script
python -I tools/build_stubs.py  # deploy.ps1 runs this; needs build-tools 34 + ~/.android/debug.keystore
```
`tools/env.ps1` puts the JDK/SDK on PATH, `tools/fire.ps1` wraps adb for 192.168.1.141:5555.
Reinstalling with `-r` keeps the enabled shortcuts and Magisk's root grant.

## On the stick
- **Switch Input** (the app): a card per input. Select = add/remove its launcher shortcut,
  hold = switch to it now. The left column shows how many shortcuts are on the launcher.
- Each shortcut (HDMI 1, PlayStation, Fire TV, …) is a normal TV app in the launcher with its
  own banner. Open it: toast "Switching to …", TV changes input, app is gone.
- Uninstalling the main app leaves installed stubs behind; remove them from the main screen
  first, or `pm uninstall pt.aadf.sourcesswitcher.input.<type>`.

## Layout
- `app/` – Kotlin app (`Switcher`, `InputRepository`, `Root`, `Shortcuts`, `MainActivity`,
  `SwitchActivity`); `res/values/input_types.xml`, `res/drawable-xhdpi/*` are generated and
  `assets/stubs/` is build output (git-ignored)
- `tools/` – env / adb helpers, deploy script, generators (`gen_inputs.py`, `build_stubs.py`,
  `stub/Trampoline.java`), the old probe batch
- `re/` – notes; `re/apk`, `re/jadx`, pulled DB copies are git-ignored
- `diag/` – package dumps and screenshots captured during investigation
