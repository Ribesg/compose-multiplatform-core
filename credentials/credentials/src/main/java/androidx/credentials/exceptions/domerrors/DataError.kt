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

package androidx.credentials.exceptions.domerrors

/**
 * During the create public key credential flow, this is thrown when an authenticator response
 * exception contains a data_err code from the fido spec, indicating inadequate data provided. The
 * fido spec can be found [here](https://webidl.spec.whatwg.org/#idl-DOMException-error-names).
 *
 * @see CreatePublicKeyCredentialDomException
 */
@Suppress("ExtendsError") // This is not a real java `Error`
class DataError : DomError(TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_DATA_ERROR) {
    internal companion object {
        internal const val TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_DATA_ERROR: String =
            "androidx.credentials.TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_DATA_ERROR"
    }
}