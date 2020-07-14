/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.semantics

import androidx.ui.text.AnnotatedString
import kotlin.reflect.KProperty

/**
 * General semantics properties, mainly used for accessibility.
 */
object SemanticsProperties {
    /**
     * Developer-set content description of the semantics node. If this is not set, accessibility
     * services will present the [Text] of this node as content part.
     *
     * @see SemanticsPropertyReceiver.accessibilityLabel
     */
    val AccessibilityLabel = SemanticsPropertyKey<String>("AccessibilityLabel")

    /**
     * Developer-set state description of the semantics node. For example: on/off. If this not
     * set, accessibility services will derive the state from other semantics properties, like
     * [AccessibilityRangeInfo], but it is not guaranteed and the format will be decided by
     * accessibility services.
     *
     * @see SemanticsPropertyReceiver.accessibilityValue
     */
    val AccessibilityValue = SemanticsPropertyKey<String>("AccessibilityValue")

    /**
     * The node is a range with current value.
     *
     * @see SemanticsPropertyReceiver.accessibilityValueRange
     */
    val AccessibilityRangeInfo =
        SemanticsPropertyKey<AccessibilityRangeInfo>("AccessibilityRangeInfo")

    /**
     * Whether this semantics node is disabled.
     *
     * @see SemanticsPropertyReceiver.disabled
     */
    val Disabled = SemanticsPropertyKey<Unit>("Disabled")

    /**
     * Whether this semantics node is hidden.
     *
     * @see SemanticsPropertyReceiver.hidden
     */
    val Hidden = SemanticsPropertyKey<Unit>("Hidden")

    /**
     * Whether this semantics node represents a Popup. Not to be confused with if this node is
     * _part of_ a Popup.
     *
     * @see SemanticsPropertyReceiver.popup
     */
    val IsPopup = SemanticsPropertyKey<Unit>("IsPopup")

    // TODO(b/138172781): Move to FoundationSemanticsProperties
    /**
     * Test tag attached to this semantics node.
     *
     * @see SemanticsPropertyReceiver.testTag
     */
    val TestTag = SemanticsPropertyKey<String>("TestTag")

    /**
     * Text of the semantics node. It must be the actual text displayed by this component instead
     * of developer-set content description.
     *
     * @see SemanticsPropertyReceiver.text
     */
    val Text = SemanticsPropertyKey<AnnotatedString>("Text")
}

/**
 * Ths object defines keys of the actions which can be set in semantics and performed on the
 * semantics node.
 */
object SemanticsActions {
    /**
     * Action to be performed when the node is clicked.
     *
     * @see SemanticsPropertyReceiver.onClick
     */
    val OnClick = SemanticsPropertyKey<AccessibilityAction<() -> Boolean>>("OnClick")

    /**
     * Action to scroll to a specified position.
     *
     * @see SemanticsPropertyReceiver.ScrollBy
     */
    val ScrollBy =
        SemanticsPropertyKey<AccessibilityAction<(x: Float, y: Float) -> Boolean>>("ScrollBy")

    /**
     * Action to scroll the content forward.
     *
     * @see SemanticsPropertyReceiver.scrollForward
     */
    @Deprecated("Use scroll up/down/left/right instead. Need more discussion")
    // TODO(b/157692376): remove scroll forward/backward api together with slider scroll action.
    val ScrollForward =
        SemanticsPropertyKey<AccessibilityAction<() -> Boolean>>("ScrollForward")

    /**
     * Action to scroll the content backward.
     *
     * @see SemanticsPropertyReceiver.scrollBackward
     */
    @Deprecated("Use scroll up/down/left/right instead. Need more discussion.")
    // TODO(b/157692376): remove scroll forward/backward api together with slider scroll action.
    val ScrollBackward =
        SemanticsPropertyKey<AccessibilityAction<() -> Boolean>>("ScrollForward")

    /**
     * Action to set slider progress.
     *
     * @see SemanticsPropertyReceiver.setProgress
     */
    val SetProgress =
        SemanticsPropertyKey<AccessibilityAction<(progress: Float) -> Boolean>>("SetProgress")

    /**
     * Custom actions which are defined by app developers.
     *
     * @see SemanticsPropertyReceiver.customActions
     */
    val CustomActions =
        SemanticsPropertyKey<List<CustomAccessibilityAction>>("CustomActions")
}

class SemanticsPropertyKey<T>(
    /**
     * The name of the property.  Should be the same as the constant from which it is accessed.
     */
    val name: String
) {
    /**
     * Throws [UnsupportedOperationException].  Should not be called.
     */
    // TODO(KT-6519): Remove this getter
    // TODO(KT-32770): Cannot deprecate this either as the getter is considered called by "by"
    final operator fun getValue(thisRef: SemanticsPropertyReceiver, property: KProperty<*>): T {
        throw UnsupportedOperationException(
            "You cannot retrieve a semantics property directly - " +
                    "use one of the SemanticsConfiguration.getOr* methods instead"
        )
    }

    final operator fun setValue(
        thisRef: SemanticsPropertyReceiver,
        property: KProperty<*>,
        value: T
    ) {
        thisRef[this] = value
    }

    override fun toString(): String {
        return "SemanticsPropertyKey: $name"
    }
}

