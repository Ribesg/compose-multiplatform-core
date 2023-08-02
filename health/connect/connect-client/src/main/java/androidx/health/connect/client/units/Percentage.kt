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

package androidx.health.connect.client.units

/** Represents a value as a percentage, not a fraction - for example 100%, 89.62%, etc. */
class Percentage(val value: Double) : Comparable<Percentage> {

    override fun compareTo(other: Percentage): Int = value.compareTo(other.value)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Percentage) return false

        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = "$value%"
}

/** Creates [Percentage] with the specified percentage value, not a fraction. */
@get:JvmSynthetic
val Double.percent: Percentage
    get() = Percentage(value = this)

/** Creates [Percentage] with the specified percentage value, not a fraction. */
@get:JvmSynthetic
val Long.percent: Percentage
    get() = toDouble().percent

/** Creates [Percentage] with the specified percentage value, not a fraction. */
@get:JvmSynthetic
val Float.percent: Percentage
    get() = toDouble().percent

/** Creates [Percentage] with the specified percentage value, not a fraction. */
@get:JvmSynthetic
val Int.percent: Percentage
    get() = toDouble().percent
