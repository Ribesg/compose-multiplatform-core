/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.tools.core.generator

import androidx.privacysandbox.tools.core.model.AnnotatedInterface
import androidx.privacysandbox.tools.core.model.Method
import androidx.privacysandbox.tools.core.model.Parameter
import androidx.privacysandbox.tools.core.model.ParsedApi
import androidx.privacysandbox.tools.core.model.Type
import androidx.privacysandbox.tools.core.model.Types
import androidx.privacysandbox.tools.testing.loadFilesFromDirectory
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AidlInterfaceGeneratorTest {
    @Test
    fun generate() {
        val api = ParsedApi(
            services = setOf(
                AnnotatedInterface(
                    type = Type(packageName = "com.mysdk", simpleName = "MySdk"),
                    methods = listOf(
                        Method(
                            name = "methodWithInterfaceParam",
                            parameters = listOf(
                                Parameter("myInterface", Type("com.mysdk", "MyInterface"))
                            ),
                            returnType = Types.unit,
                            isSuspend = false,
                        ),
                        Method(
                            name = "suspendMethodWithInterfaceReturn",
                            parameters = listOf(),
                            returnType = Type("com.mysdk", "MyInterface"),
                            isSuspend = true,
                        )
                    )
                )
            ),
            interfaces = setOf(
                AnnotatedInterface(
                    type = Type(packageName = "com.mysdk", simpleName = "MyInterface"),
                    methods = listOf(
                        Method(
                            name = "methodWithInterfaceParam",
                            parameters = listOf(
                                Parameter("myInterface", Type("com.mysdk", "MyInterface"))
                            ),
                            returnType = Types.unit,
                            isSuspend = false,
                        ),
                        Method(
                            name = "suspendMethodWithInterfaceReturn",
                            parameters = listOf(),
                            returnType = Type("com.mysdk", "MyInterface"),
                            isSuspend = true,
                        )
                    )
                )
            )
        )

        val (aidlGeneratedSources, javaGeneratedSources) = AidlTestHelper.runGenerator(api)
        assertThat(javaGeneratedSources.map { it.packageName to it.interfaceName })
            .containsExactly(
                "com.mysdk" to "IMySdk",
                "com.mysdk" to "IMyInterface",
                "com.mysdk" to "ICancellationSignal",
                "com.mysdk" to "IMyInterfaceTransactionCallback",
                "com.mysdk" to "PrivacySandboxThrowableParcel",
                "com.mysdk" to "ParcelableStackFrame",
            )

        val outputTestDataDir = File("src/test/test-data/aidlinterfacegeneratortest/output")
        val expectedSources = loadFilesFromDirectory(outputTestDataDir)

        assertThat(aidlGeneratedSources.map { it.relativePath to it.content })
            .containsExactlyElementsIn(expectedSources)
    }
}