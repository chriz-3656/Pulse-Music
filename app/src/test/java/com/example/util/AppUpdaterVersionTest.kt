package com.example.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdaterVersionTest {

    @Test
    fun `newer patch version is an update`() {
        assertTrue(AppUpdater.isVersionGreater("2.7.1", "2.7.0"))
    }

    @Test
    fun `same version is not an update`() {
        assertFalse(AppUpdater.isVersionGreater("2.7.1", "2.7.1"))
    }

    @Test
    fun `older version is not an update`() {
        assertFalse(AppUpdater.isVersionGreater("2.7.0", "2.7.1"))
    }

    @Test
    fun `numeric comparison handles multi digit components`() {
        assertTrue(AppUpdater.isVersionGreater("2.10.0", "2.9.9"))
        assertFalse(AppUpdater.isVersionGreater("2.9.9", "2.10.0"))
    }

    @Test
    fun `version tags with v prefix are supported`() {
        assertTrue(AppUpdater.isVersionGreater("v2.8.0", "2.7.1"))
        assertTrue(AppUpdater.isVersionGreater("2.8.0", "v2.7.1"))
    }

    @Test
    fun `missing trailing components are treated as zero`() {
        assertFalse(AppUpdater.isVersionGreater("2.7", "2.7.0"))
        assertTrue(AppUpdater.isVersionGreater("2.7.1", "2.7"))
    }
}
