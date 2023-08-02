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
@file:RestrictTo(RestrictTo.Scope.LIBRARY)

package androidx.health.connect.client.impl.converters.request

import androidx.annotation.RestrictTo
import androidx.health.connect.client.impl.converters.datatype.toDataType
import androidx.health.connect.client.impl.converters.time.toProto
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.platform.client.proto.RequestProto
import kotlin.reflect.KClass

/**
 * Converts public API object into internal proto for ipc.
 *
 * @suppress
 */
fun toDeleteDataRangeRequestProto(
    dataTypeKC: KClass<out Record>,
    timeRangeFilter: TimeRangeFilter
): RequestProto.DeleteDataRangeRequest =
    RequestProto.DeleteDataRangeRequest.newBuilder()
        .addDataType(dataTypeKC.toDataType())
        .setTimeSpec(timeRangeFilter.toProto())
        .build()
