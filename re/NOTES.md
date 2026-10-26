# Reverse-engineering notes (Fire TV Stick 4K Max, Fire OS 8.1.8.0 / Android 11, Magisk)

## Packages
- `com.amazon.tv.devicecontrol` (DCS, the "Equipment Control" engine, /system/system_ext/priv-app)
- `com.amazon.tv.devicecontrolsettings` (the Settings > Equipment Control UI)
- `com.amazon.vizzini` (Alexa voice client), `com.amazon.alexadirectivebrokerservice`

Pull with `adb pull /system/system_ext/priv-app/<pkg>/<pkg>.apk`, decompile with
`jadx -d re/jadx/<pkg> --no-res re/apk/<pkg>.apk` (both git-ignored).

## Where the equipment list lives
`/data/data/com.amazon.tv.devicecontrol/databases/device_control_db`, table `activities`:
`id` (uuid), `data` (JSON: `activityType` = HDMI_1..4 / ANTENA / PLAY_STATION / NINTENDO_SWITCH /
STARK …, `name`, `activityMap[..].activate` = functions blasted, e.g. ["POWER_ON","HDMI_3"]),
`last_used`. `STARK` is the Fire TV itself. Table `devices` holds the TV (Samsung, IR code set
512, CEC on). `data_store` row `METADATA_HASH` → `deviceMetadata[<device uuid>].chrEndpointId`
is the TV's Alexa endpoint id. The activity types are the fixed enum
`Definitions.ActivityType` (48 values; the names Alexa and the discovery payload use).

Same data is exposed by the unexported, root-readable content provider
`content://com.amazon.tv.devicecontrol.repository/ACTIVITIES_PATH` (see `DbUriMapper`).

## The trigger that works: inject Alexa's directive
Alexa's cloud directive reaches DCS as a broadcast handled by
`csmintegration.DirectiveReceiver` (actions `com.amazon.speech.SmartCloud_Request`, …), parsed by
`amazon.speech.simclient.directive.DirectiveIntent.fromV1IntentExtras`: string extras
`directiveId` (required), `namespace`, `name`, `payload`, `payloadVersion`, `keyMap`,
`correlationToken`. The enveloped form is `namespace=SmartCloud`, `name=Request`, with the
V3 envelope as `payload`:

```json
{"directive":{"header":{"namespace":"Alexa.InputController","name":"SelectInput",
  "payloadVersion":"3","messageId":"…","correlationToken":"…"},
  "endpoint":{"endpointId":"<chrEndpointId of the TV>","cookie":{"id":"<device uuid>"}},
  "payload":{"input":"PLAY_STATION"}}}
```

`NativeDirectiveServiceHandler` deserialises it, `DirectiveHandlingContextFactory` checks a
blaster is available, and `NativeControlHandler` executes it (CEC one-touch-play / IR, logged by
the native `com_amazon_devicecontrol` process). Inputs missing from the native instruction table
fall back to the Java `InputSwitchDirectiveHandler`, which resolves `input` by activity type.

Gotchas found the hard way:
- without `directiveId` the intent is silently dropped ("Directive not present in intent");
- without `cookie.id` (the device uuid) `ErrorDirectiveHandlerFactory` answers
  `NO_SUCH_ENDPOINT` / metric `noEndpointCookie`;
- the receiver is not exported, so the broadcast must come from root (`su -mm -c am broadcast
  -a com.amazon.speech.SmartCloud_Request -n com.amazon.tv.devicecontrol/.csmintegration.DirectiveReceiver …`).

Verified on the device: `OZ-DCS:NativeDirectiveServiceHandler: Directive handling complete,
success : true`, DCS answers Alexa with a `Response` carrying the new input.

## Dead ends
- `receivers.SwitchInputReceiver` accepts `android.intent.action.VIEW` with
  `tvinput://tunebyprovider?providerId=<activity uuid>` and forwards it to
  `csmintegration.DirectiveService` (`SWITCH_INPUT`), whose Java handler would call
  `tryActivateActivity`. On this firmware the service is not declared ("Unable to start
  service … not found") and `NativeDirectiveServiceHandler.handleInputSwitchFromVideo` only
  logs "Input switch from Video is not supported in Native DCS".
- AIDL `api.DeviceControlApiService` (`execute`, `getAllActivityConfigurations`, …) is guarded by
  the signature|privileged permission `com.amazon.tv.devicecontrol.WRITE`.
