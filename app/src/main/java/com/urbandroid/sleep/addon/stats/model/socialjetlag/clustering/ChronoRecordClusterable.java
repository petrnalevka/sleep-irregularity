package com.urbandroid.sleep.addon.stats.model.socialjetlag.clustering;

import com.urbandroid.sleep.addon.stats.model.socialjetlag.ChronoRecord;

import org.apache.commons.math3.ml.clustering.Clusterable;

public class ChronoRecordClusterable implements Clusterable {

    private final ChronoRecord record;
    private final double[] point;

    public ChronoRecordClusterable(ChronoRecord record) {
        this.record = record;
        this.point = new double[]{record.getToHour(), record.getLength()};
    }

    @Override
    public double[] getPoint() {
        return point;
    }

    public ChronoRecord getRecord() {
        return record;
    }
}
