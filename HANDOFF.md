# Handoff: Alexa input-switcher app for FireStick

## Status (2026-10-26, evening)
Working and installed on the stick (192.168.1.141:5555, Fire OS 8.1.8.0, Magisk 30.7).
Second iteration: the app is now a picker that enables one launcher shortcut (activity-alias)
per input; each shortcut switches and closes itself. See README.md for usage and
re/NOTES.md for the reverse-engineering details.

Verified on the device through logcat (`OZ-DCS:NativeDirectiveServiceHandler ...
Directive handling complete, success : true`) and `cmd package query-activities`:
- toggling a card enables/disables its alias, which then is/isn't listed as a
  LEANBACK_LAUNCHER entry,
- `am start -n pt.aadf.sourcesswitcher/.Shortcut_STARK` switches and leaves Projectivy
  as the resumed activity,
- the main screen renders as designed (diag/main_screen.png).

All shortcuts were disabled again after testing, so the launcher only shows "Switch Input"
until the user adds some. Magisk already holds an allow policy for the app (uid 10245),
which survives `adb install -r`.

## What was tried and rejected
- `tvinput://tunebyprovider?providerId=<activity>` → `SwitchInputReceiver`: the service it
  forwards to no longer exists and native DCS logs "Input switch from Video is not supported".
- AIDL `DeviceControlApiService`: signature|privileged permission, not reachable from a
  third-party app without an `app_process` root helper.

## Possible follow-ups
- Read inputs through `content://com.amazon.tv.devicecontrol.repository/ACTIVITIES_PATH`
  (root `content query`) instead of copying the SQLite file.
- Release signing config so `install -r` works with a stable key outside debug builds.

## Constraints
- No TLS / network changes on the Fire Stick.
- Installs and tests switch the real TV input; the STARK activity (Fire TV) is the safe test.
