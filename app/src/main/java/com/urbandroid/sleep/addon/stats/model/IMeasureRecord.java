package com.urbandroid.sleep.addon.stats.model;

public interface IMeasureRecord {

    //Total length.
    float getTrackLengthInHours();

    //Used for efficiency computation.
    float getLengthInHours();

    float getIrregularityInMinutes();

    //Deep sleep ratio - 0..1
    float getQuality();

    //Snore length in seconds.
    int getSnore();

    //0..5
    float getRating();

    float getHrvBefore();
    float getHrvAfter();
    //Generally, hrvGain = hrvAfter - hrvBefore
    //However, there are special cases.
    //Either of hrvBefore/After may be unavailable due to sensor outage.
    //In this case, hrvGain is undefined for a single record.
    //Average gain (e.g. for a period/tag) should be calculated as avg(hrvGain), rather than avg(hrvAfter) - avg(hrvBefore).
    float getHrvGain();
    //SDANN for the whole night
    float getHrv();

    int getRdi();

    int getSmart();
    float getNoiseLevel();
    double getCycles();
    int getSnooze();
}
