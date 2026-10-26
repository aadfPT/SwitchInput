package pt.aadf.sourcesswitcher

object Safe {
    private val INPUT_TYPE = Regex("^[A-Z0-9_]{1,40}$")
    private val HEX_ID = Regex("^[0-9a-f]{32}$")
    private val UUID = Regex("^[0-9a-fA-F-]{36}$")

    fun inputType(value: String): String =
        if (INPUT_TYPE.matches(value)) value else throw IllegalArgumentException("invalid input type")

    fun endpointId(value: String): String =
        if (HEX_ID.matches(value)) value else throw IllegalArgumentException("invalid endpoint id")

    fun deviceId(value: String): String =
        if (value.isEmpty() || UUID.matches(value)) value else throw IllegalArgumentException("invalid device id")
}
