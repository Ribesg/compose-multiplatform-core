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

package androidx.compose.ui.awt

import androidx.compose.ui.input.key.KeyEvent as ComposeKeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.scene.MultiLayerComposeScene
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.scene.skia.SkiaLayerComponent
import androidx.compose.ui.scene.toPointerKeyboardModifiers
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.density
import java.awt.*
import java.awt.Cursor
import java.awt.event.*
import java.awt.event.KeyEvent
import java.awt.im.InputMethodRequests
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineExceptionHandler
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.*

/**
 * Provides a base implementation for integrating a Compose scene with AWT/Swing.
 * It allows setting Compose content by [setContent], this content should be drawn on [component].
 *
 * This bridge contain 2 components that should be added to the view hirarachy:
 * [component] the main visible Swing component, on which Compose will be shown
 * [invisibleComponent] service component used to bypass Swing issues:
 * - for forcing refocus on input methods change
 */
internal class ComposeBridge(
    layoutDirection: LayoutDirection,
    createSkiaLayerComponent: (ComposeBridge) -> SkiaLayerComponent,
) {
    private var isDisposed = false

    val skiaLayerComponent by lazy { createSkiaLayerComponent(this) } // TODO: Make private
    val component get() = skiaLayerComponent.contentComponent

    private val _invisibleComponent = InvisibleComponent()
    val invisibleComponent: Component get() = _invisibleComponent

    private val clipMap = mutableMapOf<Component, ClipComponent>()

    var currentInputMethodRequests: InputMethodRequests? = null
        private set

    private val windowFocusListener = object : WindowFocusListener {
        override fun windowGainedFocus(e: WindowEvent) = refreshWindowFocus()
        override fun windowLostFocus(e: WindowEvent) = refreshWindowFocus()
    }

    private var window: Window? = null

    private fun refocus() {
        if (component.isFocusOwner) {
            _invisibleComponent.requestFocusTemporary()
            component.requestFocus()
        }
    }

    private val platformComponent: PlatformComponent = object : PlatformComponent {
        override fun enableInput(inputMethodRequests: InputMethodRequests) {
            currentInputMethodRequests = inputMethodRequests
            component.enableInputMethods(true)
            // Without resetting the focus, Swing won't update the status (doesn't show/hide popup)
            // enableInputMethods is design to used per-Swing component level at init stage,
            // not dynamically
            refocus()
        }

        override fun disableInput() {
            currentInputMethodRequests = null
            component.enableInputMethods(false)
            // Without resetting the focus, Swing won't update the status (doesn't show/hide popup)
            // enableInputMethods is design to used per-Swing component level at init stage,
            // not dynamically
            refocus()
        }

        override val locationOnScreen: Point
            get() = component.locationOnScreen

        override val density: Density
            get() = component.density
    }

    private val coroutineExceptionHandler = object :
        AbstractCoroutineContextElement(CoroutineExceptionHandler), CoroutineExceptionHandler {
        override fun handleException(context: CoroutineContext, exception: Throwable) {
            exceptionHandler?.onException(exception) ?: throw exception
        }
    }

    var exceptionHandler: WindowExceptionHandler? = null

    private fun catchExceptions(body: () -> Unit) {
        try {
            body()
        } catch (e: Throwable) {
            exceptionHandler?.onException(e) ?: throw e
        }
    }

    val windowContext = PlatformWindowContext()
    private val desktopTextInputService = DesktopTextInputService(platformComponent)
    private val platformContext = DesktopPlatformContext()
    internal var rootForTestListener: PlatformContext.RootForTestListener? by DelegateRootForTestListener()
    internal var isWindowTransparent by windowContext::isWindowTransparent

    private val semanticsOwnerListener = DesktopSemanticsOwnerListener()
    val accessible = ComposeSceneAccessible {
        semanticsOwnerListener.accessibilityControllers
    }

    private val sceneCoroutineContext = MainUIDispatcher + coroutineExceptionHandler
    private val scene = MultiLayerComposeScene(
        coroutineContext = sceneCoroutineContext,
        composeSceneContext = object : ComposeSceneContext {
            override val platformContext get() = this@ComposeBridge.platformContext
        },
        density = Density(1f),
        layoutDirection = layoutDirection,
        invalidate = {
            skiaLayerComponent.onComposeInvalidation()
        },
    )
    val focusManager by scene::focusManager
    var layoutDirection by scene::layoutDirection

    var compositionLocalContext: CompositionLocalContext? by scene::compositionLocalContext

    val skikoView = object : SkikoView {
        override val input: SkikoInput
            get() = SkikoInput.Empty

        override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
            catchExceptions {
                scene.render(canvas.asComposeCanvas(), nanoTime)
            }
        }
    }

    /**
     * Provides the size of ComposeScene content inside infinity constraints
     *
     * This is needed for the bridge between Compose and Swing since
     * in some cases, Swing's LayoutManagers need
     * to calculate the preferred size of the content without max/min constraints
     * to properly lay it out.
     *
     * Example: Compose content inside Popup without a preferred size.
     * Swing will calculate the preferred size of the Compose content and set Popup's side for that.
     *
     * See [androidx.compose.ui.awt.ComposePanelTest] test `initial panel size of LazyColumn with border layout`
     */
    val preferredSize: Dimension
        get() {
            val contentSize = scene.calculateContentSize()
            return Dimension(
                (contentSize.width / component.density.density).toInt(),
                (contentSize.height / component.density.density).toInt()
            )
        }

    private val density get() = platformComponent.density.density

    /**
     * Keyboard modifiers state might be changed when window is not focused, so window doesn't
     * receive any key events.
     * This flag is set when window focus changes. Then we can rely on it when handling the
     * first movementEvent to get the actual keyboard modifiers state from it.
     * After window gains focus, the first motionEvent.metaState (after focus gained) is used
     * to update windowInfo.keyboardModifiers.
     *
     * TODO: needs to be set `true` when focus changes:
     * (Window focus change is implemented in JB fork, but not upstreamed yet).
     */
    private var keyboardModifiersRequireUpdate = false

    init {
        component.enableInputMethods(false)
        component.addInputMethodListener(object : InputMethodListener {
            override fun caretPositionChanged(event: InputMethodEvent?) {
                if (isDisposed) return
                // Which OSes and which input method could produce such events? We need to have some
                // specific cases in mind before implementing this
            }

            override fun inputMethodTextChanged(event: InputMethodEvent) {
                if (isDisposed) return
                catchExceptions {
                    desktopTextInputService.inputMethodTextChanged(event)
                }
            }
        })

        component.addFocusListener(object : FocusListener {
            override fun focusGained(e: FocusEvent) {
                // We don't reset focus for Compose when the component loses focus temporary.
                // Partially because we don't support restoring focus after clearing it.
                // Focus can be lost temporary when another window or popup takes focus.
                if (!e.isTemporary) {
                    scene.focusManager.requestFocus()
                }
            }

            override fun focusLost(e: FocusEvent) {
                // We don't reset focus for Compose when the component loses focus temporary.
                // Partially because we don't support restoring focus after clearing it.
                // Focus can be lost temporary when another window or popup takes focus.
                if (!e.isTemporary) {
                    scene.focusManager.releaseFocus()
                }
            }
        })

        component.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) = Unit
            override fun mousePressed(event: MouseEvent) = onMouseEvent(event)
            override fun mouseReleased(event: MouseEvent) = onMouseEvent(event)
            override fun mouseEntered(event: MouseEvent) = onMouseEvent(event)
            override fun mouseExited(event: MouseEvent) = onMouseEvent(event)
        })
        component.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(event: MouseEvent) = onMouseEvent(event)
            override fun mouseMoved(event: MouseEvent) = onMouseEvent(event)
        })
        component.addMouseWheelListener { event ->
            onMouseWheelEvent(event)
        }
        component.focusTraversalKeysEnabled = false
        component.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(event: KeyEvent) = onKeyEvent(event)
            override fun keyReleased(event: KeyEvent) = onKeyEvent(event)
            override fun keyTyped(event: KeyEvent) = onKeyEvent(event)
        })
    }

    private fun onMouseEvent(event: MouseEvent): Unit = catchExceptions {
        // AWT can send events after the window is disposed
        if (isDisposed) return@catchExceptions
        if (keyboardModifiersRequireUpdate) {
            keyboardModifiersRequireUpdate = false
            setCurrentKeyboardModifiers(event.keyboardModifiers)
        }
        scene.onMouseEvent(density, event)
    }

    private fun onMouseWheelEvent(event: MouseWheelEvent): Unit = catchExceptions {
        if (isDisposed) return@catchExceptions
        scene.onMouseWheelEvent(density, event)
    }

    private fun onKeyEvent(event: KeyEvent) = catchExceptions {
        if (isDisposed) return@catchExceptions
        desktopTextInputService.onKeyEvent(event)
        setCurrentKeyboardModifiers(event.toPointerKeyboardModifiers())

        val composeEvent = ComposeKeyEvent(event)
        if (onPreviewKeyEvent(composeEvent) ||
            scene.sendKeyEvent(composeEvent) ||
            onKeyEvent(composeEvent)
        ) {
            event.consume()
        }
    }

    fun dispose() {
        check(!isDisposed)
        scene.close()
        skiaLayerComponent.dispose()
        _initContent = null
        isDisposed = true
    }

    private var onPreviewKeyEvent: (ComposeKeyEvent) -> Boolean = { false }
    private var onKeyEvent: (ComposeKeyEvent) -> Boolean = { false }

    fun setKeyEventListeners(
        onPreviewKeyEvent: (ComposeKeyEvent) -> Boolean = { false },
        onKeyEvent: (ComposeKeyEvent) -> Boolean = { false },
    ) {
        this.onPreviewKeyEvent = onPreviewKeyEvent
        this.onKeyEvent = onKeyEvent
    }

    fun setContent(content: @Composable () -> Unit) {
        // If we call it before attaching, everything probably will be fine,
        // but the first composition will be useless, as we set density=1
        // (we don't know the real density if we have unattached component)
        _initContent = {
            catchExceptions {
                scene.setContent(content)
            }
        }
        initContent()
    }

    fun addClipComponent(component: Component) {
        val clipComponent = ClipComponent(component)
        clipMap[component] = clipComponent
        skiaLayerComponent.clipComponents.add(clipComponent)
    }

    fun removeClipComponent(component: Component) {
        clipMap.remove(component)?.let {
            skiaLayerComponent.clipComponents.remove(it)
        }
    }

    private var _initContent: (() -> Unit)? = null

    fun initContent() {
        if (component.isDisplayable) {
            _initContent?.invoke()
            _initContent = null
        }
    }

    private fun setCurrentKeyboardModifiers(modifiers: PointerKeyboardModifiers) {
        windowContext.setKeyboardModifiers(modifiers)
    }

    fun updateSceneSize() {
        val scale = component.density.density
        val size = IntSize(
            width = (component.width * scale).toInt(),
            height = (component.height * scale).toInt()
        )
        windowContext.setContainerSize(size)

        // Zero size will literally limit scene's content size to zero,
        // so it case of late initialization skip this to avoid extra layout run.
        scene.size = size.takeIf { size != IntSize.Zero }
    }

    fun resetSceneDensity() {
        if (scene.density != component.density) {
            scene.density = component.density
            updateSceneSize()
        }
    }

    fun setParentWindow(window: Window?) {
        this.window?.removeWindowFocusListener(windowFocusListener)
        window?.addWindowFocusListener(windowFocusListener)
        this.window = window
        refreshWindowFocus()
    }

    private fun refreshWindowFocus() {
        windowContext.setWindowFocused(window?.isFocused ?: false)
        keyboardModifiersRequireUpdate = true
    }

    private inner class DesktopViewConfiguration : ViewConfiguration by EmptyViewConfiguration {
        override val touchSlop: Float get() = with(platformComponent.density) { 18.dp.toPx() }
    }

    private inner class DesktopFocusManager : FocusManager {
        override fun clearFocus(force: Boolean) {
            val root = component.rootPane
            root?.focusTraversalPolicy?.getDefaultComponent(root)?.requestFocusInWindow()
        }

        override fun moveFocus(focusDirection: FocusDirection): Boolean =
            when (focusDirection) {
                FocusDirection.Next -> {
                    val toFocus = component.focusCycleRootAncestor?.let { root ->
                        val policy = root.focusTraversalPolicy
                        policy.getComponentAfter(root, component)
                            ?: policy.getDefaultComponent(root)
                    }
                    val hasFocus = toFocus?.hasFocus() == true
                    !hasFocus && toFocus?.requestFocusInWindow(FocusEvent.Cause.TRAVERSAL_FORWARD) == true
                }

                FocusDirection.Previous -> {
                    val toFocus = component.focusCycleRootAncestor?.let { root ->
                        val policy = root.focusTraversalPolicy
                        policy.getComponentBefore(root, component)
                            ?: policy.getDefaultComponent(root)
                    }
                    val hasFocus = toFocus?.hasFocus() == true
                    !hasFocus && toFocus?.requestFocusInWindow(FocusEvent.Cause.TRAVERSAL_BACKWARD) == true
                }

                else -> false
            }
    }

    private inner class DesktopSemanticsOwnerListener : PlatformContext.SemanticsOwnerListener {
        /**
         * A new [SemanticsOwner] is always created above existing ones. So, usage of [LinkedHashMap]
         * is required here to keep insertion-order (that equal to [SemanticsOwner]s order).
         */
        private val _accessibilityControllers = linkedMapOf<SemanticsOwner, AccessibilityController>()
        val accessibilityControllers get() = _accessibilityControllers.values.reversed()

        override fun onSemanticsOwnerAppended(semanticsOwner: SemanticsOwner) {
            check(semanticsOwner !in _accessibilityControllers)
            _accessibilityControllers[semanticsOwner] = AccessibilityController(
                owner = semanticsOwner,
                desktopComponent = platformComponent,
                coroutineContext = sceneCoroutineContext,
                onFocusReceived = {
                    skiaLayerComponent.requestNativeFocusOnAccessible(it)
                }
            ).also {
                it.syncLoop()
            }
        }

        override fun onSemanticsOwnerRemoved(semanticsOwner: SemanticsOwner) {
            _accessibilityControllers.remove(semanticsOwner)?.dispose()
        }

        override fun onSemanticsChange(semanticsOwner: SemanticsOwner) {
            _accessibilityControllers[semanticsOwner]?.onSemanticsChange()
        }
    }

    protected inner class DesktopPlatformContext : PlatformContext by PlatformContext.Empty {
        override val windowInfo: WindowInfo get() = windowContext.windowInfo
        override val isWindowTransparent: Boolean get() = windowContext.isWindowTransparent
        override val viewConfiguration: ViewConfiguration = DesktopViewConfiguration()
        override val textInputService: PlatformTextInputService = desktopTextInputService

        override fun setPointerIcon(pointerIcon: PointerIcon) {
            component.cursor =
                (pointerIcon as? AwtCursor)?.cursor ?: Cursor(Cursor.DEFAULT_CURSOR)
        }
        override val parentFocusManager: FocusManager = DesktopFocusManager()
        override fun requestFocus(): Boolean {
            return component.hasFocus() || component.requestFocusInWindow()
        }

        override val rootForTestListener: PlatformContext.RootForTestListener?
            get() = this@ComposeBridge.rootForTestListener
        override val semanticsOwnerListener: PlatformContext.SemanticsOwnerListener?
            get() = this@ComposeBridge.semanticsOwnerListener
    }

    private class InvisibleComponent : Component() {
        fun requestFocusTemporary(): Boolean {
            return super.requestFocus(true)
        }
    }
}

