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

package androidx.tv.tvmaterial.samples

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material.LocalContentColor
import androidx.tv.material.Tab
import androidx.tv.material.TabDefaults
import androidx.tv.material.TabRow
import androidx.tv.material.TabRowDefaults

enum class Navigation(val displayName: String, val action: @Composable () -> Unit) {
  LazyRowsAndColumns("Lazy Rows and Columns", { LazyRowsAndColumns() }),
  FeaturedCarousel("Featured Carousel", { FeaturedCarousel() }),
  ImmersiveList("Immersive List", { SampleImmersiveList() }),
}

@Composable
internal fun TopNavigation(
  updateSelectedTab: (Navigation) -> Unit = {},
) {
  var selectedTabIndex by remember { mutableStateOf(0) }
  val tabs = Navigation.values().map { it.displayName }

  // Pill indicator
  PillIndicatorTabRow(
    tabs = tabs,
    selectedTabIndex = selectedTabIndex,
    updateSelectedTab = { selectedTabIndex = it }
  )

  LaunchedEffect(selectedTabIndex) { updateSelectedTab(Navigation.values()[selectedTabIndex]) }
}

/**
 * Pill indicator tab row for reference
 */
@Composable
fun PillIndicatorTabRow(
  tabs: List<String>,
  selectedTabIndex: Int,
  updateSelectedTab: (Int) -> Unit
) {
  TabRow(
    selectedTabIndex = selectedTabIndex,
    separator = { Spacer(modifier = Modifier.width(12.dp)) },
  ) {
    tabs.forEachIndexed { index, tab ->
      Tab(
        selected = index == selectedTabIndex,
        onSelect = { updateSelectedTab(index) },
      ) {
        Text(
          text = tab,
          fontSize = 12.sp,
          color = LocalContentColor.current,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
      }
    }
  }
}

/**
 * Underlined indicator tab row for reference
 */
@Composable
fun UnderlinedIndicatorTabRow(
  tabs: List<String>,
  selectedTabIndex: Int,
  updateSelectedTab: (Int) -> Unit
) {
  TabRow(
    selectedTabIndex = selectedTabIndex,
    separator = { Spacer(modifier = Modifier.width(12.dp)) },
    indicator = { tabPositions ->
      TabRowDefaults.UnderlinedIndicator(
        currentTabPosition = tabPositions[selectedTabIndex]
      )
    }
  ) {
    tabs.forEachIndexed { index, tab ->
      Tab(
        selected = index == selectedTabIndex,
        onSelect = { updateSelectedTab(index) },
        colors = TabDefaults.underlinedIndicatorTabColors(),
      ) {
        Text(
          text = tab,
          fontSize = 12.sp,
          color = LocalContentColor.current,
          modifier = Modifier.padding(bottom = 4.dp)
        )
      }
    }
  }
}
