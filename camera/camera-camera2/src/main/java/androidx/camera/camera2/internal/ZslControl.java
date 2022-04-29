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

package androidx.camera.camera2.internal;

import android.media.ImageWriter;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.impl.SessionConfig;

/**
 * Interface for Zero-Shutter Lag image capture control.
 */
interface ZslControl {

    /**
     * Adds zero-shutter lag config to {@link SessionConfig}.
     *
     * @param resolution surface resolution.
     * @param sessionConfigBuilder session config builder.
     */
    void addZslConfig(
            @NonNull Size resolution,
            @NonNull SessionConfig.Builder sessionConfigBuilder);

    /**
     * Sets zsl disabled or not.
     *
     * @param disabled True if zero-shutter lag should be disabled. Otherwise, should not be
     *                 disabled. However, enabling zero-shutter lag needs other conditions e.g.
     *                 flash mode OFF, so setting to false doesn't guarantee zero-shutter lag to
     *                 be always ON.
     */
    void setZslDisabled(boolean disabled);

    /**
     * Dequeues {@link ImageProxy} from ring buffer.
     *
     * @return {@link ImageProxy}.
     */
    @Nullable
    ImageProxy dequeueImageFromBuffer();

    /**
     * Enqueues image to {@link ImageWriter} for reprocessing capture request.
     *
     * @param imageProxy {@link ImageProxy} to enqueue.
     * @return True if enqueuing image succeeded, otherwise false.
     */
    boolean enqueueImageToImageWriter(@NonNull ImageProxy imageProxy);
}
