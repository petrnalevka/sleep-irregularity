package com.urbandroid.sleep.domain.interval;

import java.util.List;

public class ValueInterval extends Interval {

    private float value;

    public ValueInterval(long from, long to, float value) {
        super(from, to);
        this.value = value;
    }

    public float getValue() {
        return value;
    }

    public static float weightedAvg(List<ValueInterval> intervals) {

        float sum = 0;
        float count = 0;

        for (ValueInterval vi : intervals) {
            sum += vi.getValue() * vi.getLength();
            count += vi.getLength();
        }

        if (count == 0) {
            return 0;
        }

        return sum / count;
    }

}

