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

package androidx.compose.foundation.text2.input.internal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.internal.selection.TextFieldSelectionState
import androidx.compose.foundation.text2.input.internal.selection.textFieldMagnifierNode
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextPainter
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.truncate
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Modifier element for the core functionality of [BasicTextField2] that is passed as inner
 * TextField to the decoration box. This is only half the actual modifiers for the field, the other
 * half are only attached to the decorated text field.
 *
 * This modifier mostly handles layout and draw.
 */
internal data class TextFieldCoreModifier(
    private val isFocused: Boolean, /* true iff component is focused and the window in focus */
    private val textLayoutState: TextLayoutState,
    private val textFieldState: TransformedTextFieldState,
    private val textFieldSelectionState: TextFieldSelectionState,
    private val cursorBrush: Brush,
    private val writeable: Boolean,
    private val scrollState: ScrollState,
    private val orientation: Orientation,
) : ModifierNodeElement<TextFieldCoreModifierNode>() {

    override fun create(): TextFieldCoreModifierNode = TextFieldCoreModifierNode(
        isFocused = isFocused,
        textLayoutState = textLayoutState,
        textFieldState = textFieldState,
        textFieldSelectionState = textFieldSelectionState,
        cursorBrush = cursorBrush,
        writeable = writeable,
        scrollState = scrollState,
        orientation = orientation,
    )

    override fun update(node: TextFieldCoreModifierNode) {
        node.updateNode(
            isFocused = isFocused,
            textLayoutState = textLayoutState,
            textFieldState = textFieldState,
            textFieldSelectionState = textFieldSelectionState,
            cursorBrush = cursorBrush,
            writeable = writeable,
            scrollState = scrollState,
            orientation = orientation,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        // no inspector info
    }
}

/** Modifier node for [TextFieldCoreModifier]. */
@OptIn(ExperimentalFoundationApi::class)
internal class TextFieldCoreModifierNode(
    // true iff this component is focused and the window is focused
    private var isFocused: Boolean,
    private var textLayoutState: TextLayoutState,
    private var textFieldState: TransformedTextFieldState,
    private var textFieldSelectionState: TextFieldSelectionState,
    private var cursorBrush: Brush,
    private var writeable: Boolean,
    private var scrollState: ScrollState,
    private var orientation: Orientation,
) : DelegatingNode(),
    LayoutModifierNode,
    DrawModifierNode,
    CompositionLocalConsumerModifierNode,
    GlobalPositionAwareModifierNode,
    SemanticsModifierNode {

    /**
     * Animatable object for cursor's alpha value. It becomes 1f for half a second and 0f for
     * another half a second when TextField is focused and editable. Initial value should be 0f
     * so that when cursor needs to be drawn for the first time, change to 1f invalidates draw.
     */
    private val cursorAnimation = CursorAnimationState()

    /**
     * Whether to show cursor at all when TextField has focus. This depends on enabled, read only,
     * and brush at a given time.
     */
    private val showCursor: Boolean
        get() = writeable && isFocused && cursorBrush.isSpecified

    /**
     * Observes the [textFieldState] for any changes to content or selection. If a change happens,
     * cursor blink animation gets reset.
     */
    private var changeObserverJob: Job? = null

    /**
     * When selection/cursor changes its position, it may go out of the visible area. When that
     * happens, ideally we would want to scroll the TextField to keep the changing handle in the
     * visible area. The following member variables keep track of the latest selection and cursor
     * positions that we have adjusted for. When we detect a change to both of them during the
     * layout phase, ScrollState gets adjusted.
     */
    private var previousSelection: TextRange? = null
    private var previousCursorRect: Rect = Rect(-1f, -1f, -1f, -1f)

    private val textFieldMagnifierNode = delegate(
        textFieldMagnifierNode(
            textFieldState = textFieldState,
            textFieldSelectionState = textFieldSelectionState,
            textLayoutState = textLayoutState,
            isFocused = isFocused
        )
    )

    /**
     * Updates all the related properties and invalidates internal state based on the changes.
     */
    fun updateNode(
        isFocused: Boolean,
        textLayoutState: TextLayoutState,
        textFieldState: TransformedTextFieldState,
        textFieldSelectionState: TextFieldSelectionState,
        cursorBrush: Brush,
        writeable: Boolean,
        scrollState: ScrollState,
        orientation: Orientation,
    ) {
        val previousShowCursor = this.showCursor
        val wasFocused = this.isFocused
        val previousTextFieldState = this.textFieldState
        val previousTextLayoutState = this.textLayoutState
        val previousTextFieldSelectionState = this.textFieldSelectionState
        val previousScrollState = this.scrollState

        this.isFocused = isFocused
        this.textLayoutState = textLayoutState
        this.textFieldState = textFieldState
        this.textFieldSelectionState = textFieldSelectionState
        this.cursorBrush = cursorBrush
        this.writeable = writeable
        this.scrollState = scrollState
        this.orientation = orientation

        textFieldMagnifierNode.update(
            textFieldState = textFieldState,
            textFieldSelectionState = textFieldSelectionState,
            textLayoutState = textLayoutState,
            isFocused = isFocused
        )

        if (!showCursor) {
            changeObserverJob?.cancel()
            changeObserverJob = null
            cursorAnimation.cancelAndHide()
        } else if (!wasFocused ||
            previousTextFieldState != textFieldState ||
            !previousShowCursor
        ) {
            // this node is writeable, focused and gained that focus just now.
            // start the state value observation
            changeObserverJob = coroutineScope.launch {
                snapshotFlow {
                    // Read the text state, so the animation restarts when the text or cursor
                    // position change.
                    textFieldState.text
                    // Only animate the cursor when its window is actually focused. This also
                    // disables the cursor animation when the screen is off.
                    currentValueOf(LocalWindowInfo).isWindowFocused
                }.collectLatest { isWindowFocused ->
                    if (isWindowFocused) {
                        cursorAnimation.snapToVisibleAndAnimate()
                    }
                }
            }
        }

        if (previousTextFieldState != textFieldState ||
            previousTextLayoutState != textLayoutState ||
            previousTextFieldSelectionState != textFieldSelectionState ||
            previousScrollState != scrollState) {
            invalidateMeasurement()
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ) = if (orientation == Orientation.Vertical) {
        measureVerticalScroll(measurable, constraints)
    } else {
        measureHorizontalScroll(measurable, constraints)
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val value = textFieldState.text
        val textLayoutResult = textLayoutState.layoutResult ?: return

        if (value.selectionInChars.collapsed) {
            drawText(textLayoutResult)
            drawCursor()
        } else {
            drawSelection(value.selectionInChars, textLayoutResult)
            drawText(textLayoutResult)
        }

        with(textFieldMagnifierNode) { draw() }
    }

    private fun MeasureScope.measureVerticalScroll(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        // remove any height constraints for TextField since it'll be able to scroll vertically.
        val childConstraints = constraints.copy(maxHeight = Constraints.Infinity)
        val placeable = measurable.measure(childConstraints)
        // final height is the minimum of calculated or constrained maximum height.
        val height = min(placeable.height, constraints.maxHeight)

        return layout(placeable.width, height) {
            // we may need to update the scroll state to bring the cursor back into view after
            // layout is completed.
            val currSelection = textFieldState.text.selectionInChars
            val offsetToFollow = calculateOffsetToFollow(currSelection)

            val cursorRectInScroller = if (offsetToFollow >= 0) {
                getCursorRectInScroller(
                    cursorOffset = offsetToFollow,
                    textLayoutResult = textLayoutState.layoutResult,
                    rtl = layoutDirection == LayoutDirection.Rtl,
                    textFieldWidth = placeable.width
                )
            } else {
                null
            }

            updateScrollState(
                cursorRect = cursorRectInScroller,
                containerSize = height,
                textFieldSize = placeable.height
            )

            // only update the previous selection if this node is focused.
            if (isFocused) {
                previousSelection = currSelection
            }

            placeable.placeRelative(0, -scrollState.value)
        }
    }

    private fun MeasureScope.measureHorizontalScroll(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        // If the maxIntrinsicWidth of the children is already smaller than the constraint, pass
        // the original constraints so that the children has more information to determine its
        // size.
        val maxIntrinsicWidth = measurable.maxIntrinsicWidth(constraints.maxHeight)

        // remove any width constraints for TextField since it'll be able to scroll horizontally.
        val childConstraints = if (maxIntrinsicWidth < constraints.maxWidth) {
            constraints
        } else {
            constraints.copy(maxWidth = Constraints.Infinity)
        }
        val placeable = measurable.measure(childConstraints)
        val width = min(placeable.width, constraints.maxWidth)

        return layout(width, placeable.height) {
            // we may need to update the scroll state to bring the cursor back into view before
            // layout is updated.
            val currSelection = textFieldState.text.selectionInChars
            val offsetToFollow = calculateOffsetToFollow(currSelection)

            val cursorRectInScroller = if (offsetToFollow >= 0) {
                getCursorRectInScroller(
                    cursorOffset = offsetToFollow,
                    textLayoutResult = textLayoutState.layoutResult,
                    rtl = layoutDirection == LayoutDirection.Rtl,
                    textFieldWidth = placeable.width
                )
            } else {
                null
            }

            updateScrollState(
                cursorRect = cursorRectInScroller,
                containerSize = width,
                textFieldSize = placeable.width
            )

            // only update the previous selection if this node is focused.
            if (isFocused) {
                previousSelection = currSelection
            }

            placeable.placeRelative(-scrollState.value, 0)
        }
    }

    private fun calculateOffsetToFollow(currSelection: TextRange): Int {
        return when {
            currSelection.end != previousSelection?.end -> currSelection.end
            currSelection.start != previousSelection?.start -> currSelection.start
            else -> -1
        }
    }

    /**
     * Updates the scroll state to make sure cursor is visible after text content, selection, or
     * layout changes. Only scroll changes won't trigger this.
     *
     * @param cursorRect Rectangle area to bring into view. Pass null to skip this functionality.
     * @param containerSize Either height or width of scrollable host, depending on scroll
     * orientation
     * @param textFieldSize Either height or width of scrollable text field content, depending on
     * scroll orientation
     */
    private fun updateScrollState(
        cursorRect: Rect?,
        containerSize: Int,
        textFieldSize: Int,
    ) {
        // update the maximum scroll value
        val difference = textFieldSize - containerSize
        scrollState.maxValue = difference

        // if the cursor is not showing, we don't have to update the scroll state for the cursor
        // if there is no rect area to bring into view, we can early return.
        if (!showCursor || cursorRect == null) return

        // Check if cursor has actually changed its location
        if (cursorRect.left != previousCursorRect.left ||
            cursorRect.top != previousCursorRect.top) {
            val vertical = orientation == Orientation.Vertical
            val cursorStart = if (vertical) cursorRect.top else cursorRect.left
            val cursorEnd = if (vertical) cursorRect.bottom else cursorRect.right

            val startVisibleBound = scrollState.value
            val endVisibleBound = startVisibleBound + containerSize
            val offsetDifference = when {
                // make bottom/end of the cursor visible
                //
                // text box
                // +----------------------+
                // |                      |
                // |                      |
                // |          cursor      |
                // |             |        |
                // +-------------|--------+
                //               |
                //
                cursorEnd > endVisibleBound -> cursorEnd - endVisibleBound

                // in rare cases when there's not enough space to fit the whole cursor, prioritise
                // the bottom/end of the cursor
                //
                //             cursor
                // text box      |
                // +-------------|--------+
                // |             |        |
                // +-------------|--------+
                //               |
                //
                cursorStart < startVisibleBound && cursorEnd - cursorStart > containerSize ->
                    cursorEnd - endVisibleBound

                // make top/start of the cursor visible if there's enough space to fit the whole
                // cursor
                //
                //               cursor
                // text box       |
                // +--------------|-------+
                // |              |       |
                // |                      |
                // |                      |
                // |                      |
                // +----------------------+
                //
                cursorStart < startVisibleBound && cursorEnd - cursorStart <= containerSize ->
                    cursorStart - startVisibleBound

                // otherwise keep current offset
                else -> 0f
            }
            previousCursorRect = cursorRect
            // this call will respect the earlier set maxValue
            // no need to coerce again.
            // prefer to use immediate dispatch instead of suspending scroll calls
            coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                scrollState.scrollBy(offsetDifference.roundToNext())
            }
        }
    }

    /**
     * Draws the selection highlight.
     */
    private fun DrawScope.drawSelection(
        selection: TextRange,
        textLayoutResult: TextLayoutResult
    ) {
        val start = selection.min
        val end = selection.max
        if (start != end) {
            val selectionBackgroundColor = currentValueOf(LocalTextSelectionColors)
                .backgroundColor
            val selectionPath = textLayoutResult.getPathForRange(start, end)
            drawPath(selectionPath, color = selectionBackgroundColor)
        }
    }

    /**
     * Draws the text content.
     */
    private fun DrawScope.drawText(textLayoutResult: TextLayoutResult) {
        drawIntoCanvas { canvas ->
            TextPainter.paint(canvas, textLayoutResult)
        }
    }

    /**
     * Draws the cursor indicator. Do not confuse it with cursor handle which is a popup that
     * carries the cursor movement gestures.
     */
    private fun DrawScope.drawCursor() {
        // Only draw cursor if it can be shown and its alpha is higher than 0f
        // Alpha is checked before showCursor purposefully to make sure that we read
        // cursorAlpha in draw phase. So, when the alpha value changes, draw phase invalidates.
        val cursorAlphaValue = cursorAnimation.cursorAlpha
        if (cursorAlphaValue == 0f || !showCursor) return

        val cursorRect = textFieldSelectionState.cursorRect

        drawLine(
            cursorBrush,
            cursorRect.topCenter,
            cursorRect.bottomCenter,
            alpha = cursorAlphaValue,
            strokeWidth = cursorRect.width
        )
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        this.textLayoutState.coreNodeCoordinates = coordinates
        textFieldMagnifierNode.onGloballyPositioned(coordinates)
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        with(textFieldMagnifierNode) { applySemantics() }
    }
}

private val DefaultCursorThickness = 2.dp

/**
 * If brush has a specified color. It's possible that [SolidColor] contains [Color.Unspecified].
 */
private val Brush.isSpecified: Boolean
    get() = !(this is SolidColor && this.value.isUnspecified)

/**
 * Finds the rectangle area that corresponds to the visible cursor.
 *
 * @param cursorOffset Index of where cursor is at
 * @param textLayoutResult Current text layout to look for cursor rect.
 * @param rtl True if layout direction is RightToLeft
 * @param textFieldWidth Total width of TextField composable
 */
private fun Density.getCursorRectInScroller(
    cursorOffset: Int,
    textLayoutResult: TextLayoutResult?,
    rtl: Boolean,
    textFieldWidth: Int
): Rect {
    val cursorRect = textLayoutResult?.getCursorRect(
        cursorOffset.coerceIn(0..textLayoutResult.layoutInput.text.length)
    ) ?: Rect.Zero
    val thickness = DefaultCursorThickness.roundToPx()

    val cursorLeft = if (rtl) {
        textFieldWidth - cursorRect.left - thickness
    } else {
        cursorRect.left
    }

    val cursorRight = if (rtl) {
        textFieldWidth - cursorRect.left
    } else {
        cursorRect.left + thickness
    }
    return cursorRect.copy(left = cursorLeft, right = cursorRight)
}

/**
 * Rounds a negative number to floor, and a positive number to ceil. This is essentially the
 * opposite of [truncate].
 */
private fun Float.roundToNext(): Float = when {
    this.isNaN() || this.isInfinite() -> this
    this > 0 -> ceil(this)
    else -> floor(this)
}
