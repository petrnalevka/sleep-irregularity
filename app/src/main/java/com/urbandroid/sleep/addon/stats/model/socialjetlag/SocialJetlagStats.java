package com.urbandroid.sleep.addon.stats.model.socialjetlag;

import static com.urbandroid.sleep.addon.stats.model.socialjetlag.ChronoRecordKt.toChronoRecord;

import android.content.Context;

import com.urbandroid.common.logging.Logger;
import com.urbandroid.sleep.addon.stats.model.StatRecord;
import com.urbandroid.sleep.addon.stats.model.socialjetlag.clustering.ClusteredChronoRecords;
import com.urbandroid.sleep.addon.stats.model.socialjetlag.clustering.SleepLabel;
import com.urbandroid.util.ScienceUtil;

import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class SocialJetlagStats {

    public static SocialJetlagStats create(
            Collection<? extends StatRecord> statRecords,
            Context context)
    {
        //Skip too short or too long sleeps.
        List<StatRecord> goodStatRecords = SleepStatsUtilKt.filterByGrossLength(statRecords, 2, 16);

        ArrayList<ChronoRecord> chronoRecords = new ArrayList<>();
        for (StatRecord statRecord : goodStatRecords) {
            ChronoRecord chronoRecord = toChronoRecord(statRecord);
            if (chronoRecord != null) {
                chronoRecords.add(chronoRecord);
            }
        }

        boolean useUTCforIrregularity = false;

        return new SocialJetlagStats(new ChronoRecords(chronoRecords), useUTCforIrregularity);
    }

    private final ChronoRecords records;

    private final boolean useUTCforIrregularity;

    private final ValueCache valueCache;

    SocialJetlagStats(ChronoRecords records, boolean useUTCforIrregularity) {
        this.records = records;
        this.useUTCforIrregularity = useUTCforIrregularity;
        this.valueCache = new ValueCache();
    }

    public int size() {
        return records.size();
    }

    public ChronoRecords getRecords() {
        return records;
    }

    public SocialJetlagStats narrow(Date from, Date to) {
        return new SocialJetlagStats(records.narrow(from, to), useUTCforIrregularity);
    }

    public float getSleepIrregularity() {
        return valueCache.computeIfAbsent("SleepIrregularity", new ValueCache.Supplier() {
            @Override
            public Object get() {
                if (records.size() < 5) {
                    return -1f;
                } else {
                    float[] midSleeps = useUTCforIrregularity ? records.getMidSleepUTC() : records.getMidSleeps();
                    float midSleepStd = CyclicFloatKt.stdev(midSleeps, 24f);
                    float sleepLenStd = ScienceUtil.stddev(records.getLengths());
                    return (midSleepStd + sleepLenStd) / 2;
                }
            }
        });
    }

    public float getRecordIrregularity(final StatRecord statRecord) {

        if (records.size() < 5) {
            return -1f;
        }

        ChronoRecord record = toChronoRecord(statRecord);
        if (record == null) {
            return -1f;
        } else {
            float midSleepAverage = getAverageMidSleepHour();
            float recordMidSleep = useUTCforIrregularity ? record.getMidSleepUTC() : record.getMidSleep();
            float midSleepDiff = CyclicFloatKt.distance(midSleepAverage, recordMidSleep, 24f);

            float sleepLenAverage = getAverageLengthHours();
            float recordSleepLen = record.getLength();
            float sleepLenDiff = Math.abs(recordSleepLen - sleepLenAverage);

            return (midSleepDiff + sleepLenDiff) / 2;
        }
    }

    public float getAverageMidSleepHour() {
        if (records.size() < 5) {
            return -1f;
        }
        float[] midSleeps = useUTCforIrregularity ? records.getMidSleepUTC() : records.getMidSleeps();
        return CyclicFloatKt.center(midSleeps, 24f);
    }

    public float getAverageLengthHours() {
        if (records.size() < 5) {
            return -1f;
        }
        float[] sleepLens = records.getLengths();
        return ScienceUtil.avg(sleepLens);
    }

    private void splitFreeAndBusyDays() {
        valueCache.computeIfAbsent("FreeAndBusyDays", new ValueCache.Supplier() {
            @Override
            public Object get() {
                ClusteredChronoRecords clusteredRecords = getClusteredRecords();
                boolean goodClustering = decideIfClusteringIsGood(clusteredRecords);
                valueCache.put("GoodClustering", goodClustering);
                if (goodClustering) {
                    valueCache.put("FreeDays", clusteredRecords.getLabeledRecords(SleepLabel.FREE_DAY));
                    valueCache.put("BusyDays", clusteredRecords.getLabeledRecords(SleepLabel.BUSY_DAY));
                    valueCache.put("UnclassifiedDays", clusteredRecords.getLabeledRecords(SleepLabel.OUTLIER));
                } else {
                    Pair<ChronoRecords, ChronoRecords> pair = records.split(weekendFilter());
                    valueCache.put("FreeDays", pair.getFirst());
                    valueCache.put("BusyDays", pair.getSecond());
                    valueCache.put("UnclassifiedDays", new ChronoRecords());
                }
                return null;
            }
        });
    }

    private boolean decideIfClusteringIsGood(ClusteredChronoRecords clusteredRecords) {

        if (clusteredRecords.getClusteringStrength() < 2.75) {
            //The data appear to be too homogenous.
            //Two clusters did not reduce the mean distances from the center enough.
            return false;
        }

        ChronoRecords freeDays = clusteredRecords.getLabeledRecords(SleepLabel.FREE_DAY);
        ChronoRecords busyDays = clusteredRecords.getLabeledRecords(SleepLabel.BUSY_DAY);

        if (freeDays.size() < 5 || ((double)freeDays.size())/size() < 0.1) {
            //The free days cluster is too small.
            //It is probably just a random artifact.
            return false;
        }

        float avgLenFree = ScienceUtil.avg(freeDays.getLengths());
        float avgLenBusy = ScienceUtil.avg(busyDays.getLengths());
        float stdLenBusy = ScienceUtil.stddev(busyDays.getLengths());
        if ( (avgLenFree - avgLenBusy) / stdLenBusy < -0.5f ) {
            //Sleep length on free days is smaller than sleep length on working days.
            //It seems unlikely and the clustering is probably wrong.
            return false;
        }

        return true;
    }

    private ClusteredChronoRecords getClusteredRecords() {
        return valueCache.computeIfAbsent("ClusteredRecords", new ValueCache.Supplier() {
            @Override
            public Object get() {
                return new ClusteredChronoRecords(records);
            }
        });
    }

    public boolean hasGoodClustering() {
        splitFreeAndBusyDays();
        return valueCache.get("GoodClustering");
    }

    public ChronoRecords getFreeDays() {
        splitFreeAndBusyDays();
        return valueCache.get("FreeDays");
    }

    public ChronoRecords getBusyDays() {
        splitFreeAndBusyDays();
        return valueCache.get("BusyDays");
    }

    public ChronoRecords getUnclassifiedDays() {
        splitFreeAndBusyDays();
        return valueCache.get("UnclassifiedDays");
    }

    public float getMidSleepFreeDays() {
        return valueCache.computeIfAbsent("MidSleepFreeDays", new ValueCache.Supplier() {
            @Override
            public Object get() {
                ChronoRecords records = getFreeDays();
                if (records.size() < 5) {
//                    Logger.logDebug("SocialJetlagStats.midSleepFreeDays: not enough data");
                    //-1 is a valid value for mid sleep, so I return NaN
                    return Float.NaN;
                } else {
                    return ScienceUtil.avg(records.getMidSleeps());
                }
            }
        });
    }

    public float getMidSleepBusyDays() {
        return valueCache.computeIfAbsent("MidSleepBusyDays", new ValueCache.Supplier() {
            @Override
            public Object get() {
                ChronoRecords records = getBusyDays();
                if (records.size() < 5) {
//                    Logger.logDebug("SocialJetlagStats.midSleepBusyDays: not enough data");
                    //-1 is a valid value for mid sleep, so I return NaN
                    return Float.NaN;
                } else {
                    return ScienceUtil.avg(records.getMidSleeps());
                }
            }
        });
    }

    public float getSocialJetLag() {
        return valueCache.computeIfAbsent("SocialJetLag", new ValueCache.Supplier() {
            @Override
            public Object get() {
                float midSleepBusyDays = getMidSleepBusyDays();
                float midSleepFreeDays = getMidSleepFreeDays();
                if (Float.isNaN(midSleepBusyDays) || Float.isNaN(midSleepFreeDays)) {
                    //-1 is a valid value for social jetlag, so I return NaN
                    return Float.NaN;
                } else {
                    return Math.abs(midSleepBusyDays - midSleepFreeDays);
                }
            }
        });
    }

    public float getChronotype() {
        return valueCache.computeIfAbsent("Chronotype", new ValueCache.Supplier() {
            @Override
            public Object get() {
                float midSleepFreeDays = getMidSleepFreeDays();
                if (Float.isNaN(midSleepFreeDays)) {
                    return -1f;
                } else {
                    float chronotype =  PopulationChronoStats.getChronotypeQuantile(midSleepFreeDays);
                    Logger.logDebug("SocialJetlagStats.chronotype: "+midSleepFreeDays+" "+chronotype);
                    return chronotype;
                }
            }
        });
    }

    public List<Pair<Date,Float>> getChronotypeHistory(int fragmentLenghtMonths, int stepMonths) {
        return getAggregatedHistoryMonths(
                fragmentLenghtMonths, stepMonths,
                new ChronoRecords.RecordsToFloat() {
                    @Override
                    public float apply(ChronoRecords chunk) {
                        return new SocialJetlagStats(chunk, useUTCforIrregularity).getChronotype();
                    }
                });
    }

    public List<Pair<Date,Float>> getSleepIrregularityHistory(int fragmentLenghtDays, int stepDays) {
        return getAggregatedHistoryDays(
                fragmentLenghtDays, stepDays,
                new ChronoRecords.RecordsToFloat() {
                    @Override
                    public float apply(ChronoRecords chunk) {
                        return new SocialJetlagStats(chunk, useUTCforIrregularity).getSleepIrregularity();
                    }
                });
    }

    private List<Pair<Date,Float>> getAggregatedHistoryMonths(
            int fragmentLenghtMonths, int stepMonths,
            ChronoRecords.RecordsToFloat valueSupplier)
    {
        List<Pair<Date,Float>> result = new ArrayList<>();
        List<ChronoRecords> chunks = records.splitByMonth(fragmentLenghtMonths, stepMonths);
        for(ChronoRecords chunk : chunks) {
            Date endDate = chunk.getTo();
            float value = valueSupplier.apply(chunk);
            result.add(Pair.create(endDate, value));
        }
        return result;
    }

    private List<Pair<Date,Float>> getAggregatedHistoryDays(
            int fragmentLenghtDays, int stepDays,
            ChronoRecords.RecordsToFloat valueSupplier)
    {
        List<Pair<Date,Float>> result = new ArrayList<>();
        List<ChronoRecords> chunks = records.splitByDays(fragmentLenghtDays, stepDays);
        for(ChronoRecords chunk : chunks) {
            Date endDate = chunk.getTo();
            float value = valueSupplier.apply(chunk);
            result.add(Pair.create(endDate, value));
        }
        return result;
    }

    private ChronoRecords.RecordToBool weekendFilter() {
        return new ChronoRecords.RecordToBool() {
            @Override
            public boolean apply(ChronoRecord record) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(record.getTo());
                return  calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY ||
                        calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY;
            }
        };
    }
}
