package com.urbandroid.sleep.domain;

public interface IEvent {
    long getTimestamp();
    EventLabel getLabel();
    float getValue();
}
