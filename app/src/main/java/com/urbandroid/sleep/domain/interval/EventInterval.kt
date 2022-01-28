package com.urbandroid.sleep.domain.interval

import com.urbandroid.sleep.domain.Event

data class EventInterval(val from: Event, val to: Event) {

    fun toInterval(): Interval {
        return Interval(from.timestamp, to.timestamp)
    }

    override fun toString(): String {
        return from.toString() + " - " + toInterval().toString() + " - " + to.toString()
    }
}