/**
 * Data class for standard accessibility action.
 *
 * @param label The description of this action
 * @param action The function to invoke when this action is performed. The function should return
 * a boolean result indicating whether the action is successfully handled. For example, a scroll
 * forward action should return false if the widget is not enabled or has reached the end of the
 * list.
 */
data class AccessibilityAction<T : Function<Boolean>>(val label: CharSequence?, val action: T)

/**
 * Data class for custom accessibility action.
 *
 * @param label The description of this action
 * @param action The function to invoke when this action is performed. The function should have no
 * arguments and return a boolean result indicating whether the action is successfully handled.
 */
data class CustomAccessibilityAction(val label: CharSequence, val action: () -> Boolean)

data class AccessibilityRangeInfo(
    val current: Float,
    val range: ClosedFloatingPointRange<Float>
)

interface SemanticsPropertyReceiver {
    operator fun <T> set(key: SemanticsPropertyKey<T>, value: T)
}

/**
 * Developer-set content description of the semantics node. If this is not set, accessibility
 * services will present the text of this node as content part.
 *
 * @see SemanticsProperties.AccessibilityLabel
 */
var SemanticsPropertyReceiver.accessibilityLabel by SemanticsProperties.AccessibilityLabel

/**
 * Developer-set state description of the semantics node. For example: on/off. If this not
 * set, accessibility services will derive the state from other semantics properties, like
 * [AccessibilityRangeInfo], but it is not guaranteed and the format will be decided by
 * accessibility services.
 *
 * @see SemanticsProperties.AccessibilityValue
 */
var SemanticsPropertyReceiver.accessibilityValue by SemanticsProperties.AccessibilityValue

/**
 * The node is a range with current value.
 *
 * @see SemanticsProperties.AccessibilityRangeInfo
 */
var SemanticsPropertyReceiver.accessibilityValueRange by SemanticsProperties.AccessibilityRangeInfo

/**
 * Whether this semantics node is disabled.
 *
 * @see SemanticsProperties.Disabled
 */
fun SemanticsPropertyReceiver.disabled() {
    this[SemanticsProperties.Disabled] = Unit
}

/**
 * Whether this semantics node is hidden.
 *
 * @See SemanticsProperties.Hidden
 */
fun SemanticsPropertyReceiver.hidden() {
    this[SemanticsProperties.Hidden] = Unit
}

/**
 * Whether this semantics node represents a Popup. Not to be confused with if this node is
 * _part of_ a Popup.
 *
 * @See SemanticsProperties.IsPopup
 */
fun SemanticsPropertyReceiver.popup() {
    this[SemanticsProperties.IsPopup] = Unit
}

// TODO(b/138172781): Move to FoundationSemanticsProperties.kt
/**
 * Test tag attached to this semantics node.
 *
 * @see SemanticsPropertyReceiver.testTag
 */
var SemanticsPropertyReceiver.testTag by SemanticsProperties.TestTag

/**
 * Text of the semantics node. It must be real text instead of developer-set content description.
 *
 * @see SemanticsProperties.Text
 */
var SemanticsPropertyReceiver.text by SemanticsProperties.Text

/**
 * Custom actions which are defined by app developers.
 *
 * @see SemanticsPropertyReceiver.customActions
 */
var SemanticsPropertyReceiver.customActions by SemanticsActions.CustomActions

/**
 * This function adds the [SemanticsActions.OnClick] to the [SemanticsPropertyReceiver].
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.OnClick] is called.
 */
fun SemanticsPropertyReceiver.onClick(label: String? = null, action: () -> Boolean) {
    this[SemanticsActions.OnClick] = AccessibilityAction(label, action)
}

/**
 * This function adds the [SemanticsActions.ScrollBy] to the [SemanticsPropertyReceiver].
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.ScrollBy] is called.
 */
fun SemanticsPropertyReceiver.scrollBy(
    label: String? = null,
    action: (x: Float, y: Float) -> Boolean
) {
    this[SemanticsActions.ScrollBy] = AccessibilityAction(label, action)
}

/**
 * This function adds the [SemanticsActions.ScrollForward] to the [SemanticsPropertyReceiver].
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.ScrollForward] is called.
 */
// TODO(b/157692376): remove scroll forward/backward api together with slider scroll action.
@Deprecated("Use scroll up/down/left/right instead")
fun SemanticsPropertyReceiver.scrollForward(label: String? = null, action: () -> Boolean) {
    @Suppress("DEPRECATION")
    this[SemanticsActions.ScrollForward] = AccessibilityAction(label, action)
}

/**
 * This function adds the [SemanticsActions.ScrollBackward] to the [SemanticsPropertyReceiver].
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.ScrollBackward] is called.
 */
// TODO(b/157692376): remove scroll forward/backward api together with slider scroll action.
@Deprecated("Use scroll up/down/left/right instead")
fun SemanticsPropertyReceiver.scrollBackward(label: String? = null, action: () -> Boolean) {
    @Suppress("DEPRECATION")
    this[SemanticsActions.ScrollBackward] = AccessibilityAction(label, action)
}

/**
 * This function adds the [SemanticsActions.SetProgress] to the [SemanticsPropertyReceiver].
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.SetProgress] is called.
 */
fun SemanticsPropertyReceiver.setProgress(
    label: String? = null,
    action: (progress: Float) -> Boolean
) {
    this[SemanticsActions.SetProgress] = AccessibilityAction(label, action)
}
