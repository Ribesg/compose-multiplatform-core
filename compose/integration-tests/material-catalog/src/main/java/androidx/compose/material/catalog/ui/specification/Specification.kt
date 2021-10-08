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

package androidx.compose.material.catalog.ui.specification

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.catalog.R
import androidx.compose.material.catalog.model.Specification
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues

// TODO: Use components/values from Material3 when available
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun Specification(
    specifications: List<Specification>,
    onSpecificationClick: (specification: Specification) -> Unit
) {
    SpecificationScaffold(
        topBarTitle = stringResource(id = R.string.compose_material_catalog)
    ) { paddingValues ->
        BoxWithConstraints(modifier = Modifier.padding(paddingValues)) {
            LazyColumn(
                content = {
                    items(specifications) { specification ->
                        SpecificationItem(
                            specification = specification,
                            onClick = onSpecificationClick
                        )
                    }
                },
                contentPadding = rememberInsetsPaddingValues(
                    insets = LocalWindowInsets.current.navigationBars
                )
            )
        }
    }
}
