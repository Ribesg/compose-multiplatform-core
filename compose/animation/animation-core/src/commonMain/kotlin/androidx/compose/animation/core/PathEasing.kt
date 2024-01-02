/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.animation.core

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathSegment

/**
 * An easing function for an arbitrary [Path].
 *
 * The [Path] must begin at `(0, 0)` and end at `(1, 1)`. The x-coordinate along the
 * [Path] is the input value and the output is the y coordinate of the line at that
 * point. This means that the Path must conform to a function `y = f(x)`.
 *
 * The [Path] must be continuous along the x axis. The [Path] should also be
 * monotonically increasing along the x axis. If the [Path] is not monotonic and
 * there are multiple y values for a given x, the chosen y value is implementation
 * dependent and may vary.
 *
 * The [Path] must not contain any [Path.close] command as it would force the path
 * to restart from the beginning.
 *
 * This is equivalent to the Android `PathInterpolator`.
 *
 * [CubicBezierEasing] should be used if a single bezier curve is required as it
 * performs fewer allocations. [PathEasing] should be used when creating an
 * arbitrary path.
 *
 * Note: a [PathEasing] instance can be used from any thread, but not concurrently.
 *
 * @sample androidx.compose.animation.core.samples.PathEasingSample
 *
 * @param path The [Path] to use to make the curve representing the easing curve.
 *
 */
@Immutable
class PathEasing(private val path: Path) : Easing {
    private lateinit var intervals: IntervalTree<PathSegment>

    override fun transform(fraction: Float): Float {
        if (fraction <= 0.0f) {
            return 0.0f
        } else if (fraction >= 1.0f) {
            return 1.0f
        }

        if (!::intervals.isInitialized) {
            val roots = FloatArray(5)

            // Using an interval tree is a bit heavy handed but since we are dealing with
            // easing curves, we don't expect many segments, and therefore few allocations.
            // The interval tree allows us to quickly query for the correct segment inside
            // the transform() function.
            val segmentIntervals = IntervalTree<PathSegment>().apply {
                for (segment in path) {
                    require(segment.type != PathSegment.Type.Close) {
                        "The path cannot contain a close() command."
                    }
                    if (segment.type != PathSegment.Type.Move &&
                        segment.type != PathSegment.Type.Done
                    ) {
                        val bounds = computeHorizontalBounds(segment, roots)
                        this += Interval(bounds.first, bounds.second, segment)
                    }
                }
            }

            require(0.0f in segmentIntervals) {
                "The easing path must start at 0.0f."
            }

            require(1.0f in segmentIntervals) {
                "The easing path must end at 1.0f."
            }

            intervals = segmentIntervals
        }

        val result = intervals.findFirstOverlap(fraction)
        val segment = checkNotNull(result.data) {
            "The easing path is invalid. Make sure it is continuous on the x axis."
        }

        val t = findFirstRoot(segment, fraction)
        check(!t.isNaN()) {
            "The easing path is invalid. Make sure it does not contain NaN/Infinity values."
        }

        return evaluateY(segment, t).coerceAtLeast(0.0f).coerceAtMost(1.0f)
    }
}
