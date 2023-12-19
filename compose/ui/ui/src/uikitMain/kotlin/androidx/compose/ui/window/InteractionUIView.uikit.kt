/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.window

import androidx.compose.ui.toDpOffset
import androidx.compose.ui.unit.DpOffset
import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import org.jetbrains.skiko.SkikoInputModifiers
import org.jetbrains.skiko.SkikoKey
import org.jetbrains.skiko.SkikoKeyboardEvent
import org.jetbrains.skiko.SkikoKeyboardEventKind
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIEvent
import platform.UIKit.UIKeyModifierAlternate
import platform.UIKit.UIKeyModifierCommand
import platform.UIKit.UIKeyModifierControl
import platform.UIKit.UIKeyModifierShift
import platform.UIKit.UIPress
import platform.UIKit.UIPressesEvent
import platform.UIKit.UIView

internal enum class UITouchesEventPhase {
    BEGAN, MOVED, ENDED, CANCELLED
}

internal class InteractionUIView(
    private val keyboardEventHandler: KeyboardEventHandler,
    private val touchesDelegate: Delegate,
    private val updateTouchesCount: (count: Int) -> Unit,
    private val checkBounds: (point: DpOffset) -> Boolean,
) : UIView(CGRectZero.readValue()) {

    interface Delegate {
        fun pointInside(point: CValue<CGPoint>, event: UIEvent?): Boolean
        fun onTouchesEvent(view: UIView, event: UIEvent, phase: UITouchesEventPhase)
    }

    /**
     * When there at least one tracked touch, we need notify redrawer about it. It should schedule CADisplayLink which
     * affects frequency of polling UITouch events on high frequency display and forces it to match display refresh rate.
     */
    private var _touchesCount = 0
        set(value) {
            field = value
            updateTouchesCount(value)
        }

    init {
        multipleTouchEnabled = true
        userInteractionEnabled = true
    }

    override fun canBecomeFirstResponder() = true

    override fun pressesBegan(presses: Set<*>, withEvent: UIPressesEvent?) {
        handleUIViewPressesBegan(keyboardEventHandler, presses, withEvent)
        super.pressesBegan(presses, withEvent)
    }

    override fun pressesEnded(presses: Set<*>, withEvent: UIPressesEvent?) {
        handleUIViewPressesEnded(keyboardEventHandler, presses, withEvent)
        super.pressesEnded(presses, withEvent)
    }

    /**
     * https://developer.apple.com/documentation/uikit/uiview/1622533-point
     */
    override fun pointInside(point: CValue<CGPoint>, withEvent: UIEvent?): Boolean {
        val pointOffset = point.useContents { this.toDpOffset() }
        return checkBounds(pointOffset) && touchesDelegate.pointInside(point, withEvent)
    }

    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesBegan(touches, withEvent)

        _touchesCount += touches.size

        withEvent?.let { event ->
            touchesDelegate.onTouchesEvent(this, event, UITouchesEventPhase.BEGAN)
        }
    }

    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesEnded(touches, withEvent)

        _touchesCount -= touches.size

        withEvent?.let { event ->
            touchesDelegate.onTouchesEvent(this, event, UITouchesEventPhase.ENDED)
        }
    }

    override fun touchesMoved(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesMoved(touches, withEvent)

        withEvent?.let { event ->
            touchesDelegate.onTouchesEvent(this, event, UITouchesEventPhase.MOVED)
        }
    }

    override fun touchesCancelled(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesCancelled(touches, withEvent)

        _touchesCount -= touches.size

        withEvent?.let { event ->
            touchesDelegate.onTouchesEvent(this, event, UITouchesEventPhase.CANCELLED)
        }
    }

}

internal fun handleUIViewPressesBegan(
    keyboardEventHandler: KeyboardEventHandler,
    presses: Set<*>,
    withEvent: UIPressesEvent?
) {
    if (withEvent != null) {
        for (press in withEvent.allPresses) {
            if (press is UIPress) {
                keyboardEventHandler.onKeyboardEvent(
                    toSkikoKeyboardEvent(press, SkikoKeyboardEventKind.DOWN)
                )
            }
        }
    }
}

internal fun handleUIViewPressesEnded(
    keyboardEventHandler: KeyboardEventHandler,
    presses: Set<*>,
    withEvent: UIPressesEvent?
) {
    if (withEvent != null) {
        for (press in withEvent.allPresses) {
            if (press is UIPress) {
                keyboardEventHandler.onKeyboardEvent(
                    toSkikoKeyboardEvent(press, SkikoKeyboardEventKind.UP)
                )
            }
        }
    }
}

private fun toSkikoKeyboardEvent(
    event: UIPress,
    kind: SkikoKeyboardEventKind
): SkikoKeyboardEvent {
    val timestamp = (event.timestamp * 1_000).toLong()
    return SkikoKeyboardEvent(
        SkikoKey.valueOf(event.key!!.keyCode),
        toSkikoModifiers(event),
        kind,
        timestamp,
        event
    )
}

private fun toSkikoModifiers(event: UIPress): SkikoInputModifiers {
    var result = 0
    val modifiers = event.key!!.modifierFlags
    if (modifiers and UIKeyModifierAlternate != 0L) {
        result = result.or(SkikoInputModifiers.ALT.value)
    }
    if (modifiers and UIKeyModifierShift != 0L) {
        result = result.or(SkikoInputModifiers.SHIFT.value)
    }
    if (modifiers and UIKeyModifierControl != 0L) {
        result = result.or(SkikoInputModifiers.CONTROL.value)
    }
    if (modifiers and UIKeyModifierCommand != 0L) {
        result = result.or(SkikoInputModifiers.META.value)
    }
    return SkikoInputModifiers(result)
}
