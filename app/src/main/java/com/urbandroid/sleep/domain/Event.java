package com.urbandroid.sleep.domain;

import androidx.annotation.Nullable;

import java.io.Serializable;

public class Event implements IEvent, Serializable {

    private long timestamp;

    private EventLabel label;

    // Optional field, that is set for unknown labels (to distinguish them)
    private String labelString;

    // Optional value.
    private float value;

    // Needed for GWT
    public Event() {
    }

    @Deprecated
    // Not passing in any value.
    public Event(long timestamp, EventLabel label) {
        this.timestamp = timestamp;
        this.label = label;
        this.labelString = null;
        this.value = 0.0f;
    }

    public Event(long timestamp, EventLabel label, float value) {
        this.timestamp = timestamp;
        this.label = label;
        this.labelString = null;
        this.value = value;
    }

    public Event(long timestamp, EventLabel label, String labelString, float value) {
        this.timestamp = timestamp;
        this.label = label;
        this.labelString = labelString;
        this.value = value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Nullable
    public EventLabel getLabel() {
        return label;
    }

    public String getLabelString() {
        return labelString;
    }

    // Version to get a string that represents current EventLabel. If string is unknown, we rather represent by explicit labelString.
    public String getLabelOrLabelString() {
        return label != EventLabel.UNKNOWN ? label.name() : labelString;
    }

    public float getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "E(" + label + ")@" + timestamp + "=" + value;
    }

    public String toStringDeterministic() {
        return new StringBuilder()
                .append(label)
                .append(":")
                .append(timestamp)
                .append(label.hasValue() ? ":" + value : "")
                .append(";")
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        return  (o instanceof Event) &&
                ((Event)o).timestamp == timestamp &&
                ((Event)o).label == label &&
                ((Event)o).value == value;
    }

    @Override
    public int hashCode() {
        return new Long(timestamp + label.ordinal()).hashCode();
    }

    public Event withLabel(EventLabel label) {
        return new Event(timestamp, label, value);
    }

    public Event withTimestamp(long timestamp) {
        return new Event(timestamp, label, value);
    }

    public Event withValue(float value) {
        return new Event(timestamp, label, value);
    }
}
