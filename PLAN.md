# SourcesSwitcher – approach plan

Goal: an Android (Fire OS) app that, when launched from the Projectivy launcher on the
rooted Fire Stick (192.168.1.141:5555), sends one pre-configured "Change to <input>"
command to Alexa / Fire TV Equipment Control and exits. The list of selectable inputs
is read from the Fire TV's own equipment-control configuration.

## Phases

1. Tooling (Windows host, user-level via scoop, no admin)
   - adb (platform-tools), Temurin JDK 17, Android command-line tools + SDK 34,
     Gradle, apktool + jadx for reading Fire OS system packages.
2. Device reconnaissance (read-only, over `adb connect 192.168.1.141:5555`)
   - Fire OS version, root availability (`su`), package list.
   - Identify the packages behind Settings > Equipment Control and Alexa voice
     ("Change to HDMI 2").  Candidates: com.amazon.tv.devicecontrolsettings,
     com.amazon.tv.devicecontrol, com.amazon.vizzini (Alexa voice), com.amazon.hedwig.
   - Capture `logcat` while the user issues the voice command to see which intents /
     services / broadcasts carry the input switch, and where the equipment list is
     persisted (settings, SharedPreferences, content provider, SQLite).
3. Reverse engineering (only as far as needed)
   - `adb pull` the relevant APKs, decompile with jadx, find the public entry point
     (exported activity/service/broadcast, text-to-Alexa interface, or the device
     control directive handler) and the data store for the equipment list.
4. Proof of concept from `adb shell` (as root if a signature permission blocks it)
   - A single `am start` / `am broadcast` / `service call` / `content query` that
     performs the switch and one that lists the devices.  Nothing else is built until
     this works on the real device.
5. Android app
   - Kotlin, minSdk matching the Fire Stick, single leanback launcher activity.
   - Config screen: lists inputs read from the device, user picks one, saved in
     SharedPreferences.  Optional "one app per input" via activity-alias / shortcuts.
   - Launch behaviour: if configured, fire the command (through `su` when needed)
     and `finish()` immediately; long-press / menu reopens config.
6. Deploy & iterate
   - `tools/deploy.ps1`: build, `adb connect`, `adb install -r`, launch.
   - Every step committed to the local git repo with explanatory messages.

## Ground rules
- Device actions that are not read-only (installs, settings changes) only with the
  user's go-ahead; this file and HANDOFF.md record that constraint.
- No TLS / network changes on the Fire Stick.
