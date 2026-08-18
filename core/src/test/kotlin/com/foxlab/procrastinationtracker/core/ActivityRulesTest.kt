package com.foxlab.procrastinationtracker.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Whether a slice counts as procrastination is decided by its name, accent-insensitively, so that
 * a user renaming a slice to "Procrastinação" gets the same behaviour as the seeded one without a
 * database migration. That makes the accent handling a real rule, not an implementation detail.
 */
class ActivityRulesTest {

    @Test
    fun `the seeded name is recognised`() {
        assertTrue(ActivityRules.isProcrastination("Procrastinando"))
    }

    @Test
    fun `accents do not change the verdict`() {
        assertTrue(ActivityRules.isProcrastination("Procrastinação"))
        assertTrue(ActivityRules.isProcrastination("procrastinaçao"))
    }

    @Test
    fun `case does not change the verdict`() {
        assertTrue(ActivityRules.isProcrastination("PROCRASTINAR"))
        assertTrue(ActivityRules.isProcrastination("pRoCrAsTiNando"))
    }

    @Test
    fun `the rule matches the stem anywhere in the name`() {
        assertTrue(ActivityRules.isProcrastination("Tempo procrastinado"))
        assertTrue(ActivityRules.isProcrastination("Procrastination"))
    }

    @Test
    fun `ordinary slice names are not procrastination`() {
        assertFalse(ActivityRules.isProcrastination("Trabalho"))
        assertFalse(ActivityRules.isProcrastination("Estudo"))
        assertFalse(ActivityRules.isProcrastination("Treino"))
        assertFalse(ActivityRules.isProcrastination(""))
    }

    @Test
    fun `a minute is the floor for offering a report`() {
        assertEquals(60_000L, ActivityRules.MIN_REPORTABLE_MILLIS)
    }
}
