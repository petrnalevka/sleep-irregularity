package com.urbandroid.sleep.domain.interval;

import androidx.annotation.NonNull;

import com.urbandroid.sleep.domain.EventLabel;

public class EventPair {

    private final EventLabel start;
    private final EventLabel end;

    public EventPair(EventLabel start, EventLabel end) {
        this.start = start;
        this.end = end;
    }

    public EventLabel getStart() {
        return start;
    }

    public EventLabel getEnd() {
        return end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventPair)) return false;

        EventPair eventPair = (EventPair) o;

        if (start != eventPair.start) return false;
        return end == eventPair.end;

    }

    @Override
    public int hashCode() {
        int result = start != null ? start.hashCode() : 0;
        result = 31 * result + (end != null ? end.hashCode() : 0);
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return "[" + start + "," + end + "]";
    }
}
