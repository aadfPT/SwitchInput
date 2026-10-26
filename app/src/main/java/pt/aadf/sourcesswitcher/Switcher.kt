package pt.aadf.sourcesswitcher

import org.json.JSONObject

/**
 * Performs the input switch by injecting, as root, the very same broadcast the Alexa cloud
 * delivers to the Fire TV device-control app after "Alexa, change to <input>":
 * action com.amazon.speech.SmartCloud_Request carrying a SmartCloud/Request envelope whose
 * inner directive is Alexa.InputController / SelectInput for the TV endpoint.
 *
 * Found by decompiling com.amazon.tv.devicecontrol (see re/NOTES.md): the receiver is
 * .csmintegration.DirectiveReceiver, the intent is parsed by amazon.speech.simclient
 * DirectiveIntent (needs directiveId/namespace/name/payload/keyMap extras) and the endpoint
 * cookie must carry the device id or the handler answers NO_SUCH_ENDPOINT.
 */
object Switcher {
    private const val RECEIVER = "com.amazon.tv.devicecontrol/.csmintegration.DirectiveReceiver"
    private const val ACTION = "com.amazon.speech.SmartCloud_Request"

    fun buildCommand(inputType: String, endpointId: String, deviceId: String): String {
        val msgId = "sourcesswitcher-" + System.currentTimeMillis()
        val payload = JSONObject()
            .put("directive", JSONObject()
                .put("header", JSONObject()
                    .put("namespace", "Alexa.InputController")
                    .put("name", "SelectInput")
                    .put("payloadVersion", "3")
                    .put("messageId", msgId)
                    .put("correlationToken", msgId))
                .put("endpoint", JSONObject()
                    .put("endpointId", endpointId)
                    .put("cookie", JSONObject().put("id", deviceId)))
                .put("payload", JSONObject().put("input", inputType)))
            .toString()
        // JSON never contains single quotes, so single-quoting is safe for the device's sh.
        return "am broadcast -a $ACTION -n $RECEIVER" +
            " --es directiveId '$msgId' --es correlationToken '$msgId'" +
            " --es namespace SmartCloud --es name Request --es payloadVersion 3 --es keyMap '{}'" +
            " --es payload '$payload'"
    }

    /** Returns null on success, otherwise an error description. */
    fun switchTo(inputType: String, endpointId: String, deviceId: String): String? {
        val r = Root.run(buildCommand(inputType, endpointId, deviceId))
        if (!r.ok) return "su exit ${r.exitCode}: ${r.stderr.ifEmpty { r.text }}"
        if (!r.text.contains("Broadcast completed")) return r.text.trim().ifEmpty { "no response from am" }
        return null
    }
}
