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

package androidx.room.compiler.processing.ksp

import androidx.room.compiler.processing.XMethodType
import androidx.room.compiler.processing.XSuspendMethodType
import androidx.room.compiler.processing.XType
import com.squareup.javapoet.TypeVariableName

internal sealed class KspMethodType(
    val env: KspProcessingEnv,
    val origin: KspMethodElement,
    val containing: KspDeclaredType
) : XMethodType {
    override val parameterTypes: List<XType> by lazy {
        origin.parameters.map {
            it.asMemberOf(containing)
        }
    }

    override val typeVariableNames: List<TypeVariableName> by lazy {
        origin.declaration.typeParameters.map {
            val typeParameterBounds = it.bounds.map {
                it.typeName(env.resolver)
            }.toTypedArray()
            TypeVariableName.get(
                it.name.asString(),
                *typeParameterBounds
            )
        }
    }

    /**
     * Creates a MethodType where variance is inherited for java code generation.
     *
     * see [OverrideVarianceResolver] for details.
     */
    fun inheritVarianceForOverride(): XMethodType {
        return OverrideVarianceResolver(env, this).resolve()
    }

    private class KspNormalMethodType(
        env: KspProcessingEnv,
        origin: KspMethodElement,
        containing: KspDeclaredType
    ) : KspMethodType(env, origin, containing) {
        override val returnType: XType by lazy {
            env.wrap(
                originatingReference = origin.declaration.returnType!!,
                ksType = origin.declaration.returnTypeAsMemberOf(
                    resolver = env.resolver,
                    ksType = containing.ksType
                )
            )
        }
    }

    private class KspSuspendMethodType(
        env: KspProcessingEnv,
        origin: KspMethodElement,
        containing: KspDeclaredType
    ) : KspMethodType(env, origin, containing), XSuspendMethodType {
        override val returnType: XType
            // suspend functions always return Any?, no need to call asMemberOf
            get() = origin.returnType

        override fun getSuspendFunctionReturnType(): XType {
            // suspend functions work w/ continuation so it is always boxed
            return env.wrap(
                ksType = origin.declaration.returnTypeAsMemberOf(
                    resolver = env.resolver,
                    ksType = containing.ksType
                ),
                allowPrimitives = false
            )
        }
    }

    companion object {
        fun create(
            env: KspProcessingEnv,
            origin: KspMethodElement,
            containing: KspDeclaredType
        ) = if (origin.isSuspendFunction()) {
            KspSuspendMethodType(env, origin, containing)
        } else {
            KspNormalMethodType(env, origin, containing)
        }
    }
}
