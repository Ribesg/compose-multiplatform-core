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

package androidx.camera.core;

import android.graphics.Rect;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.ImageOutputConfig;

import com.google.auto.value.AutoValue;

/**
 * A {@link ResolutionInfo} allows the application to know the resolution information of a
 * specific use case.
 *
 * <p>Applications can know the resolution information of {@link Preview} before a
 * {@link SurfaceRequest} is received, or know the resolution information of
 * {@link ImageAnalysis} before an {@link ImageProxy} is received from
 * {@link ImageAnalysis.Analyzer}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@AutoValue
public abstract class ResolutionInfo {
    /**
     * Creates a new instance of {@link ResolutionInfo} with the given parameters.
     */
    @NonNull
    static ResolutionInfo create(@NonNull Size resolution, @NonNull Rect cropRect,
            @ImageOutputConfig.RotationDegreesValue int rotationDegrees) {
        return new AutoValue_ResolutionInfo(resolution, cropRect, rotationDegrees);
    }

    /**
     * Returns the output resolution used for the use case.
     *
     * <p>The resolution will be expressed in the coordinates of the camera sensor. The
     * dimensions need to be flipped by the rotation degrees obtained via
     * {@link #getRotationDegrees()} to match the target rotation setting.
     */
    @NonNull
    public abstract Size getResolution();

    /**
     * Returns the crop rectangle.
     *
     * <p>The crop rectangle specifies the region of valid pixels in the buffer, using
     * coordinates from <code>(0, 0)</code> to the <code>(width, height)</code> of the resolution
     * obtained by {@link #getResolution}. The dimensions need to be flipped by the rotation
     * degrees obtained via {@link #getRotationDegrees()} to match the target rotation setting.
     *
     * <p>If the use case is configured with a {@link ViewPort}, this value is calculated based
     * on the configuration of {@link ViewPort}; if not, it returns the full rect of the buffer
     * which the dimensions will be the same as the value obtained from {@link #getResolution}.
     */
    @NonNull
    public abstract Rect getCropRect();

    /**
     * Returns the rotation degrees needed to transform the output resolution to the target
     * rotation.
     *
     * <p>This is a clockwise rotation in degrees that needs to be applied to the output buffer
     * so the image appears upright at the target rotation setting. The rotation will be
     * determined by the camera sensor orientation value and target rotation setting.
     *
     * @return The rotation in degrees which will be a value in {0, 90, 180, 270}.
     */
    @ImageOutputConfig.RotationDegreesValue
    public abstract int getRotationDegrees();

    ResolutionInfo() {
    }
}