private fun ComposeScene.onMouseEvent(
    density: Float,
    event: MouseEvent
) {
    val eventType = when (event.id) {
        MouseEvent.MOUSE_PRESSED -> PointerEventType.Press
        MouseEvent.MOUSE_RELEASED -> PointerEventType.Release
        MouseEvent.MOUSE_DRAGGED -> PointerEventType.Move
        MouseEvent.MOUSE_MOVED -> PointerEventType.Move
        MouseEvent.MOUSE_ENTERED -> PointerEventType.Enter
        MouseEvent.MOUSE_EXITED -> PointerEventType.Exit
        else -> PointerEventType.Unknown
    }
    sendPointerEvent(
        eventType = eventType,
        position = Offset(event.x.toFloat(), event.y.toFloat()) * density,
        timeMillis = event.`when`,
        type = PointerType.Mouse,
        buttons = event.buttons,
        keyboardModifiers = event.keyboardModifiers,
        nativeEvent = event,
        button = event.getPointerButton()
    )
}

private fun MouseEvent.getPointerButton(): PointerButton? {
    if (button == MouseEvent.NOBUTTON) return null
    return when (button) {
        MouseEvent.BUTTON2 -> PointerButton.Tertiary
        MouseEvent.BUTTON3 -> PointerButton.Secondary
        else -> PointerButton(button - 1)
    }
}

