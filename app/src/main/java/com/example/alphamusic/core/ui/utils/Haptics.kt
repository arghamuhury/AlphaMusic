package com.example.alphamusic.core.ui.utils

import android.view.HapticFeedbackConstants
import android.view.View

object Haptics {
    fun playClick(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    fun playLightTick(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
}
