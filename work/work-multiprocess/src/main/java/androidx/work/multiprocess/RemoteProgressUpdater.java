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

package androidx.work.multiprocess;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.Data;
import androidx.work.ProgressUpdater;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.UUID;

/**
 * Updates progress for a {@link androidx.work.multiprocess.RemoteListenableWorker}.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteProgressUpdater implements ProgressUpdater {
    @NonNull
    @Override
    public ListenableFuture<Void> updateProgress(
            @NonNull Context context,
            @NonNull UUID id,
            @NonNull Data data) {
        return RemoteWorkManager.getInstance(context).setProgress(id, data);
    }
}