private fun ComposeScene.onMouseWheelEvent(
    density: Float,
    event: MouseWheelEvent
) {
    sendPointerEvent(
        eventType = PointerEventType.Scroll,
        position = Offset(event.x.toFloat(), event.y.toFloat()) * density,
        scrollDelta = if (event.isShiftDown) {
            Offset(event.preciseWheelRotation.toFloat(), 0f)
        } else {
            Offset(0f, event.preciseWheelRotation.toFloat())
        },
        timeMillis = event.`when`,
        type = PointerType.Mouse,
        buttons = event.buttons,
        keyboardModifiers = event.keyboardModifiers,
        nativeEvent = event
    )
}


private val MouseEvent.buttons get() = PointerButtons(
    // We should check [event.button] because of case where [event.modifiersEx] does not provide
    // info about the pressed mouse button when using touchpad on MacOS 12 (AWT only).
    // When the [Tap to click] feature is activated on Mac OS 12, half of all clicks are not
    // handled because [event.modifiersEx] may not provide info about the pressed mouse button.
    isPrimaryPressed = ((modifiersEx and MouseEvent.BUTTON1_DOWN_MASK) != 0
        || (id == MouseEvent.MOUSE_PRESSED && button == MouseEvent.BUTTON1))
        && !isMacOsCtrlClick,
    isSecondaryPressed = (modifiersEx and MouseEvent.BUTTON3_DOWN_MASK) != 0
        || (id == MouseEvent.MOUSE_PRESSED && button == MouseEvent.BUTTON3)
        || isMacOsCtrlClick,
    isTertiaryPressed = (modifiersEx and MouseEvent.BUTTON2_DOWN_MASK) != 0
        || (id == MouseEvent.MOUSE_PRESSED && button == MouseEvent.BUTTON2),
    isBackPressed = (modifiersEx and MouseEvent.getMaskForButton(4)) != 0
        || (id == MouseEvent.MOUSE_PRESSED && button == 4),
    isForwardPressed = (modifiersEx and MouseEvent.getMaskForButton(5)) != 0
        || (id == MouseEvent.MOUSE_PRESSED && button == 5),
)

