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

package androidx.appactions.builtintypes.experimental.types

import androidx.appactions.builtintypes.experimental.properties.Name

interface GenericErrorStatus : Thing {
    override fun toBuilder(): Builder<*>

    companion object {
        @JvmStatic
        fun Builder(): Builder<*> = GenericErrorStatusBuilderImpl()
    }

    interface Builder<Self : Builder<Self>> : Thing.Builder<Self> {
        override fun build(): GenericErrorStatus
    }
}

private class GenericErrorStatusBuilderImpl :
    GenericErrorStatus.Builder<GenericErrorStatusBuilderImpl> {

    private var identifier: String? = null
    private var name: Name? = null

    override fun build() = GenericErrorStatusImpl(identifier, name)

    override fun setIdentifier(text: String?): GenericErrorStatusBuilderImpl =
        apply { identifier = text }

    override fun setName(text: String): GenericErrorStatusBuilderImpl = apply { name = Name(text) }

    override fun setName(name: Name?): GenericErrorStatusBuilderImpl = apply { this.name = name }

    override fun clearName(): GenericErrorStatusBuilderImpl = apply { name = null }
}

private class GenericErrorStatusImpl(override val identifier: String?, override val name: Name?) :
    GenericErrorStatus {
    override fun toBuilder(): GenericErrorStatus.Builder<*> =
        GenericErrorStatusBuilderImpl().setIdentifier(identifier).setName(name)

    override fun toString(): String = "GenericErrorStatus"
}
