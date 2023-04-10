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

package androidx.car.app.hardware.common;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.ExperimentalCarApi;

import com.google.auto.value.AutoValue;

/**
 * Container class for information about property Ids and area Ids.
 *
 * <p>The hash code generated by the auto class is used as uId.
 * @hide
 */
@RestrictTo(LIBRARY)
@AutoValue
public abstract class PropertyIdAreaId {

    /** Returns one of the property Ids in {@link android.car.VehiclePropertyIds}. */
    public abstract int getPropertyId();

    /** Returns one of area Ids in {@link android.car.VehicleAreaSeat}. */
    public abstract int getAreaId();

    /** Get a builder class for {@link PropertyIdAreaId}*/
    @NonNull
    @OptIn(markerClass = ExperimentalCarApi.class)
    public static PropertyIdAreaId.Builder builder() {
        return new AutoValue_PropertyIdAreaId.Builder().setAreaId(0);
    }

    /**
     * A builder for {@link PropertyIdAreaId}
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /** Sets a property ID for the {@link PropertyIdAreaId}. */
        @NonNull
        public abstract PropertyIdAreaId.Builder setPropertyId(int propertyId);

        /** Sets an area Id for the {@link PropertyIdAreaId}. */
        @NonNull
        public abstract PropertyIdAreaId.Builder setAreaId(int areaId);

        /** Create an instance of {@link PropertyIdAreaId}. */
        @NonNull
        public abstract PropertyIdAreaId build();
    }

}
