package com.spendwise.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearancePreferenceMapperTest {
    @Test
    fun defaultsToLightModeWhenNoPreferenceIsSaved() {
        assertFalse(AppearancePreferenceMapper.toDarkMode(null))
    }

    @Test
    fun mapsPersistedDarkModeValue() {
        assertTrue(AppearancePreferenceMapper.toDarkMode(true))
        assertFalse(AppearancePreferenceMapper.toDarkMode(false))
    }

    @Test
    fun keepsMissingPreferenceUnsetForStartupThemeFallback() {
        assertNull(AppearancePreferenceMapper.toStartupPreference(null))
        assertTrue(AppearancePreferenceMapper.toStartupPreference(true)!!)
        assertFalse(AppearancePreferenceMapper.toStartupPreference(false)!!)
    }
}
