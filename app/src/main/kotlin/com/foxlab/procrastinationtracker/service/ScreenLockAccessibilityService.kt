package com.foxlab.procrastinationtracker.service

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.accessibility.AccessibilityEvent

/**
 * Does nothing but exist: once the user enables it once in Configurações > Acessibilidade, it
 * lets [requestLockScreenIfAvailable] perform the system's GLOBAL_ACTION_LOCK_SCREEN action —
 * the same "lock now" behavior utility apps use, without requiring the much heavier Device Admin
 * permission. The "Bloquear tela agora" button works without this service too, it just won't be
 * able to actually lock the screen until it's granted.
 */
class ScreenLockAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    companion object {
        private var instance: ScreenLockAccessibilityService? = null

        val isEnabled: Boolean get() = instance != null

        /** Returns true if the screen was actually locked. */
        fun requestLockScreenIfAvailable(): Boolean {
            val service = instance ?: return false
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
            return service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }
    }
}
