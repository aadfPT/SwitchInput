# Handoff: Alexa input-switcher app for FireStick

## Goal
Android app for a rooted FireStick (IP 192.168.1.141, wireless adb on port 5555,
Projectify as default launcher). When launched it sends a configured command
("Change to Playstation", "Change to hdmi 2") to Alexa and exits. Devices are
read from the FireStick Settings equipment list. Must be replaceable on new builds
(adb install -r). Keep work in a local git repo with commented, revertible commits.

## Already done
- tools/adb-probe.bat (Windows batch): runs `adb connect 192.168.1.141:5555`, then
  executes each line of tools/probe_cmds.txt as `adb shell <line>`, writing
  diag/out_N.txt and diag/probe_log.txt.
- tools/probe_cmds.txt: getprop, id, pm list packages, settings, voice/assist intents.
- Not yet done: the probe has NOT been run. No git repo, no app code.

## Next steps
1. Run tools/adb-probe.bat (double-click or `cmd /c`). Confirm diag/probe_log.txt
   shows the device as connected.
2. Read diag/out_*.txt. Find the Alexa package and the voice entry point
   (VOICE_COMMAND / ASSIST activity, or a way to send text to Alexa), and how the
   Equipment list is stored (settings list / package names).
3. git init in this folder, commit scaffolding, then the Android app.
4. Build needs the Android SDK + JDK on Windows (not available in the Linux sandbox:
   dl.google.com and maven.google.com are blocked by the proxy).
5. Install with `adb -s 192.168.1.141:5555 install -r app.apk`.

## Constraints
- Do not disable TLS or change network settings. Ask the user before any action on
  the FireStick that is not read-only, including installs.
