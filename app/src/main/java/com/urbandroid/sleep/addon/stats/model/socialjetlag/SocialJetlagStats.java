package com.urbandroid.sleep.addon.stats.model.socialjetlag;

import static com.urbandroid.sleep.addon.stats.model.socialjetlag.ChronoRecordKt.toChronoRecord;

import android.content.Context;

import com.urbandroid.common.logging.Logger;
import com.urbandroid.sleep.addon.stats.model.StatRecord;
import com.urbandroid.sleep.addon.stats.model.socialjetlag.clustering.ClusteredChronoRecords;
import com.urbandroid.sleep.addon.stats.model.socialjetlag.clustering.SleepLabel;
import com.urbandroid.util.ScienceUtil;

import org.apache.commons.math3.util.Pair;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;

import java.sql.Array;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

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



    /**
     * Splits a date that over flows to the next day into two intervals
     * @param startTime StartTime of an interval that overflows
     * @param endTime EndTime of an interval that overflows
     * @return Interval[] with spliced times
     */
    private Interval[] splitOverFlowingDate(DateTime startTime, DateTime endTime){
        DateTime firstIntervalEndTime = new DateTime(
                startTime.year().get(), startTime.monthOfYear().get(), startTime.dayOfMonth().get(), 23, 59
        ).withZoneRetainFields(DateTimeZone.UTC);

        DateTime secondIntervalStartTime = new DateTime(
                endTime.year().get(), endTime.monthOfYear().get(), endTime.dayOfMonth().get(), 00, 00
        ).withZoneRetainFields(DateTimeZone.UTC);

        return new Interval[] {new Interval(startTime, firstIntervalEndTime), new Interval(secondIntervalStartTime, endTime)};
    }

    /**
     * Checks if "nextDate" is the actual next date of prevDate
     * @param prevDate
     * @param nextDate
     * @return
     */
    private boolean isNextDate(DateTime prevDate, DateTime nextDate){
        DateTime actualNextDate = prevDate.plusDays(1);
        boolean sameYear = (actualNextDate.getYear() == nextDate.getYear());
        boolean sameMonth = (actualNextDate.getMonthOfYear() == nextDate.getMonthOfYear());
        boolean sameDate = (actualNextDate.getDayOfMonth() == nextDate.getDayOfMonth());

        return sameYear && sameMonth && sameDate;
    }

    /**
     * (1) Converts ChronoRecords to Joda Time Intervals - easier to manipulate than "Date"
     * (2) Splices Time Intervals Overflowing to Next Day
     *     (i.e. [Monday 8pm - Tuesday 2am] --> [Monday 8pm - Monday 11:59pm], [Tuesday 12 - 2am]
     * @param records ChronoRecords including awake times
     * @return nonOverFlowingTimeIntervals
     */
    public List<Interval> convertChronoRecordsToTimeIntervals(ChronoRecords records ){
        List<ChronoRecord> listOfRecords = records.getRecordsList();
        List<Interval> nonOverFlowingTimeIntervals = new ArrayList<Interval>();

        for(int i = 0; i< listOfRecords.size(); i++){
            ChronoRecord cr1 = listOfRecords.get(i);

            DateTime intervalStart = new DateTime(cr1.getFrom()).withZone(DateTimeZone.UTC);
            DateTime intervalEnd = new DateTime(cr1.getTo()).withZone(DateTimeZone.UTC);

            if(isNextDate(intervalStart, intervalEnd)){
                // Split interval to two, i1, i2
                Interval[] splitIntervals = splitOverFlowingDate(intervalStart, intervalEnd);
                nonOverFlowingTimeIntervals.add(splitIntervals[0]);
                nonOverFlowingTimeIntervals.add(splitIntervals[1]);
            }else{
                nonOverFlowingTimeIntervals.add(new Interval(intervalStart, intervalEnd));
            }
        }

        return nonOverFlowingTimeIntervals;
    }

    /**
     * (1) Store in values awakeTimes as a 1440 (24 hrs x 60 min) sized bitset
     *     such that each bitset value is non-empty if in "awake-state"
     *     0 if in "sleep-state"
     * (2) Key is the date in String format yyyy-MM-dd
     * (3) Used for easy cross-day comparison of intersecting time intervals
     * @param awakeTimes List of intervals that indicate user "awake" state
     * @return sleepStateByDateMap
     */
    public TreeMap<String, BitSet> createSleepStateByDateMap(List<Interval> awakeTimes){
        TreeMap<String, BitSet> sleepStateByDateMap = new TreeMap<>();
        for(int j = 0; j < awakeTimes.size(); ++j){
            Interval awakeInterval = awakeTimes.get(j);

            BitSet sleepState;

            DateTime awakeIntervalStartTime = awakeInterval.getStart();
            String awakeIntervalFrom = awakeIntervalStartTime.toString("yyyy-MM-dd");

            if(sleepStateByDateMap.containsKey(awakeIntervalFrom)){
                sleepState = sleepStateByDateMap.get(awakeIntervalFrom);
            } else{
                sleepState = new BitSet(1440); // 24 hrs * 60 minutes
            }

            int startIdx = awakeIntervalStartTime.getMinuteOfDay();
            int endIdx = awakeInterval.getEnd().getMinuteOfDay();
            sleepState.set(startIdx, endIdx);

            sleepStateByDateMap.put(awakeIntervalFrom, sleepState);
        }

        return sleepStateByDateMap;
    }

    /**
     * Calculates SRI score given 2 days
     * It is the (total number of minutes in which a user is in a consistent sleep state) / 1440 mins
     * Consistent state: prevDay[i] == nextDay[i]
     * Inconsistent state: prevDay[i] =/= nextDay[i]
     * @param prevDay
     * @param nextDay
     * @return
     */
    public float calculateSRI(BitSet prevDay, BitSet nextDay){
        // Make sure not to use isEmpty as that checks if there are no falses
        if(prevDay.size() < 1440 || nextDay.size() < 1440 ){
            return -1.0f;
        }
        BitSet minsInconsistentSleepState = (BitSet) prevDay.clone(); //store inconsistent sleep state mins
        minsInconsistentSleepState.xor(nextDay); // true means prevDay(i) and nextDay(i) had different values

        return (1.0f - (minsInconsistentSleepState.cardinality()/1440.f));
    }

    /**
     * Extracts SRI scores and groups them by continuous segments in a HashMap
     * @param sleepStateByDateMap
     * @return
     */
    public HashMap<Integer, List<Float>> extractSRIScores(TreeMap<String, BitSet> sleepStateByDateMap){
        HashMap<Integer, List<Float>> groupedSRIScores = new HashMap<Integer, List<Float>>();

        Set keys = sleepStateByDateMap.keySet();
        int groupNum = 0;

        Iterator nextDayIt = keys.iterator();
        // iterate through pairs of days to calculate the SRI
        if (nextDayIt.hasNext()) {
            nextDayIt.next();
            for (Iterator prevDayIt = keys.iterator(); prevDayIt.hasNext() && nextDayIt.hasNext();) {

                String prevDateKey = (String) prevDayIt.next();
                String nextDateKey =  (String) nextDayIt.next();
                DateTime prevDate = new DateTime(prevDateKey);
                DateTime nextDate = new DateTime(nextDateKey);

                if(isNextDate(prevDate, nextDate)){ // if there are a continuous pair of days, calculate the SRI

                    BitSet prevDaySleepStates = sleepStateByDateMap.get(prevDateKey);
                    BitSet nextDaySleepStates = sleepStateByDateMap.get(nextDateKey);

                    // extract the date and month and if it is one
                    float sriScore = calculateSRI(prevDaySleepStates, nextDaySleepStates);

                    // insert score into corresponding group
                    if(groupedSRIScores.containsKey(groupNum)){
                        groupedSRIScores.get(groupNum).add(sriScore);
                    } else {
                        LinkedList<Float> listOfSRIScores = new LinkedList<Float>();
                        listOfSRIScores.add(sriScore);
                        groupedSRIScores.put(groupNum, listOfSRIScores);
                    }
                } else { // Dates are no longer continuous, make a new group
                    groupNum++;
                    System.out.println("Skipping SRI score calculation for following pair of dates. Date ranges must be contiguous to compute the SRI. User provided date ranges: " + prevDateKey + "- " + nextDateKey);
                }
            }
        }
        return groupedSRIScores;
    }

    /**
     * Calculates average SRI across multiple, continuous dates
     * @param sleepStateByDateMap
     * @return
     */
    public float calculateAverageSRI(TreeMap<String, BitSet> sleepStateByDateMap){
        Set keys = sleepStateByDateMap.keySet();

        if (keys.size() < 2){ // there should be at least two dates to calculate the SRI
            return -1.0f;
        }

        HashMap<Integer, List<Float>> groupedSRISCores = extractSRIScores(sleepStateByDateMap);

        float cumulativeSRI = 0;
        int numSRIScores = 0;

        for ( List<Float> sriScores : groupedSRISCores.values()) {
            final Iterator<Float> scoreIterator = sriScores.listIterator();
            while(scoreIterator.hasNext()){
                cumulativeSRI += scoreIterator.next();
                numSRIScores++;
            }
        }
        return cumulativeSRI / numSRIScores;
    }

    /**
     * Calculates average mSRI across multiple, continuous dates
     * The mSRI is the cumulative difference between pairs of SRI scores
     * @param sleepStateByDateMap
     * @return
     */
    public float calculateAverageMSRI(TreeMap<String, BitSet> sleepStateByDateMap){
        Set keys = sleepStateByDateMap.keySet();

        if (keys.size() < 3){ // there should be at least three dates to calculate the mSRI
            return -1.0f;
        }

        HashMap<Integer, List<Float>> groupedSRISCores = extractSRIScores(sleepStateByDateMap);

        float cumulativeMSRI = 0;
        int numMSRIScores = 0;

        for ( List<Float> sriScores : groupedSRISCores.values()) {
            // if there are at least two sri scores, calculate the msri; else skip
            if(sriScores.size() >= 2){
                final Iterator<Float> scoreIterator = sriScores.listIterator();
                float currSRI = scoreIterator.next();
                while(scoreIterator.hasNext()){
                    float nextSRI = scoreIterator.next();
                    float mSRI = Math.abs(currSRI - nextSRI);
                    cumulativeMSRI += mSRI;
                    numMSRIScores++;
                    currSRI = nextSRI; // update pointer
                }
            }
        }

        return (numMSRIScores == 0 ) ? -1.0f : cumulativeMSRI / numMSRIScores;
    }

    public float getSleepIrregularityModified(){
        List<Interval> awakeTimes = convertChronoRecordsToTimeIntervals(records);
        TreeMap<String, BitSet> sleepStateByDateMap = createSleepStateByDateMap(awakeTimes);
        return calculateAverageMSRI(sleepStateByDateMap);
    }

    public float getSleepIrregularity() {
        return valueCache.computeIfAbsent("SleepIrregularity", new ValueCache.Supplier() {
            @Override
            public Object get() {
                if (records.size() < 5) {
                    return -1f;
                } else {

                    List<Interval> awakeTimes = convertChronoRecordsToTimeIntervals(records);
                    TreeMap<String, BitSet> sleepStateByDateMap = createSleepStateByDateMap(awakeTimes);

                    return calculateAverageSRI(sleepStateByDateMap);
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
