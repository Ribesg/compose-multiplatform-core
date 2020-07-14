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

package androidx.biometric;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class ErrorUtilsTest {
    @Test
    public void testIsKnownError_ReturnsTrue_ForKnownErrors() {
        assertThat(ErrorUtils.isKnownError(BiometricPrompt.ERROR_HW_UNAVAILABLE)).isTrue();
        assertThat(ErrorUtils.isKnownError(BiometricPrompt.ERROR_UNABLE_TO_PROCESS)).isTrue();
        assertThat(ErrorUtils.isKnownError(BiometricPrompt.ERROR_TIMEOUT)).isTrue();
        assertThat(ErrorUtils.isKnownError(BiometricPrompt.ERROR_NO_SPACE)).isTrue();
        assertThat(ErrorUtils.isKnownError(BiometricPrompt.ERROR_CANCELED)).isTrue();
        assertThat(ErrorUtils.isKnownError(BiometricPrompt.ERROR_LOCKOUT)).isTrue();
        assertThat(ErrorUtils.isKnownError(BiometricPrompt.ERROR_VENDOR)).isTrue();
        assertThat(ErrorUtils.isKnownError(BiometricPrompt.ERROR_LOCKOUT_PERMANENT)).isTrue();
        assertThat(ErrorUtils.isKnownError(BiometricPrompt.ERROR_USER_CANCELED)).isTrue();
        assertThat(ErrorUtils.isKnownError(BiometricPrompt.ERROR_NO_BIOMETRICS)).isTrue();
        assertThat(ErrorUtils.isKnownError(BiometricPrompt.ERROR_HW_NOT_PRESENT)).isTrue();
        assertThat(ErrorUtils.isKnownError(BiometricPrompt.ERROR_NEGATIVE_BUTTON)).isTrue();
        assertThat(ErrorUtils.isKnownError(BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL)).isTrue();
    }

    @Test
    public void testIsKnownError_ReturnsFalse_ForUnknownErrors() {
        assertThat(ErrorUtils.isKnownError(-1)).isFalse();
        assertThat(ErrorUtils.isKnownError(1337)).isFalse();
    }

    @Test
    public void testGetFingerprintErrorString_ReturnsEmpty_ForNullContext() {
        assertThat(ErrorUtils.getFingerprintErrorString(
                null /* context */, BiometricPrompt.ERROR_CANCELED)).isEmpty();
    }
}
