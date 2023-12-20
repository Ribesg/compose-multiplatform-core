/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.room.integration.kotlintestapp.vo

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    primaryKeys = ["mPlaylistId", "mSongId"],
    foreignKeys = [ForeignKey(
        entity = Playlist::class,
        parentColumns = ["mPlaylistId"],
        childColumns = ["mPlaylistId"],
        onDelete = ForeignKey.CASCADE
    ), ForeignKey(
        entity = Song::class,
        parentColumns = ["mSongId"],
        childColumns = ["mSongId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("mPlaylistId"), Index("mSongId")]
)
data class PlaylistSongXRef(val mPlaylistId: Int, val mSongId: Int)
