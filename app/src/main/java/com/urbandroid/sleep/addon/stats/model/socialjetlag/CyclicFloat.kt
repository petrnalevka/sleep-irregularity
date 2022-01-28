package com.urbandroid.sleep.addon.stats.model.socialjetlag

import com.urbandroid.common.util.math.sun.MoreMath
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sqrt

/** ----------------------------------------------------------------------------------
 *  ----------------------------------------------------------------------------------
 *
 *  When changing anything here, make the same change to CyclicDouble. It sucks, I know.
 *
 *  ----------------------------------------------------------------------------------
 *  ----------------------------------------------------------------------------------
 */

/**
 * Map x to the interval [0, cycle). Like modulo arithmetic, but on a continuous domain (floats).
 */
fun normalize(x: Float, cycle: Float): Float {
    if (cycle <= 0 ) {
        throw IllegalArgumentException("Cycle must be positive: $cycle")
    }
    return if (x >= 0) {
        if (x < cycle) {
            x
        } else {
            fixRoundingErrors(x - floor(x / cycle) * cycle, cycle)
        }
    } else {
        if (x > -cycle) {
            fixRoundingErrors(cycle + x, cycle)
        } else {
            fixRoundingErrors(cycle + x - ceil(x / cycle) * cycle, cycle)
        }
    }
}

/**
 * Just to deal with minor numeric errors, like (1/3)*3 = 1.0000001
 */
private fun fixRoundingErrors(x: Float, cycle: Float) = if (x < 0 || x >= cycle) 0f else x

fun sub(x: Float, y: Float, cycle: Float)
        = normalize(x - y, cycle)

fun distance(x: Float, y: Float, cycle: Float)
        = min(sub(x, y, cycle), sub(y, x, cycle))

/**
 * Like distance, but with a sign. Minus if x is before refPoint "clockwise", plus otherwise.
 */
fun signedDistance(x: Float, refPoint: Float, cycle: Float): Float {
    val nx = normalize(x, cycle)
    val nRefPoint = normalize(refPoint, cycle)
    val distance = distance(nx, nRefPoint, cycle)
    return if (nx < nRefPoint) {
        if (nRefPoint - nx < cycle / 2f) {
            -distance
        } else {
            distance
        }
    } else {
        if (nx - nRefPoint < cycle / 2f) {
            distance
        } else {
            -distance
        }
    }
}

/**
 * Weighted center between two points on a circular interval (like a clock face).
 */
fun center(x: Float, weightX: Float, y: Float, weightY: Float, cycle: Float): Float {
    if (weightX < 0) {
        throw IllegalArgumentException("weightX must be non-negative: $weightX")
    }
    if (weightY < 0) {
        throw IllegalArgumentException("weightY must be non-negative: $weightY")
    }
    val nx = normalize(x, cycle)
    val ny = normalize(y, cycle)
    val distance = distance(nx, ny, cycle)
    return if (nx < ny) {
        if (ny - nx < cycle / 2f) {
            normalize(nx + distance * weightY / (weightX + weightY), cycle)
        } else {
            normalize(nx - distance * weightY / (weightX + weightY), cycle)
        }
    } else {
        if (nx - ny < cycle / 2f) {
            normalize(ny + distance * weightX / (weightX + weightY), cycle)
        } else {
            normalize(ny - distance * weightX / (weightX + weightY), cycle)
        }
    }
}

/**
 * Calculate "average" or "center of mass" of a set of points on
 * a circular interval (like a clock face).
 */
fun center(xs: FloatArray, cycle: Float): Float {
    if (xs.isEmpty()) {
        return Float.NaN
    }
    var center = xs[0]
    for(i in 1 until xs.size) {
        center = center(center, i.toFloat(), xs[i], 1f, cycle)
    }
    return center
}

fun median(xs: FloatArray, cycle: Float) = median(xs, center(xs, cycle), cycle)

/**
 * What is median on a circular space? The re is no beginning and end,
 * let let alone a middle element. How to define it meaningfully...
 *
 * I want the median somewhere close to the center of mass, where most points are located.
 * So I start from the center of the mass, then take the opposite point on the circle -
 * where are typically no points at all, sort the points by distance from the opposite point,
 * and take the middle one. Typically, there is a cluster of points in a small area
 * (e.g. wake-up hours), and this definition really gives a meaningful median.
 */
fun median(xs: FloatArray, center: Float, cycle: Float): Float {

    val opposite = opposite(center, cycle)

    val sorted = xs.map { Pair(it, clockwiseDistance(opposite, it, cycle)) }
            .sortedBy { it.second }
            .map { it.first }

    val size = sorted.size
    if (size == 0) {
        return Float.NaN
    }
    else if (MoreMath.odd(size)) {
        return sorted[size/2]
    }
    else {
        return (sorted[ size/2 - 1 ] + sorted[ size/2 ]) / 2f
    }
}

/**
 * Standard deviation, using the center of mass above, rather than arithmetic mean.
 */
fun stdev(xs: FloatArray, cycle: Float) = stdev(xs, center(xs, cycle), cycle)

/**
 * Standard deviation from a given center, on a circular interval.
 */
fun stdev(xs: FloatArray, center: Float, cycle: Float): Float {
    if (xs.isEmpty()) {
        return Float.NaN
    }
    var sqDevSum = 0f
    for(x in xs) {
        val distance = distance(x, center, cycle)
        sqDevSum += distance*distance
    }
    return sqrt(sqDevSum/xs.size)
}

fun opposite(x: Float, cycle: Float) = normalize(x + cycle/2, cycle)

/**
 * @return how far is from x to y if we move clockwise
 */
fun clockwiseDistance(x: Float, y: Float, cycle: Float) =
        if (x <= y) {
            y - x
        } else {
            cycle - (x - y)
        }

/**
 * @param start inclusive
 * @param start inclusive
 * @return if we move clockwise from start to end, do we go through x?
 */
fun isBetweenClockwise(x: Float, start: Float, end: Float, cycle: Float) =
        clockwiseDistance(start, x, cycle) <= clockwiseDistance(start, end, cycle)


/**
 * @param start inclusive
 * @param start inclusive
 * @return the subset of xs that lay between start and end
 */
fun filterBetweenClockwise(xs: FloatArray, start: Float, end: Float, cycle: Float) =
        xs.filter { isBetweenClockwise(it, start, end, cycle)}.toFloatArray()

/**
 * Standard deviation from the given center,
 * but only taking the points that lie between the center and its opposite clockwise.
 */
fun halfStdevClockwise(xs: FloatArray, center: Float, cycle: Float): Float {
    val points = filterBetweenClockwise(xs, center, opposite(center, cycle), cycle)
    return stdev(points, center, cycle)
}

/**
 * Standard deviation from the given center,
 * but only taking the points that lie between the center and its opposite anticlockwise.
 */
fun halfStdevAnticlockwise(xs: FloatArray, center: Float, cycle: Float): Float {
    val points = filterBetweenClockwise(xs, opposite(center, cycle), center, cycle)
    return stdev(points, center, cycle)
}
