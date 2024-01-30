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

package androidx.compose.foundation.copyPasteAndroidTests.text

import androidx.compose.foundation.assertThat
import androidx.compose.foundation.isGreaterThan
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class BasicTextDensityTest {

    @Test
    fun simpleParagraph_densityChange() = runSkikoComposeUiTest {
        val density1 = Density(1f)
        val density2 = Density(2f)

        var textSize: IntSize? = null
        var ruleDensity by mutableStateOf(density1)
        setContent {
            CompositionLocalProvider(
                LocalDensity provides ruleDensity
            ) {
                BasicText(
                    "Hello World!",
                    modifier = Modifier
                        .onGloballyPositioned {
                            // do not read the size from onTextLayout, it takes a different path
                            textSize = it.size
                        }
                )
            }
        }

        waitForIdle()
        val textSizeDensity1 = textSize!!

        ruleDensity = density2

        waitForIdle()
        val textSizeDensity2 = textSize!!

        assertThat(textSizeDensity2.width).isGreaterThan(textSizeDensity1.width)
        assertThat(textSizeDensity2.height).isGreaterThan(textSizeDensity1.height)
    }

    @Test
    fun simpleMultiParagraph_densityChange() = runSkikoComposeUiTest {
        val density1 = Density(1f)
        val density2 = Density(2f)

        var textSize: IntSize? = null
        var ruleDensity by mutableStateOf(density1)
        setContent {
            CompositionLocalProvider(
                LocalDensity provides ruleDensity
            ) {
                BasicText(
                    // forces BasicText to generate a MultiParagraph
                    text = buildAnnotatedString {
                        withStyle(ParagraphStyle(lineHeight = 20.sp)) {
                            append("Hello")
                        }
                        withStyle(ParagraphStyle(lineHeight = 15.sp)) {
                            append("World")
                        }
                    },
                    modifier = Modifier
                        .onGloballyPositioned {
                            // do not read the size from onTextLayout, it takes a different path
                            textSize = it.size
                        }
                )
            }
        }

        waitForIdle()
        val textSizeDensity1 = textSize!!

        ruleDensity = density2

        waitForIdle()
        val textSizeDensity2 = textSize!!

        assertThat(textSizeDensity2.width).isGreaterThan(textSizeDensity1.width)
        assertThat(textSizeDensity2.height).isGreaterThan(textSizeDensity1.height)
    }

    @Test
    fun selectableText_densityChange() = runSkikoComposeUiTest {
        val density1 = Density(1f)
        val density2 = Density(2f)

        var textSize: IntSize? = null
        var ruleDensity by mutableStateOf(density1)
        setContent {
            CompositionLocalProvider(
                LocalDensity provides ruleDensity
            ) {
                SelectionContainer {
                    // this should internally use the same cache but selectable text takes
                    // a slightly different path. Include a test to make sure everything goes right.
                    BasicText(
                        "Hello World!",
                        modifier = Modifier
                            .onGloballyPositioned {
                                // do not read the size from onTextLayout, it takes a different path
                                textSize = it.size
                            }
                    )
                }
            }
        }

        waitForIdle()
        val textSizeDensity1 = textSize!!

        ruleDensity = density2

        waitForIdle()
        val textSizeDensity2 = textSize!!

        assertThat(textSizeDensity2.width).isGreaterThan(textSizeDensity1.width)
        assertThat(textSizeDensity2.height).isGreaterThan(textSizeDensity1.height)
    }
}