private val MouseEvent.keyboardModifiers get() = PointerKeyboardModifiers(
    isCtrlPressed = (modifiersEx and InputEvent.CTRL_DOWN_MASK) != 0,
    isMetaPressed = (modifiersEx and InputEvent.META_DOWN_MASK) != 0,
    isAltPressed = (modifiersEx and InputEvent.ALT_DOWN_MASK) != 0,
    isShiftPressed = (modifiersEx and InputEvent.SHIFT_DOWN_MASK) != 0,
    isAltGraphPressed = (modifiersEx and InputEvent.ALT_GRAPH_DOWN_MASK) != 0,
    isSymPressed = false,
    isFunctionPressed = false,
    isCapsLockOn = getLockingKeyStateSafe(KeyEvent.VK_CAPS_LOCK),
    isScrollLockOn = getLockingKeyStateSafe(KeyEvent.VK_SCROLL_LOCK),
    isNumLockOn = getLockingKeyStateSafe(KeyEvent.VK_NUM_LOCK),
)

private fun getLockingKeyStateSafe(
    mask: Int
): Boolean = try {
    Toolkit.getDefaultToolkit().getLockingKeyState(mask)
} catch (_: Exception) {
    false
}

private val MouseEvent.isMacOsCtrlClick
    get() = (
            hostOs.isMacOS &&
                    ((modifiersEx and InputEvent.BUTTON1_DOWN_MASK) != 0) &&
                    ((modifiersEx and InputEvent.CTRL_DOWN_MASK) != 0)
            )
