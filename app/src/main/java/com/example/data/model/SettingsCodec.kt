package com.example.data.model

object SettingsCodec {
    private val adapter = MoshiProvider.moshi.adapter(SettingsBundle::class.java)

    fun encode(bundle: SettingsBundle): String = adapter.toJson(bundle)

    fun decode(json: String): SettingsBundle? =
        runCatching { adapter.fromJson(json) }.getOrNull()
}
