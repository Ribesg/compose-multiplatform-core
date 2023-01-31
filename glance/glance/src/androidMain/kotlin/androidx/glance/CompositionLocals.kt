/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.DpSize
import androidx.datastore.preferences.core.Preferences
import androidx.glance.color.ColorProviders
import androidx.glance.color.DynamicThemeColorProviders
import androidx.glance.state.GlanceStateDefinition

/**
 * Size of the glance view being generated.
 *
 * The glance view will have at least that much space to be displayed. The exact meaning may
 * changed depending on the surface and how it is configured.
 */
val LocalSize = staticCompositionLocalOf<DpSize> { error("No default size") }

/**
 * Context of application when generating the glance view.
 */
val LocalContext = staticCompositionLocalOf<Context> { error("No default context") }

/**
 * Local view state, defined in surface implementation. A customizable store for view specific state
 * data.
 */
val LocalState = compositionLocalOf<Any?> { null }

/**
 * Unique Id for the glance view being generated by the current composition.
 */
val LocalGlanceId = staticCompositionLocalOf<GlanceId> { error("No default glance id") }

/**
 * Retrieves the current customizable store for view specific state data as defined by
 * [GlanceStateDefinition] in the surface implementation.
 *
 * @return the current store of the provided type [T]
 */
@Composable
inline fun <reified T> currentState(): T = LocalState.current as T

/**
 * Retrieves the current [Preferences] value of the provided [Preferences.Key] from the current
 * state when [Preferences] store is used as [GlanceStateDefinition] in the surface implementation.
 *
 * @param key the [Preferences.Key] to retrieve its value
 * @return the stored value or null if not available.
 */
@Composable
inline fun <reified T> currentState(key: Preferences.Key<T>): T? =
    currentState<Preferences>()[key]

/**
 * The colors currently provided by [GlanceTheme]. If no overrides are provided, it will provide a
 * standard set of Material3 colors, see [DynamicThemeColorProviders]. This should usually be
 * accessed through [GlanceTheme.colors] rather than directly.
 */
internal val LocalColors: ProvidableCompositionLocal<ColorProviders> =
    staticCompositionLocalOf { DynamicThemeColorProviders }
