package com.urbandroid.sleep.addon.stats.model.socialjetlag

import com.urbandroid.sleep.addon.stats.model.StatRecord
import java.util.*

class ChronoRecord @JvmOverloads constructor(
        val from: Date, val to: Date,
        //Fractional hours (e.g. 22:30 is 22.5) in the user's local time.
        val fromHour: Float, val toHour: Float,
        //Net sleep time in hours (to - from - awake_pauses).
        val length: Float, val timeZone: TimeZone = TimeZone.getDefault())

{
    //Fractional hours (e.g. 22:30 is 22.5) in the user's local time.
    val fromHourUTC = getHourUTC(from)
    val toHourUTC = getHourUTC(to)

    //Midpoint between from and to
    val midSleep = getMidSleep(fromHour, toHour)
    val midSleepUTC = getMidSleep(fromHourUTC, toHourUTC)

    init {
        if (from.time > to.time) {
            throw IllegalArgumentException("$from > $to")
        }
        //Max sleep length is 20 hours. It needs to be less than one day
        //in order to midpoint calculation to work, and the four hours
        //are a buffer for DST time change, or timezone change, etc.
        if (to.time - from.time > 1000 * 60 * 60 * 20) {
            throw IllegalArgumentException("$to - $from > one day")
        }
        if (fromHour < 0 || fromHour >= 24)  {
            throw IllegalArgumentException("Invalid fromHour: $fromHour")
        }
        if (toHour < 0 || toHour >= 24)  {
            throw IllegalArgumentException("Invalid toHour: $toHour")
        }
    }

    private fun getHourUTC(d: Date): Float {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = d }
        val hour = calendar.get(Calendar.HOUR_OF_DAY).toFloat()
        val minute = calendar.get(Calendar.MINUTE).toFloat()
        val second = calendar.get(Calendar.SECOND).toFloat()
        return hour + minute/60f + second/3600f
    }

    private fun getMidSleep(fromHour: Float, toHour: Float): Float = if (fromHour <= toHour) {
        (fromHour + toHour) / 2f
    } else {
        val midPoint = (fromHour + toHour) / 2f + 12f
        if (midPoint < 24f) midPoint else midPoint - 24f
    }

    fun getEndDayOfWeek() = Calendar.getInstance(timeZone).run {
        time = to
        get(Calendar.DAY_OF_WEEK)
    }

    //Indeed, we are dealing with records from a single user, so "to" fully identifies the record.
    override fun equals(other: Any?) = other is ChronoRecord && other.to == to

    override fun hashCode() = to.hashCode()

    override fun toString(): String {
        return "ChronoRecord(from=$from, to=$to, length=$length, midSleep=$midSleep)"
    }
}

fun StatRecord.toChronoRecord(): ChronoRecord? {
    try {
        return ChronoRecord(
                from = fromDate,
                to = toDate,
                fromHour = fromHour.toFloat(),
                toHour = toHour.toFloat(),
                length = trackLengthInHours,
                timeZone = timeZone ?: TimeZone.getDefault()
        )
    } catch (e: RuntimeException) {
        return null
    }
}
