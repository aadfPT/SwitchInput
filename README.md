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

Launcher shortcuts are `activity-alias` entries, one per input type Fire TV knows
(48, from DCS's `Definitions.ActivityType`), generated into the manifest by
`tools/gen_inputs.py` together with their labels and banner/icon PNGs. They start disabled;
the main screen enables the ones you pick with `PackageManager.setComponentEnabledSetting`,
which is what makes them appear or disappear in launchers. Every alias targets
`SwitchActivity`, a windowless activity that sends the directive and finishes.

## Build / deploy (Windows, scoop toolchain)
```powershell
.\tools\deploy.ps1          # gradle assembleDebug + adb install -r + open the app
.\tools\deploy.ps1 -NoLaunch
python -I tools\gen_inputs.py   # only after editing the type list / palette in the script
```
`tools/env.ps1` puts the JDK/SDK on PATH, `tools/fire.ps1` wraps adb for 192.168.1.141:5555.
Reinstalling with `-r` keeps the enabled shortcuts and Magisk's root grant.

## On the stick
- **Switch Input** (the app): a card per input. Select = add/remove its launcher shortcut,
  hold = switch to it now. The left column shows how many shortcuts are on the launcher.
- Each shortcut (HDMI 1, PlayStation, Fire TV, …) is a normal launcher entry with its own
  banner. Open it: toast "Switching to …", TV changes input, app is gone.
- Projectivy caches app banners; if a shortcut shows an old image, clear Projectivy's cache
  or re-add the shortcut.

## Layout
- `app/` – Kotlin app (`Switcher`, `InputRepository`, `Root`, `Shortcuts`, `MainActivity`,
  `SwitchActivity`); `res/values/input_types.xml`, `res/drawable-xhdpi/*` and the alias block
  of the manifest are generated
- `tools/` – env / adb helpers, deploy script, generator, the old probe batch
- `re/` – notes; `re/apk`, `re/jadx`, pulled DB copies are git-ignored
- `diag/` – package dumps and screenshots captured during investigation
