# Changelog

## 1.0.2 – 2026-10-27

Hardening of the root path.

- Only packages signed with the app's own key can start the switch activity
  (signature-level permission); the shortcut packages request it.
- Input type, endpoint id and device id are validated against strict patterns before any
  root command is built.
- Shortcut packages are streamed into `pm install` from the app process; no world-readable
  copy of the APK is written to disk any more.
- A package only counts as a Switch Input shortcut, and is only ever uninstalled, when its
  signature matches the app's.
- Tooling: the stick's address comes from `FIRE_TV_ADDR`; the first-session probe scripts
  are gone.

## 1.0.1 – 2026-10-27

- Shortcuts installed earlier are shown as added again after reopening the app
  (Android 11 package visibility: the app now declares which packages it looks for).
- Vertical scrollbar on the input list.
- Focused card no longer grows past the edges of the list.

## 1.0.0 – 2026-10-27

First release.

- One launcher app per Fire TV input: pick inputs in Switch Input, each becomes a TV app
  with its own banner (Projectivy, Fire TV home, any launcher that lists TV apps).
- Opening a shortcut switches the TV through Fire TV's Equipment Control engine and
  closes itself.
- Reads the equipment configured in Settings → Equipment Control, including equipment
  added later.
- Hold Select on an input to switch immediately.
- Languages: English, Deutsch, Español, Français, Italiano, 日本語, Português (Portugal),
  Português (Brasil), हिन्दी; follows the system language with an in-app override.
- Requires a rooted Fire TV (Magisk). Tested on Fire TV Stick 4K Max, Fire OS 8.1.
