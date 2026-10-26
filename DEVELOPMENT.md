# Development notes

Everything a developer needs to build, deploy and understand Switch Input. The user-facing
description is in [README.md](README.md).

## How it works

1. **Trigger.** Fire TV's Equipment Control engine, `com.amazon.tv.devicecontrol` (DCS),
   receives Alexa's cloud directives as a broadcast (`com.amazon.speech.SmartCloud_Request`
   to `.csmintegration.DirectiveReceiver`). The app injects the same broadcast as root with an
   `Alexa.InputController / SelectInput` directive for the TV endpoint
   (`Switcher.kt`). The receiver is not exported, hence `su`.
2. **Input list.** DCS keeps the configured inputs ("activities") in its SQLite database
   `/data/data/com.amazon.tv.devicecontrol/databases/device_control_db`. `InputRepository.kt`
   copies it as root and reads the `activities` table plus the TV's endpoint id from
   `data_store`. The `activityType` (HDMI_2, PLAY_STATION, …) is the value the directive carries.
3. **Shortcuts.** Launchers such as Projectivy show one card per *package*, so each input gets
   its own tiny stub package (`pt.aadf.sourcesswitcher.input.<type>`), built by
   `tools/build_stubs.py` for all 48 input types Fire TV defines and shipped in
   `app/src/main/assets/stubs`. Adding a shortcut installs the stub (`pm install` as root),
   removing it uninstalls the package (`Shortcuts.kt`). A stub's only activity
   (`tools/stub/Trampoline.java`) forwards its input type to the main app's windowless
   `SwitchActivity`, which sends the directive and finishes.

Details of the reverse engineering, including the dead ends, are in [re/NOTES.md](re/NOTES.md).

## Languages

- UI strings: `app/src/main/res/values-<qualifier>/strings.xml` (de, es, fr, it, ja, pt = pt-PT,
  pt-rBR, hi). `app_name` and `title` are `translatable="false"`: the app name stays "Switch Input".
- Input names: `LABELS_I18N` in `tools/gen_inputs.py` → `values-<qualifier>/input_types.xml`
  and, through `tools/input_types.json`, the stub packages' own `values-<qualifier>` strings,
  so launcher shortcuts are named in the system language.
- Override: `AppLocale.kt` stores the chosen BCP-47 tag in `Prefs` and applies it with
  `AppCompatDelegate.setApplicationLocales` (persisted by `AppLocalesMetadataHolderService`).
  `SwitchActivity` is not an AppCompat activity, so it localises its toasts through
  `AppLocale.localized(context)`.
- Translation context: "switch to" always means changing the TV input; "launcher" is the
  home-screen app; "shortcut" is the small app added for one input.

## Security model

The app runs `su` for three things only: reading Fire TV's device-control database,
broadcasting the SelectInput directive, and installing/uninstalling shortcut packages.
What keeps other apps from riding on that:

- `SwitchActivity` is protected by `pt.aadf.sourcesswitcher.permission.SWITCH`
  (`protectionLevel="signature"`): only packages signed with the same key as the app can
  start it. The stub packages request that permission and are signed with the same key.
- `Safe.kt` validates every value that is interpolated into a root command (input type,
  endpoint id, device id). Values come from Fire TV's own database or from a stub's manifest;
  anything outside the expected pattern is rejected before a command is built.
- Shortcut APKs are streamed into `pm install -S` from the app's process; nothing is written
  to a world-readable location. Uninstall only touches packages whose signature matches the
  app's (`PackageManager.checkSignatures`), and only those count as installed shortcuts.
- `MainActivity` takes no intent extras; it acts only on remote input.
- Release builds are signed with a key kept outside the repository.

## Toolchain (Windows, user-level via scoop)

```powershell
scoop bucket add java
scoop install adb gradle android-clt apktool jadx java/temurin17-jdk
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
pip install pillow
```

`tools/env.ps1` puts the JDK and SDK on PATH for a shell; `tools/fire.ps1` wraps adb for the
stick and requires `FIRE_TV_ADDR` (e.g. `$env:FIRE_TV_ADDR = "192.168.1.50:5555"`).

## Build and deploy

```powershell
.\tools\deploy.ps1            # build stubs + gradle assembleDebug + adb install -r + open the app
.\tools\deploy.ps1 -NoLaunch  # build + install only
.\tools\deploy.ps1 -SkipBuild # install the existing APK
.\tools\deploy.ps1 -Release   # release build, signed with keystore.properties (see below)
.\tools\deploy.ps1 -Release -Replace   # uninstall first; needed once when the signing key changes
```

Generated inputs (run only after changing the type list or translations in the script):

