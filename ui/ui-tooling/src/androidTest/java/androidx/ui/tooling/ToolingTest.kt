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

package androidx.ui.tooling

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.onGloballyPositioned
import androidx.compose.ui.platform.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SlotTable
import androidx.compose.ui.R
import androidx.compose.ui.platform.AndroidOwner
import org.junit.Before
import org.junit.Rule
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

open class ToolingTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val activityTestRule = androidx.test.rule.ActivityTestRule<TestActivity>(
        TestActivity::class.java
    )
    lateinit var activity: TestActivity
    lateinit var handler: Handler
    lateinit var positionedLatch: CountDownLatch

    @Before
    fun setup() {
        activity = activityTestRule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)

        activityTestRule.onUiThread { handler = Handler(Looper.getMainLooper()) }
    }

    internal fun show(composable: @Composable () -> Unit) {
        positionedLatch = CountDownLatch(1)
        activityTestRule.onUiThread {
            activity.setContent {
                Box(Modifier.onGloballyPositioned { positionedLatch.countDown() }
                    .fillMaxSize()) {
                    composable()
                }
            }
        }

        // Wait for the layout to be performed
        positionedLatch.await(1, TimeUnit.SECONDS)

        // Wait for the UI thread to complete its current work so we know that layout is done.
        activityTestRule.onUiThread { }
    }

    internal fun showAndRecord(content: @Composable () -> Unit): MutableSet<SlotTable>? {

        positionedLatch = CountDownLatch(1)
        val map: MutableSet<SlotTable> = Collections.newSetFromMap(
            WeakHashMap<SlotTable, Boolean>()
        )
        activityTestRule.onUiThread {
            AndroidOwner.onAndroidOwnerCreatedCallback = {
                it.view.setTag(R.id.inspection_slot_table_set, map)
                AndroidOwner.onAndroidOwnerCreatedCallback = null
            }
            activity.setContent {
                Box(Modifier.onGloballyPositioned { positionedLatch.countDown() }
                    .fillMaxSize()) {
                    content()
                }
            }

            // Wait for the layout to be performed
            positionedLatch.await(1, TimeUnit.SECONDS)

            // Wait for the UI thread to complete its current work so we know that layout is done.
            activityTestRule.onUiThread { }
        }
        return map
    }
}

// Kotlin IR compiler doesn't seem too happy with auto-conversion from
// lambda to Runnable, so separate it here
@Suppress("DEPRECATION")
fun androidx.test.rule.ActivityTestRule<TestActivity>.onUiThread(block: () -> Unit) {
    val runnable: Runnable = object : Runnable {
        override fun run() {
            block()
        }
    }
    runOnUiThread(runnable)
}