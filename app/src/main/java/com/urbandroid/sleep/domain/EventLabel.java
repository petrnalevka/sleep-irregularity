package com.urbandroid.sleep.domain;

import java.io.Serializable;

/**
 * Labels for special points in tracking, for which we track time of their occurrence.
 *
 * WARNING: Do NOT change order or names of the elements in this enum! They are persisted and changing
 * the order/naming could break already stored data!
 */
public enum EventLabel implements Serializable {

    ALARM_EARLIEST, // Earliest possible alarm time.
    ALARM_LATEST, // Latest possible alarm time.
    ALARM_SNOOZE, // User snoozed an alarm.
    ALARM_SNOOZE_AFTER_KILL, // Alarm was automatically snoozed due to auto-kill
    ALARM_DISMISS, // User successfully dismissed alarm.
    TRACKING_PAUSED, // User paused tracking
    TRACKING_RESUMED, // User resumed tracking
    TRACKING_STOPPED_BY_USER, // User stopped tracking.
    ALARM_STARTED, // Alarm started ringing.
    SNORING, // Snoring detected.
    LOW_BATTERY, // Tagged when we get to a low battery mode.
    DEEP_START, // Deep sleep
    DEEP_END, // ...
    LIGHT_START, // Light sleep
    LIGHT_END, // ...
    REM_START, // REM start
    REM_END, // ...
    UNKNOWN, // Used for labels not known in current version (but possibly imported from some other version)
    BROKEN_START, // Broken value intervals
    BROKEN_END, //
    WALKING_START, // Walking start (currently from Google Fit)
    WALKING_END,
    AWAKE_START, // Awake detection
    AWAKE_END,
    HR(true),
    HR_HIGH_START,
    HR_HIGH_END,
    LUCID_CUE,
    SPO2(true),
    APNEA,
    RR(true), // Respiratory rate
    TALK, //talk detected
    BABY, //baby crying detected
    SICK, //cough or sneeze detected
    LAUGH,//laughter detected

    //A vector of high activity flags per 10s intervals.
    //It is used together with aggregated activity to compute hypnogram.
    //One event encodes 32 bits, so there will be about 11 events per hour of sleep.
    //We did not want to add new fields to the SleepRecord due to integration hell,
    //so we used events to store the data.
    //See EventRawStorage.kt
    DHA(true),

    //Not used yet.
    FLAGS,

    // trial event label
    T,

    LUX(true),
    ANTISNORE,

    // Users deleted awakes and we shall not compute them again
    NO_AWAKE,

    // The device used for tracking the record.
    // There is one event per SleepRecord.
    // The device is encoded as: Build.DEVICE.hashCode(), the four bytes of int stored as float.
    // To decode the device from an event, we need a reverse map (hashcode -> device name).
    // Such map can be obtained, for example, by going through all device-specific settings
    // (e.g. KEY_SONAR_STREAM, KEY_NOISE_DIR_URI), extracting the available device-specific
    // suffixes, and computing their hashes.
    DEVICE(true),

    // SDNN (a HRV statistic) for 5 minute intervals
    SDNN(true),

    // SDANN (a HRV statistic) for the entire night
    SDANN(true),

    // Respiratory disturbance index
    RDI(true);

    private final boolean hasValue;

    EventLabel(boolean hasValue) {
        this.hasValue = hasValue;
    }

    EventLabel() {
        this.hasValue = false;
    }

    public boolean hasValue() {
        return hasValue;
    }
}
