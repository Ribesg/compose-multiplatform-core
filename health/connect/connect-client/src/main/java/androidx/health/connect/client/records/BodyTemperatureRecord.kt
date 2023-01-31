/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.health.connect.client.records

import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Temperature
import java.time.Instant
import java.time.ZoneOffset

/**
 * Captures the body temperature of a user. Each record represents a single instantaneous body
 * temperature measurement.
 */
public class BodyTemperatureRecord(
    override val time: Instant,
    override val zoneOffset: ZoneOffset?,
    /** Temperature in [Temperature] unit. Required field. Valid range: 0-100 Celsius degrees. */
    public val temperature: Temperature,
    /**
     * Where on the user's body the temperature measurement was taken from. Optional field. Allowed
     * values: [BodyTemperatureMeasurementLocation].
     *
     * @see BodyTemperatureMeasurementLocation
     */
    @property:BodyTemperatureMeasurementLocations
    public val measurementLocation: Int =
        BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_UNKNOWN,
    override val metadata: Metadata = Metadata.EMPTY,
) : InstantaneousRecord {

    /*
     * Generated by the IDE: Code -> Generate -> "equals() and hashCode()".
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BodyTemperatureRecord) return false

        if (temperature != other.temperature) return false
        if (measurementLocation != other.measurementLocation) return false
        if (time != other.time) return false
        if (zoneOffset != other.zoneOffset) return false
        if (metadata != other.metadata) return false

        return true
    }

    /*
     * Generated by the IDE: Code -> Generate -> "equals() and hashCode()".
     */
    override fun hashCode(): Int {
        var result = temperature.hashCode()
        result = 31 * result + measurementLocation
        result = 31 * result + time.hashCode()
        result = 31 * result + (zoneOffset?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }
}
