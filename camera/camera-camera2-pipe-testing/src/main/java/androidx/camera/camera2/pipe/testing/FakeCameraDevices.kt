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

package androidx.camera.camera2.pipe.testing

import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraDevices
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import kotlinx.coroutines.runBlocking

/**
 * This provides a fake implementation of [CameraDevices] for tests with a fixed list of Cameras.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class FakeCameraDevices(
    private val cameras: List<CameraMetadata>
) : CameraDevices {
    override fun findAll(): List<CameraId> = runBlocking { ids() }
    override suspend fun ids(): List<CameraId> = cameras.map { it.camera }

    override suspend fun getMetadata(camera: CameraId): CameraMetadata = awaitMetadata(camera)
    override fun awaitMetadata(camera: CameraId): CameraMetadata = cameras.first {
        it.camera == camera
    }
}