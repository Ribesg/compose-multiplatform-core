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

package androidx.playground

import ProjectDependencyGraph
import SkikoSetup
import androidx.build.SettingsParser
import androidx.build.gradle.isRoot
import java.io.File
import java.util.Properties
import javax.inject.Inject
import org.gradle.api.GradleException
import org.gradle.api.initialization.Settings

open class PlaygroundExtension @Inject constructor(
    private val settings: Settings
) {
    private var supportRootDir: File? = null

    /**
     * Includes the project if it does not already exist.
     * This is invoked from `includeProject` to ensure all parent projects are included. If they are
     * not, gradle will use the root project path to set the projectDir, which might conflict in
     * playground. Instead, this method checks if another project in that path exists and if so,
     * changes the project dir to avoid the conflict.
     * see b/197253160 for details.
     */
    private fun includeFakeParentProjectIfNotExists(name: String, projectDir: File) {
        if (name.isEmpty()) return
        if (settings.findProject(name) != null) {
            return
        }
        val actualProjectDir: File = if (settings.findProject(projectDir) != null) {
            // Project directory conflicts with an existing project (possibly root). Move it
            // to another directory to avoid the conflict.
            File(projectDir.parentFile, ".ignore-${projectDir.name}")
        } else {
            projectDir
        }
        includeProjectAt(name, actualProjectDir)
        // Set it to a gradle file that does not exist.
        // We must always include projects starting with root, if we are including nested projects.
        settings.project(name).buildFileName = "ignored.gradle"
    }

    private fun includeProjectAt(name: String, projectDir: File) {
        if (settings.findProject(name) != null) {
            throw GradleException("Cannot include project twice: $name is already included.")
        }
        val parentPath = name.substring(0, name.lastIndexOf(":"))
        val parentDir = projectDir.parentFile
        // Make sure parent is created first. see: b/197253160 for details
        includeFakeParentProjectIfNotExists(
            parentPath,
            parentDir
        )
        settings.include(name)
        settings.project(name).projectDir = projectDir
    }

    /**
     * Initializes the playground project to use public repositories as well as other internal
     * projects that cannot be found in public repositories.
     *
     * @param relativePathToRoot The relative path of the project to the root AndroidX project
     */
    @Suppress("unused") // used from settings.gradle files
    fun setupPlayground(relativePathToRoot: String) {
        // gradlePluginPortal has a variety of unsigned binaries that have proper signatures
        // in mavenCentral, so don't use gradlePluginPortal() if you can avoid it
        settings.pluginManagement.repositories {
            it.mavenCentral()
            it.gradlePluginPortal().content {
                it.includeModule(
                    "org.jetbrains.kotlinx",
                    "kotlinx-benchmark-plugin"
                )
                it.includeModule(
                    "org.jetbrains.kotlinx.benchmark",
                    "org.jetbrains.kotlinx.benchmark.gradle.plugin"
                )
                it.includeModule(
                    "org.jetbrains.kotlin.plugin.serialization",
                    "org.jetbrains.kotlin.plugin.serialization.gradle.plugin"
                )
            }
        }
        SkikoSetup.defineSkikoInVersionCatalog(settings)
        val projectDir = settings.rootProject.projectDir
        val supportRoot = File(projectDir, relativePathToRoot).canonicalFile
        this.supportRootDir = supportRoot
        val buildFile = File(supportRoot, "playground-common/playground-build.gradle")
        val relativePathToBuild = projectDir.toPath().relativize(buildFile.toPath()).toString()

        val playgroundProperties = Properties()
        val propertiesFile = File(supportRoot, "playground-common/playground.properties")
        playgroundProperties.load(propertiesFile.inputStream())
        settings.gradle.beforeProject { project ->
            // load playground properties. These are not kept in the playground projects to prevent
            // AndroidX build from reading them.
            playgroundProperties.forEach {
                project.extensions.extraProperties[it.key as String] = it.value
            }
        }

        settings.rootProject.buildFileName = relativePathToBuild

        // allow public repositories
        System.setProperty("ALLOW_PUBLIC_REPOS", "true")

        // specify out dir location
        System.setProperty("CHECKOUT_ROOT", supportRoot.path)
    }

    /**
     * A convenience method to include projects from the main AndroidX build using a filter.
     *
     * @param filter This filter will be called with the project name (project path in gradle).
     *               If filter returns true, it will be included in the build.
     */
    @Suppress("unused") // used from settings.gradle files
    fun selectProjectsFromAndroidX(filter: (String) -> Boolean) {
        if (supportRootDir == null) {
            throw RuntimeException("Must call setupPlayground() first.")
        }
        val supportSettingsFile = File(supportRootDir, "settings.gradle")
        val projectDependencyGraph = ProjectDependencyGraph(settings, true /*isPlayground*/)
        SettingsParser.findProjects(supportSettingsFile).forEach {
            projectDependencyGraph.addToAllProjects(
                it.gradlePath,
                File(supportRootDir, it.filePath)
            )
        }
        val selectedGradlePaths = projectDependencyGraph.allProjectPaths().filter {
            filter(it)
        }.toSet()

        val allNeededProjects = projectDependencyGraph
            .getAllProjectsWithDependencies(selectedGradlePaths + REQUIRED_PROJECTS)
            .sortedBy { it.v1 } // sort by project path so the parent shows up before children :)

        val unsupportedProjects = allNeededProjects.map { it.v1 }.toSet().filter {
            it in UNSUPPORTED_PROJECTS
        }
        if (unsupportedProjects.isNotEmpty()) {
            val errorMsg = buildString {
                appendLine(
                    """There are unsupported builds in the project. You can break one of the
                        |following dependencies by using projectOrArtifact instead of project
                        |when declaring the dependency.
                """.trimMargin()
                )
                appendLine("----")
                unsupportedProjects.forEach {
                    appendLine("Unsupported Playground Project: $it")
                    appendLine("dependency path to $it from explicitly requested projects:")
                    projectDependencyGraph.findPathsBetween(selectedGradlePaths, it).forEach {
                        appendLine(it)
                    }
                    appendLine("----")
                }
            }
            throw GradleException(errorMsg)
        }
        // pass the list of user specified projects into the AndroidXPlaygroundRootImplPlugin
        // so that it can setup the correct CI tasks.
        settings.gradle.beforeProject { project ->
            if (project.isRoot) {
                project.extensions.extraProperties["primaryProjects"] =
                    selectedGradlePaths.joinToString(",")
            }
        }
        allNeededProjects.forEach {
            includeProjectAt(name = it.v1, projectDir = it.v2)
        }
    }

    companion object {
        private val REQUIRED_PROJECTS = listOf(":lint-checks")
        private val UNSUPPORTED_PROJECTS = listOf(
            ":benchmark:benchmark-common", // requires prebuilts
            ":core:core", // stable aidl, b/270593834
        )
    }
}
