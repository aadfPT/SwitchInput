# Handoff: Alexa input-switcher app for FireStick

## Status (2026-10-26)
Working and installed on the stick (192.168.1.141:5555, Fire OS 8.1.8.0, Magisk 30.7).
See README.md for usage and re/NOTES.md for the reverse-engineering details.

Verified on the device through logcat (`OZ-DCS:NativeDirectiveServiceHandler ...
Directive handling complete, success : true`):
- "Test now" on the setup screen,
- the launcher tile (MainActivity) – sends the saved input and returns to Projectivy,
- the injected directive for STARK (Fire TV itself) and for HDMI_2.

The saved configuration was cleared after testing, so the first launch of the tile opens
the setup screen. Magisk already holds an allow policy for the app (uid 10245), which
survives `adb install -r`.

## What was tried and rejected
- `tvinput://tunebyprovider?providerId=<activity>` → `SwitchInputReceiver`: the service it
  forwards to no longer exists and native DCS logs "Input switch from Video is not supported".
- AIDL `DeviceControlApiService`: signature|privileged permission, not reachable from a
  third-party app without an `app_process` root helper.

## Possible follow-ups
- One tile per input (activity-aliases or Android TV shortcuts) instead of one configurable tile.
- Read inputs through `content://com.amazon.tv.devicecontrol.repository/ACTIVITIES_PATH`
  (root `content query`) instead of copying the SQLite file.
- Release signing config so `install -r` works with a stable key outside debug builds.

## Constraints
- No TLS / network changes on the Fire Stick.
- Installs and tests switch the real TV input; the STARK activity (Fire TV) is the safe test.
