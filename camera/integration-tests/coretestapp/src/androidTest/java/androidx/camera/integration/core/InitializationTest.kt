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

package androidx.camera.integration.core

import android.content.Context
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CoreAppTestUtil
import androidx.concurrent.futures.await
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.testutils.LocaleTestUtils
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class InitializationTest(private val config: TestConfig) {
    @get:Rule
    val cameraPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.CAMERA)
    @get:Rule
    val storagePermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    @get:Rule
    val recordAudioRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.RECORD_AUDIO)

    data class TestConfig(
        val locale: String
    )

    companion object {
        // See https://developer.android.com/guide/topics/resources/pseudolocales for more
        // information on pseudolocales.
        private const val PSEUDOLOCALE_LTR = "en_XA"
        private const val PSEUDOLOCALE_RTL = "ar_XB"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun spec(): List<TestConfig> {
            return listOf(LocaleTestUtils.DEFAULT_TEST_LANGUAGE, PSEUDOLOCALE_LTR, PSEUDOLOCALE_RTL)
                .map { orientation ->
                    TestConfig(orientation)
                }
        }
    }

    private var providerResult: CameraXViewModel.CameraProviderResult? = null
    private val localeUtil = LocaleTestUtils(ApplicationProvider.getApplicationContext() as Context)

    @Before
    fun setUp() {
        Assume.assumeTrue(CameraUtil.deviceHasCamera())
        CoreAppTestUtil.assumeCompatibleDevice()

        localeUtil.setLocale(config.locale)

        // Clear the device UI before start each test.
        CoreAppTestUtil.clearDeviceUI(InstrumentationRegistry.getInstrumentation())
    }

    @After
    fun tearDown() {
        runBlocking {
            if (providerResult?.hasProvider() == true) {
                providerResult!!.provider!!.shutdown().await()
                providerResult = null
            }
        }

        localeUtil.resetLocale()
    }

    // Use auto-initialization in various locales to ensure the CameraXConfig.Provider which is
    // provided by resources is not translated.
    @Test
    fun canAutoInitialize() {
        with(ActivityScenario.launch(CameraXActivity::class.java)) {
            use {
                val initIdlingResource = withActivity { initializationIdlingResource }

                IdlingRegistry.getInstance().register(initIdlingResource)
                try {
                    Espresso.onIdle()
                    providerResult = withActivity { cameraProviderResult!! }
                } finally {
                    IdlingRegistry.getInstance().unregister(initIdlingResource)
                }

                assertThat(providerResult?.error).isNull()
            }
        }
    }
}