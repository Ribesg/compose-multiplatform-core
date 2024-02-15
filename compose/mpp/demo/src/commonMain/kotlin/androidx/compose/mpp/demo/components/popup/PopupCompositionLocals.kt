/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.mpp.demo.components.popup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val LocalString = staticCompositionLocalOf<String> {
    error("CompositionLocal LocalString not present")
}

@Composable
fun PopupCompositionLocalExample() {
    var current by remember { mutableStateOf("test 0") }
    LaunchedEffect(Unit) {
        var i = 1
        while (coroutineContext.isActive) {
            delay(500)
            current = "test $i"
            i++
        }
    }
    CompositionLocalProvider(LocalString provides current) {
        MaterialTheme {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Popup {
                    Box(Modifier.size(200.dp).background(Color.Yellow), contentAlignment = Alignment.Center) {
                        Text("LocalString: ${LocalString.current}")
                    }
                }
            }
        }
    }
}
