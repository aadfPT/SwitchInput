# Reverse-engineering notes (Fire TV Stick 4K Max, Fire OS 8.1.8.0 / Android 11, Magisk)

## Packages
- `com.amazon.tv.devicecontrol` (DCS, "Equipment Control" engine, /system/system_ext/priv-app)
- `com.amazon.tv.devicecontrolsettings` (the Settings > Equipment Control UI)
- `com.amazon.vizzini` (Alexa voice client), `com.amazon.alexadirectivebrokerservice`

## Where the equipment list lives
`/data/data/com.amazon.tv.devicecontrol/databases/device_control_db`, table `activities`:
`id` (uuid), `data` (JSON: `activityType` = HDMI_1..4 / ANTENA / PLAY_STATION / STARK,
`name`, `activityMap[..].activate` = functions to blast, e.g. ["POWER_ON","HDMI_3"]),
`last_used`. `STARK` = the Fire TV itself. Table `devices` holds the TV (Samsung, IR code set 512, CEC on).
Same data is exposed by the (unexported, root-readable) content provider
`content://com.amazon.tv.devicecontrol.repository/ACTIVITIES_PATH`.

## Trigger
`receivers.SwitchInputReceiver` accepts `android.intent.action.VIEW` with
`tvinput://tunebyprovider?providerId=<activity uuid>` and forwards it to
`csmintegration.DirectiveService` with action `SWITCH_INPUT`; the handler calls
`DeviceControlHandler.tryActivateActivity(providerId, forceInput=true, onlyBlastInput=true,
ActivationMethod.ALL_EXPLICIT)` – i.e. exactly what "Alexa, change to X" ends up doing after the
cloud round trip, minus the cloud. The receiver is not exported, so the broadcast must come from
root (`su -c am broadcast ... -n com.amazon.tv.devicecontrol/.receivers.SwitchInputReceiver`).

Alternative (not used): the AIDL `api.DeviceControlApiService` (`execute`, `getAllActivityConfigurations`)
is guarded by the signature|privileged permission `com.amazon.tv.devicecontrol.WRITE`.
