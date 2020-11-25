/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.input.key

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class KeyInputModifierTest {
    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @OptIn(ExperimentalKeyInput::class)
    @Test
    fun testInspectorValueForKeyInputFilter() {
        val onKeyEvent: (KeyEvent) -> Boolean = { true }
        val modifier = Modifier.keyInputFilter(onKeyEvent) as InspectableValue
        assertThat(modifier.nameFallback).isEqualTo("keyInputFilter")
        assertThat(modifier.valueOverride).isNull()
        assertThat(modifier.inspectableElements.asIterable()).containsExactly(
            ValueElement("onKeyEvent", onKeyEvent)
        )
    }

    @OptIn(ExperimentalKeyInput::class)
    @Test
    fun testInspectorValueForPreviewKeyInputFilter() {
        val onPreviewKeyEvent: (KeyEvent) -> Boolean = { true }
        val modifier = Modifier.previewKeyInputFilter(onPreviewKeyEvent) as InspectableValue
        assertThat(modifier.nameFallback).isEqualTo("previewKeyInputFilter")
        assertThat(modifier.valueOverride).isNull()
        assertThat(modifier.inspectableElements.asIterable()).containsExactly(
            ValueElement("onPreviewKeyEvent", onPreviewKeyEvent)
        )
    }
}
