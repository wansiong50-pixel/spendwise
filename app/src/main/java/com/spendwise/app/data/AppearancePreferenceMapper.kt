package com.spendwise.app.data

object AppearancePreferenceMapper {
    fun toDarkMode(savedValue: Boolean?): Boolean = savedValue ?: false

    fun toStartupPreference(savedValue: Boolean?): Boolean? = savedValue
}
