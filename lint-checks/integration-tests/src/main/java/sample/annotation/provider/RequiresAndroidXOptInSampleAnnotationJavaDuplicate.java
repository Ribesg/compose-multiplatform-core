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

package sample.annotation.provider;

import static java.lang.annotation.RetentionPolicy.CLASS;

import androidx.annotation.RequiresOptIn;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

// This is essentially a duplicate of RequiresAndroidXOptInSampleAnnotationJava. Combined, these two
// are used in @androidx.annotation.OptIn with multiple @androidx.annotation.RequiresOptIn
// declarations.
@RequiresOptIn
@Retention(CLASS)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RequiresAndroidXOptInSampleAnnotationJavaDuplicate {}
