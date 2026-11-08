package com.buannel.studio.pvt.ltd.zostream.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Android TV review requires every focused core control to be operable by DPad.
 *
 * This modifier owns the control's focus target through [clickable]. Callers
 * must not append a separate focusable modifier before it: a key handler placed
 * inside an earlier focus target cannot receive that target's key events.
 * DPAD_CENTER/ENTER are handled before the clickable node so TV remotes always
 * invoke exactly one action, while [clickable] keeps touch and accessibility.
 */
fun Modifier.tvDpadClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = this
    .onPreviewKeyEvent { event ->
        if (!enabled) {
            return@onPreviewKeyEvent false
        }

        when (event.key) {
            Key.DirectionCenter,
            Key.Enter,
            Key.NumPadEnter -> {
                if (event.type == KeyEventType.KeyUp) {
                    onClick()
                }
                true
            }
            else -> false
        }
    }
    .clickable(enabled = enabled) { onClick() }
