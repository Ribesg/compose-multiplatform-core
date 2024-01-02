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

package androidx.baselineprofile.gradle.utils

import com.android.build.api.AndroidPluginVersion

// Minimum AGP version required
internal val MIN_AGP_VERSION_REQUIRED = AndroidPluginVersion(8, 0, 0)
internal val MAX_AGP_VERSION_REQUIRED = AndroidPluginVersion(8, 3, 0)

// Prefix for the build type baseline profile
internal const val BUILD_TYPE_BASELINE_PROFILE_PREFIX = "nonMinified"
internal const val BUILD_TYPE_BENCHMARK_PREFIX = "benchmark"

// Configuration consumed by this plugin that carries the baseline profile HRF file.
internal const val CONFIGURATION_NAME_BASELINE_PROFILES = "baselineProfile"

// Custom attributes to match the baseline profile configuration
internal const val ATTRIBUTE_USAGE_BASELINE_PROFILE = "baselineProfile"
internal const val ATTRIBUTE_TARGET_JVM_ENVIRONMENT = "android"
internal const val ATTRIBUTE_BASELINE_PROFILE_PLUGIN_VERSION = "alpha1"

// Specifies the artifact type in the variant configuration
internal const val CONFIGURATION_ARTIFACT_TYPE = "baselineProfile"

// Base folder for artifacts generated by the tasks
internal const val INTERMEDIATES_BASE_FOLDER = "intermediates/baselineprofiles"

// Specifies the suffix for each baseline profile task. Note that tasks have the following
// structure: <action><variant><suffix>. For example, if action is `generate`, variant is `release`
// and suffix is `baselineProfile` the task name will be `generateReleaseBaselineProfile`.
internal const val TASK_NAME_SUFFIX = "baselineProfile"

// Other constants
internal const val RELEASE = "release"

// Kotlin Multiplatform Plugin ID
internal const val KOTLIN_MULTIPLATFORM_PLUGIN_ID = "org.jetbrains.kotlin.multiplatform"

// Instrumentation runner arguments
internal const val INSTRUMENTATION_ARG_ENABLED_RULES = "androidx.benchmark.enabledRules"
internal const val INSTRUMENTATION_ARG_ENABLED_RULES_BASELINE_PROFILE = "baselineprofile"
internal const val INSTRUMENTATION_ARG_ENABLED_RULES_BENCHMARK = "macrobenchmark"

// This should be aligned with `androidx.benchmark.Arguments#targetPackageName`
internal const val INSTRUMENTATION_ARG_TARGET_PACKAGE_NAME =
    "androidx.benchmark.targetPackageName"
