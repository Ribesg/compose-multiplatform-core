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

@file:Suppress("DEPRECATION")

package androidx.compose.foundation.gestures

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastFold

internal actual fun CompositionLocalConsumerModifierNode.platformScrollConfig(): ScrollConfig = JsConfig

private object JsConfig : ScrollConfig {
    override fun Density.calculateMouseWheelScroll(event: PointerEvent, bounds: IntSize): Offset {
        // Note: The returned offset value here is not strictly accurate.
        // However, it serves two primary purposes:
        // 1. Ensures all related tests pass successfully.
        // 2. Provides satisfactory UI behavior
        // In future iterations, this value could be refined to enhance UI behavior.
        // However, keep in mind that any modifications would also necessitate adjustments to the corresponding tests.
        return event.totalScrollDelta * -1f
    }
}

private val PointerEvent.totalScrollDelta
    get() = this.changes.fastFold(Offset.Zero) { acc, c -> acc + c.scrollDelta }


/*
import androidx.compose.ui.input.mouse.MouseScrollOrientation
import androidx.compose.ui.input.mouse.MouseScrollUnit
import androidx.compose.ui.input.mouse.mouseScrollFilter
import androidx.compose.ui.platform.DesktopPlatform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalDesktopPlatform
*/
/*
composed {
    val density = LocalDensity.current
    val desktopPlatform = LocalDesktopPlatform.current
    val config = PlatformScrollConfig(density, desktopPlatform)

    mouseScrollFilter { event, bounds ->
        if (isOrientationMatches(orientation, event.orientation)) {
            val scrollBounds = when (orientation) {
                Orientation.Vertical -> bounds.height
                Orientation.Horizontal -> bounds.width
            }
            onScroll(-config.toScrollOffset(event.delta, scrollBounds))
            true
        } else {
            false
        }
    }
}

fun isOrientationMatches(
    orientation: Orientation,
    mouseOrientation: MouseScrollOrientation
): Boolean {
    return if (mouseOrientation == MouseScrollOrientation.Horizontal) {
        orientation == Orientation.Horizontal
    } else {
        orientation == Orientation.Vertical
    }
}

private class PlatformScrollConfig(
    private val density: Density,
    private val desktopPlatform: DesktopPlatform
) {
    fun toScrollOffset(
        unit: MouseScrollUnit,
        bounds: Int
    ): Float = when (unit) {
        is MouseScrollUnit.Line -> unit.value * platformLineScrollOffset(bounds)

        // TODO(demin): Chrome/Firefox on Windows scroll differently: value * 0.90f * bounds
        // the formula was determined experimentally based on Windows Start behaviour
        is MouseScrollUnit.Page -> unit.value * bounds.toFloat()
    }

    // TODO(demin): Chrome on Windows/Linux uses different scroll strategy
    //  (always the same scroll offset, bounds-independent).
    //  Figure out why and decide if we can use this strategy instead of current one.
    private fun platformLineScrollOffset(bounds: Int): Float {
        return when (desktopPlatform) {
            // TODO(demin): is this formula actually correct? some experimental values don't fit
            //  the formula
            // the formula was determined experimentally based on Ubuntu Nautilus behaviour
            DesktopPlatform.Linux -> sqrt(bounds.toFloat())

            // the formula was determined experimentally based on Windows Start behaviour
            DesktopPlatform.Windows -> bounds / 20f

            // the formula was determined experimentally based on MacOS Finder behaviour
            // MacOS driver will send events with accelerating delta
            DesktopPlatform.MacOS -> with(density) { 10.dp.toPx() }
        }
    }
}
*/
