package pt.aadf.sourcesswitcher

import org.json.JSONObject

object Switcher {
    private const val DIRECTIVE_RECEIVER = "com.amazon.tv.devicecontrol/.csmintegration.DirectiveReceiver"
    private const val DIRECTIVE_BROADCAST = "com.amazon.speech.SmartCloud_Request"
    private const val ENVELOPE_NAMESPACE = "SmartCloud"
    private const val ENVELOPE_NAME = "Request"

    fun buildCommand(inputType: String, endpointId: String, deviceId: String): String {
        val messageId = "sourcesswitcher-" + System.currentTimeMillis()
        val envelope = alexaSelectInputEnvelope(inputType, endpointId, deviceId, messageId)
        return "am broadcast -a $DIRECTIVE_BROADCAST -n $DIRECTIVE_RECEIVER" +
            " --es directiveId '$messageId' --es correlationToken '$messageId'" +
            " --es namespace $ENVELOPE_NAMESPACE --es name $ENVELOPE_NAME --es payloadVersion 3 --es keyMap '{}'" +
            " --es payload '$envelope'"
    }

    private fun alexaSelectInputEnvelope(inputType: String, endpointId: String, deviceId: String, messageId: String): String =
        JSONObject()
            .put("directive", JSONObject()
                .put("header", JSONObject()
                    .put("namespace", "Alexa.InputController")
                    .put("name", "SelectInput")
                    .put("payloadVersion", "3")
                    .put("messageId", messageId)
                    .put("correlationToken", messageId))
                .put("endpoint", JSONObject()
                    .put("endpointId", endpointId)
                    .put("cookie", JSONObject().put("id", deviceId)))
                .put("payload", JSONObject().put("input", inputType)))
            .toString()

    fun switchTo(inputType: String, endpointId: String, deviceId: String): String? {
        val r = Root.run(buildCommand(inputType, endpointId, deviceId))
        if (!r.ok) return "su exit ${r.exitCode}: ${r.stderr.ifEmpty { r.text }}"
        if (!r.text.contains("Broadcast completed")) return r.text.trim().ifEmpty { "no response from am" }
        return null
    }
}
