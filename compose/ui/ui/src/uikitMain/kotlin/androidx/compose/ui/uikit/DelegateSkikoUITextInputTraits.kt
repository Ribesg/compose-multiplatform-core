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

package androidx.compose.ui.uikit

import androidx.compose.ui.platform.SkikoUITextInputTraits

internal class DelegateSkikoUITextInputTraits(
    val getDelegate: () -> SkikoUITextInputTraits?
) : SkikoUITextInputTraits {
    private val defaultSkikoUITextInputTraits = object : SkikoUITextInputTraits {}
    private val delegateOrDefault get() = getDelegate() ?: defaultSkikoUITextInputTraits

    override fun keyboardType() = delegateOrDefault.keyboardType()
    override fun keyboardAppearance() = delegateOrDefault.keyboardAppearance()
    override fun returnKeyType() = delegateOrDefault.returnKeyType()
    override fun textContentType() = delegateOrDefault.textContentType()
    override fun isSecureTextEntry() = delegateOrDefault.isSecureTextEntry()
    override fun enablesReturnKeyAutomatically() = delegateOrDefault.enablesReturnKeyAutomatically()
    override fun autocapitalizationType() = delegateOrDefault.autocapitalizationType()
    override fun autocorrectionType() = delegateOrDefault.autocorrectionType()
    override fun spellCheckingType() = delegateOrDefault.spellCheckingType()
    override fun smartQuotesType() = delegateOrDefault.smartQuotesType()
    override fun smartDashesType() = delegateOrDefault.smartDashesType()
    override fun smartInsertDeleteType() = delegateOrDefault.smartInsertDeleteType()
}
