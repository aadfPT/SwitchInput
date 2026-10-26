# Handoff: Alexa input-switcher app for FireStick

## Status (2026-10-26, evening)
Working and installed on the stick (192.168.1.141:5555, Fire OS 8.1.8.0, Magisk 30.7).
Third iteration: the app is a picker that installs one tiny stub *package* per input
(`pt.aadf.sourcesswitcher.input.<type>`, built by tools/build_stubs.py, shipped in assets);
Projectivy shows one card per package, so these are the only way to get one TV app per input.
Each stub forwards to SwitchActivity, which switches and closes. See README.md for usage and
re/NOTES.md for the reverse-engineering details.

Verified on the device:
- toggling a card runs `pm install` / `pm uninstall` of the stub as root (logcat INSTALL_EVENT
  SUCCESS), and Projectivy's All apps > TV Apps lists the stub ("Fire TV") with its banner
  (diag/projectivy_allapps.png),
- launching the stub (`am start -n pt.aadf.sourcesswitcher.input.stark/pt.aadf.sourcesswitcher.stub.Trampoline`)
  logs `Directive handling complete, success : true` and leaves Projectivy resumed, even with
  the picker still in the back stack (SwitchActivity has taskAffinity="" and the stub starts it
  without NEW_TASK),
- the main screen renders as designed (diag/main_screen.png).

The test stub was uninstalled again, so the launcher only shows "Switch Input" until the
user adds some. The earlier activity-alias approach is gone (aliases never showed in Projectivy). Magisk already holds an allow policy for the app (uid 10245),
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
