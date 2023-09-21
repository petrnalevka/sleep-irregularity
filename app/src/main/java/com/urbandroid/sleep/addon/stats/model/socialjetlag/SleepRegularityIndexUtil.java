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
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

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

public class SleepRegularityIndexUtil {

    public static boolean DO_SRI = true;

    /**
     * Splits a date that over flows to the next day into two intervals
     * @param startTime StartTime of an interval that overflows
     * @param endTime EndTime of an interval that overflows
     * @return Interval[] with spliced times
     */
    protected Interval[] splitOverFlowingDate(DateTime startTime, DateTime endTime){
        DateTime firstIntervalEndTime = new DateTime(
                startTime.year().get(), startTime.monthOfYear().get(), startTime.dayOfMonth().get(), 23, 59, 59, 999
        ).withZoneRetainFields(DateTimeZone.UTC);

        DateTime secondIntervalStartTime = new DateTime(
                endTime.year().get(), endTime.monthOfYear().get(), endTime.dayOfMonth().get(), 0, 0, 0, 0
        ).withZoneRetainFields(DateTimeZone.UTC);

        return new Interval[] {new Interval(startTime, firstIntervalEndTime), new Interval(secondIntervalStartTime, endTime)};
    }

    /**
     * Checks if "nextDate" is the actual next date of prevDate
     * @param prevDate
     * @param nextDate
     * @return
     */
    protected boolean isNextDate(DateTime prevDate, DateTime nextDate){
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
    protected List<Interval> convertChronoRecordsToTimeIntervals(ChronoRecords records ){
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
    private final static String DATE_PATTERN = "yyyy-MM-dd";

    /**
     * (1) Store in values awakeTimes as a 1440 (24 hrs x 60 min) sized bitset
     *     such that each bitset value is non-empty if in "awake-state"
     *     0 if in "sleep-state"
     * (2) Key is the date in String format yyyy-MM-dd
     * (3) Used for easy cross-day comparison of intersecting time intervals
     * @param awakeTimes List of intervals that indicate user "awake" state
     * @return sleepStateByDateMap
     */
    protected TreeMap<String, BitSet> createSleepStateByDateMap(List<Interval> awakeTimes){
        TreeMap<String, BitSet> sleepStateByDateMap = new TreeMap<>();
        for(int j = 0; j < awakeTimes.size(); ++j){
            Interval awakeInterval = awakeTimes.get(j);

            BitSet sleepState;

            DateTime awakeIntervalStartTime = awakeInterval.getStart();
            String awakeIntervalFrom = awakeIntervalStartTime.toString(DATE_PATTERN);

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
    protected float calculateSRI(BitSet prevDay, BitSet nextDay){
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
    protected HashMap<Integer, List<Float>> extractSRIScores(TreeMap<String, BitSet> sleepStateByDateMap){
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

                DateTimeFormatter formatter = DateTimeFormat.forPattern(DATE_PATTERN);
                DateTime prevDate = DateTime.parse(prevDateKey, formatter);
                DateTime nextDate = DateTime.parse(prevDateKey, formatter);

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
                    Logger.logSevere("Skipping SRI score calculation for following pair of dates. Date ranges must be contiguous to compute the SRI. User provided date ranges: " + prevDateKey + "- " + nextDateKey);
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
    protected float calculateAverageSRI(TreeMap<String, BitSet> sleepStateByDateMap){
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
    protected float calculateAverageMSRI(TreeMap<String, BitSet> sleepStateByDateMap){
        Set keys = sleepStateByDateMap.keySet();

        if (keys.size() < 3) { // there should be at least three dates to calculate the mSRI
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

    public float getSleepIrregularityModified(ChronoRecords records){
        List<Interval> awakeTimes = convertChronoRecordsToTimeIntervals(records);
        TreeMap<String, BitSet> sleepStateByDateMap = createSleepStateByDateMap(awakeTimes);
        return calculateAverageMSRI(sleepStateByDateMap);
    }

    public float getSleepIrregularity(ChronoRecords records) {


        List<Interval> awakeTimes = convertChronoRecordsToTimeIntervals(records);
        TreeMap<String, BitSet> sleepStateByDateMap = createSleepStateByDateMap(awakeTimes);

        float result = calculateAverageSRI(sleepStateByDateMap);
//        Logger.logSevere("REGULARITY SRI records " + records.getFrom() + " - " + records.getTo() + " = " + result);
        return result;
    }
}
