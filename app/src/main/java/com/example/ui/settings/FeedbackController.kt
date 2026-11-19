package com.example.ui.settings

import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.material3.SnackbarHostState

class FeedbackController(
    private val hapticsEnabled: () -> Boolean,
    private val snackbarHostState: SnackbarHostState? = null,
    private val soundEnabled: () -> Boolean = { true }
) {
    fun performTapFeedback(view: View?) {
        if (view != null) {
            if (hapticsEnabled()) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
            if (soundEnabled()) {
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }
        }
    }

    suspend fun showMessage(message: String) {
        snackbarHostState?.showSnackbar(message)
    }
}
