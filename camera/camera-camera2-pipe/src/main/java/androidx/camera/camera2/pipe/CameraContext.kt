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

package androidx.camera.camera2.pipe

import android.content.Context
import androidx.camera.camera2.pipe.core.Threads

/**
 * Provides access to shared application-level resources that may be configured or managed by a
 * single [CameraPipe] instance, such as standard executors, threads, dispatchers, and more.
 *
 * [CameraContext] is primarily used to share resources between [CameraBackend] factories and
 * between [CameraController] instances.
 */
interface CameraContext {
    val appContext: Context
    val threads: Threads
    val cameraBackends: CameraBackends
}