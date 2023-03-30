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

package androidx.credentials

import android.os.Bundle
import androidx.credentials.CreateCredentialResponse.Companion.createFrom
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class CreatePublicKeyCredentialResponseTest {

    @Test
    fun constructor_emptyJson_throwsIllegalArgumentException() {
        Assert.assertThrows(
            "Expected empty Json to throw error",
            IllegalArgumentException::class.java
        ) { CreatePublicKeyCredentialResponse("") }
    }

    @Test
    fun constructor_success() {
        CreatePublicKeyCredentialResponse("{\"hi\":1}")
    }

    @Test
    fun getter_registrationResponseJson_success() {
        val testJsonExpected = "{\"input\":5}"
        val createPublicKeyCredentialResponse = CreatePublicKeyCredentialResponse(testJsonExpected)
        val testJsonActual = createPublicKeyCredentialResponse.registrationResponseJson
        assertThat(testJsonActual).isEqualTo(testJsonExpected)
    }

    @Test
    fun getter_frameworkProperties_success() {
        val registrationResponseJsonExpected = "{\"input\":5}"
        val expectedData = Bundle()
        expectedData.putString(
            CreatePublicKeyCredentialResponse.BUNDLE_KEY_REGISTRATION_RESPONSE_JSON,
            registrationResponseJsonExpected
        )

        val response = CreatePublicKeyCredentialResponse(registrationResponseJsonExpected)

        assertThat(response.type).isEqualTo(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL)
        assertThat(equals(response.data, expectedData)).isTrue()
    }

    @Test
    fun frameworkConversion_success() {
        val response = CreatePublicKeyCredentialResponse("responseJson")

        val convertedResponse = createFrom(response.type, response.data)

        assertThat(convertedResponse).isInstanceOf(
            CreatePublicKeyCredentialResponse::class.java
        )
        val convertedSubclassResponse = convertedResponse as CreatePublicKeyCredentialResponse
        assertThat(convertedSubclassResponse.registrationResponseJson)
            .isEqualTo(response.registrationResponseJson)
    }
}