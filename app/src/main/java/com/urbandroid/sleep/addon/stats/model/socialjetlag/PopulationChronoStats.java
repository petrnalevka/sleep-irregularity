package com.urbandroid.sleep.addon.stats.model.socialjetlag;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Sleep statistics mined from the sleep cloud DB.
 */
public class PopulationChronoStats {

    private static final TreeMap<Float, Float> MID_SLEEP_ON_FREE_DAYS_DISTRIBUTION = new TreeMap<>();

    private static final TreeMap<Float, Float> MID_SLEEP_ON_FREE_DAYS_HIST = new TreeMap<>();

    static {

        MID_SLEEP_ON_FREE_DAYS_DISTRIBUTION.put(Float.NEGATIVE_INFINITY, 0.0f);
        MID_SLEEP_ON_FREE_DAYS_DISTRIBUTION.put(2.76f, 0.1f);
        MID_SLEEP_ON_FREE_DAYS_DISTRIBUTION.put(3.43f, 0.2f);
        MID_SLEEP_ON_FREE_DAYS_DISTRIBUTION.put(3.92f, 0.3f);
        MID_SLEEP_ON_FREE_DAYS_DISTRIBUTION.put(4.37f, 0.4f);
        MID_SLEEP_ON_FREE_DAYS_DISTRIBUTION.put(4.79f, 0.5f);
        MID_SLEEP_ON_FREE_DAYS_DISTRIBUTION.put(5.26f, 0.6f);
        MID_SLEEP_ON_FREE_DAYS_DISTRIBUTION.put(5.80f, 0.7f);
        MID_SLEEP_ON_FREE_DAYS_DISTRIBUTION.put(6.50f, 0.8f);
        MID_SLEEP_ON_FREE_DAYS_DISTRIBUTION.put(7.65f, 0.9f);

        MID_SLEEP_ON_FREE_DAYS_HIST.put(-0.5f, 1f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(0f, 1f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(0.25f, 1f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(0.5f, 3f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(1f, 6f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(1.25f, 5f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(1.5f, 7f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(1.75f, 8f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(2f, 18f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(2.25f, 38f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(2.5f, 58f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(2.75f, 78f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(3f, 127f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(3.25f, 187f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(3.5f, 205f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(3.75f, 286f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(4f, 312f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(4.25f, 360f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(4.5f, 351f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(4.75f, 360f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(5f, 357f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(5.25f, 323f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(5.5f, 299f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(5.75f, 261f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(6f, 230f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(6.25f, 183f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(6.5f, 166f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(6.75f, 142f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(7f, 99f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(7.25f, 76f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(7.5f, 57f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(7.75f, 55f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(8f, 25f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(8.25f, 29f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(8.5f, 25f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(8.75f, 9f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(9f, 14f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(9.25f, 4f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(9.5f, 6f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(9.75f, 3f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(10f, 2f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(10.5f, 2f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(10.75f, 1f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(11f, 2f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(11.75f, 1f);
        MID_SLEEP_ON_FREE_DAYS_HIST.put(14f, 1f);
    }

    public static SortedMap<Float,Float> getMidSleepOnFreeDaysHistogram() {
        return Collections.unmodifiableSortedMap(MID_SLEEP_ON_FREE_DAYS_HIST);
    }

    public static float getChronotypeQuantile(float midSleepOnFreeDays) {
        return MID_SLEEP_ON_FREE_DAYS_DISTRIBUTION.floorEntry(midSleepOnFreeDays).getValue();
    }

    public static int getChronotypeRank(float chronotypeQuantile) {
        if (chronotypeQuantile <= 0) {
            return 5;
        }
        else if (chronotypeQuantile >= 1) {
            return 5;
        }
        else if (chronotypeQuantile < 0.5f) {
            return Math.round(10 * (0.5f - chronotypeQuantile));
        }
        else {
            return 1 + Math.round(10 * (chronotypeQuantile - 0.5f));
        }
    }
}
