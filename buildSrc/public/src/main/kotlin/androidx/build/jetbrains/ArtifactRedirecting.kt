/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.build.jetbrains

import org.gradle.api.Project

data class ArtifactRedirecting(
    val groupId: String,
    val version: String,
    val targetNames: Set<String>,
)

fun Project.artifactRedirecting(): ArtifactRedirecting {
    val groupId = findProperty("artifactRedirecting.androidx.groupId") as? String
    // for androidx.compose modules we didn't add this property to every gradle.properties,
    // but we can comply to this convention:
        ?: project.group.toString().replace("org.jetbrains.", "androidx.")

    val version = findProperty("artifactRedirecting.${groupId}.version") as? String
        // artifactRedirecting for compose was added before all other libs,
        // therefore it's a default:
        ?: findProperty("artifactRedirecting.androidx.compose.version") as String

    val targetNames = (findProperty("artifactRedirecting.publication.targetNames") as? String ?: "")
        .split(",").toSet()

    return ArtifactRedirecting(
        groupId = groupId,
        version = version,
        targetNames = targetNames,
    )
}