```powershell
python -I tools\gen_inputs.py    # res/values*/input_types.xml, res/drawable-xhdpi/*.png, tools/input_types.json
python -I tools\build_stubs.py   # assets/stubs/*.apk (deploy.ps1 runs this)
```

Reinstalling with `-r` keeps installed shortcuts and Magisk's root grant as long as the
signing key is the same. A key change (debug → release) needs `-Replace`, and Magisk asks
for root again because the app gets a new uid.

## Versioning and releases

- `version.properties` is the single source: `VERSION_NAME` (semver) and `VERSION_CODE`
  (major·100 + minor·10 + patch). Gradle and `tools/build_stubs.py` read it, so the app and
  every stub carry the same version; the main screen shows it.
- Release signing: `keystore.properties` at the repo root (git-ignored) with `storeFile`,
  `storePassword`, `keyAlias`, `keyPassword`. The keystore lives outside the repo
  (`~/.android/sourcesswitcher-release.jks`). Without the file, release builds fall back to
  the debug key. Stubs are signed with the same key as the app.
- Releasing: bump `version.properties`, add a `CHANGELOG.md` entry, run
  `.\tools\deploy.ps1 -Release`, copy `app/build/outputs/apk/release/app-release.apk` to
  `dist/SwitchInput-<version>.apk` (git-ignored), tag `v<version>` and attach the APK to the
  GitHub release.
- Never commit `keystore.properties`, `*.jks`, `*.keystore`, `re/data/` or `diag/*.txt`:
  the device dumps contain serial numbers, MAC addresses and endpoint ids.

## Project layout

- `app/` – Kotlin app: `MainActivity` (picker), `SwitchActivity` (sends the switch),
  `Switcher`, `InputRepository`, `Shortcuts`, `Root`, `Prefs`.
  `res/values/input_types.xml` and `res/drawable-xhdpi/*` are generated; `assets/stubs/` is
  build output (git-ignored).
- `tools/` – `deploy.ps1`, `env.ps1`, `fire.ps1`, `gen_inputs.py`, `build_stubs.py`,
  `stub/Trampoline.java`.
- `re/` – reverse-engineering notes; pulled APKs and jadx output are git-ignored.
- `diag/` – screenshots captured on the device (the raw package dumps stay local, git-ignored).
- `docs/screenshots/` – images used by the README.

## Verifying on the device

- Switch: `adb logcat | grep OZ-DCS:NativeDirectiveServiceHandler` shows
  `Deserialized directive: SelectInput_Alexa.InputController` and
  `Directive handling complete, success : true`.
- Shortcut packages: `adb shell pm list packages | grep sourcesswitcher.input`.
- TV app entries: `adb shell cmd package query-activities -a android.intent.action.MAIN
  -c android.intent.category.LEANBACK_LAUNCHER | grep sourcesswitcher`.
- Safe test input: `STARK` (the Fire TV itself) switches to the input the stick is on.
  Tests with other inputs change the real TV input.

## Device facts (the stick this was built on)

Fire TV Stick 4K Max (karat), Fire OS 8.1.8.0 / Android 11, Magisk 30.7, Projectivy 4.72.
DCS 1.0.222657.0 with native DCS enabled. Equipment Control: Samsung TV over CEC/IR.

- Magisk su must run with `-mm` (global mount namespace); otherwise Android 11 app-data
  isolation hides other packages' `/data/data` from the app's root shell.
- The directive intent needs a `directiveId` extra or it is dropped, and the endpoint cookie
  must carry the device id or DCS answers `NO_SUCH_ENDPOINT`.
- `SwitchActivity` has `taskAffinity=""` and stubs start it without `NEW_TASK`, so finishing
  returns to the launcher even when the picker is still in the back stack.

## History

1. Approach: toolchain → device recon over adb → jadx on the DCS APKs → proof of concept from
   `adb shell` → app → deploy script. Kept as commented, revertible commits.
2. First build: one configurable tile. Second: one activity-alias per input, which Android
   lists but Projectivy folds into a single card per package. Third (current): stub packages.

## Dead ends

- `tvinput://tunebyprovider?providerId=<activity>` → `SwitchInputReceiver`: the service it
  forwards to no longer exists and native DCS logs "Input switch from Video is not supported".
- AIDL `DeviceControlApiService`: guarded by the signature|privileged permission
  `com.amazon.tv.devicecontrol.WRITE`.
- Activity aliases / pinned shortcuts: never appear as separate TV apps in Projectivy.

## Possible follow-ups

- Read inputs through `content://com.amazon.tv.devicecontrol.repository/ACTIVITIES_PATH`
  (root `content query`) instead of copying the SQLite file.
- Release signing config, so builds outside the debug key keep `install -r` working.
- Remove installed stubs when the main app is uninstalled.
