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

package androidx.camera.camera2.internal.compat;

import android.hardware.camera2.CameraCharacteristics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.Collections;
import java.util.Set;

@RequiresApi(21)
class CameraCharacteristicsBaseImpl
        implements CameraCharacteristicsCompat.CameraCharacteristicsCompatImpl {
    @NonNull
    protected final CameraCharacteristics mCameraCharacteristics;
    CameraCharacteristicsBaseImpl(@NonNull CameraCharacteristics cameraCharacteristics) {
        mCameraCharacteristics = cameraCharacteristics;
    }

    @Nullable
    @Override
    public <T> T get(@NonNull CameraCharacteristics.Key<T> key) {
        return mCameraCharacteristics.get(key);
    }

    @NonNull
    @Override
    public Set<String> getPhysicalCameraIds() {
        return Collections.emptySet();
    }

    @NonNull
    @Override
    public CameraCharacteristics unwrap() {
        return mCameraCharacteristics;
    }
}
