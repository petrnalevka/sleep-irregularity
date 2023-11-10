package com.urbandroid.sleep.addon.stats.model.socialjetlag;

import org.joda.time.Interval;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import com.urbandroid.sleep.addon.stats.model.socialjetlag.SleepRegularityIndexUtil;

public class SleepRegularityIndexTest {

    private ChronoRecords cr = new ChronoRecords(Arrays.asList(new ChronoRecord[]{
            //The first 3 records are in UTC timezone
            new ChronoRecord(
                    Date.from(Instant.parse("2018-11-11T01:30:10Z")),
                    Date.from(Instant.parse("2018-11-11T10:00:10Z")),
                    1.5f, 10f, 7f),
            new ChronoRecord(
                    Date.from(Instant.parse("2018-11-11T12:40:10Z")),
                    Date.from(Instant.parse("2018-11-11T13:00:10Z")),
                    1.5f, 10f, 7f),
            new ChronoRecord(
                    Date.from(Instant.parse("2018-11-12T01:00:10Z")),
                    Date.from(Instant.parse("2018-11-12T10:00:10Z")),
                    1.0f, 10f, 7f),
            new ChronoRecord(
                    Date.from(Instant.parse("2018-11-13T02:00:10Z")),
                    Date.from(Instant.parse("2018-11-13T10:00:10Z")),
                    2.0f, 10f, 6f),
            //The next 3 are in UTC+2
            new ChronoRecord(
                    Date.from(Instant.parse("2018-11-24T01:00:10Z")),
                    Date.from(Instant.parse("2018-11-24T10:00:10Z")),
                    3.0f, 12f, 7f),
            new ChronoRecord(
                    Date.from(Instant.parse("2018-11-25T02:00:10Z")),
                    Date.from(Instant.parse("2018-11-25T10:00:10Z")),
                    4.0f, 12f, 6f),
            new ChronoRecord(
                    Date.from(Instant.parse("2018-11-26T01:00:10Z")),
                    Date.from(Instant.parse("2018-11-26T09:00:10Z")),
                    3.0f, 11f, 6f),
    }));

    private SocialJetlagStats crStats = new SocialJetlagStats(cr, false);
    private SleepRegularityIndexUtil sriUtils = new SleepRegularityIndexUtil();

    @Test
    public void testSleepRegularityIndex() {
        float SRIScoreOne = 1.0f - (30.0f + 20.0f)/1440.f; // Score from 2018-11-11 and 2018-11-12
        float SRIScoreTwo = 1.0f - (60.0f)/1440.f; // Score from 2018-11-12 and 2018-11-13
        float SRIScoreThree = 1.0f - (60.0f)/1440.f; // Score from 2018-11-24 and 2018-11-25
        float SRIScoreFour = 1.0f - (120.0f)/1440.f; // Score from 2018-11-25 and 2018-11-26

        float expectedSRIScore = (SRIScoreOne + SRIScoreTwo + SRIScoreThree + SRIScoreFour)/4.0f;
        Assert.assertEquals(expectedSRIScore, crStats.getSleepIrregularity(), .0001);
    }

    @Test
    public void calculateSRIEdgeCases() {
        // one bitset empty
        BitSet day1Interval = new BitSet(1440);
        BitSet day2Interval = new BitSet(); //empty set
        day1Interval.set(0,720);
        Assert.assertEquals(-1.0f, sriUtils.calculateSRI(day1Interval, day2Interval), .0001);
        Assert.assertEquals(-1.0f, sriUtils.calculateSRI(day2Interval, day1Interval), .0001);
    }

    @Test public void extremeUsers(){
        // awake 24 hours for one day and asleep 24hrs for the next
        BitSet day1SleepStates = new BitSet(1440);
        BitSet day2SleepStates = new BitSet(1440);
        day1SleepStates.set(0,1440, true);
        day2SleepStates.set(0,1440, false);
        Assert.assertEquals(0, sriUtils.calculateSRI(day1SleepStates, day2SleepStates), .0001);

        // awake two days in a row
        BitSet day3SleepStates = new BitSet(1440);
        BitSet day4SleepStates = new BitSet(1440);
        day3SleepStates.set(0,1440, true);
        day4SleepStates.set(0,1440, true);
        Assert.assertEquals(1.0, sriUtils.calculateSRI(day3SleepStates, day4SleepStates), .0001);

        // asleep two days in a row
        BitSet day5SleepStates = new BitSet(1440);
        BitSet day6SleepStates = new BitSet(1440);
        day5SleepStates.set(0,1440, false);
        day6SleepStates.set(0,1440, false);
        Assert.assertEquals(1.0, sriUtils.calculateSRI(day5SleepStates, day6SleepStates), .0001);
    }

    @Test
    public void testCalculateSRI() {
        BitSet day1Interval = new BitSet(1440);
        BitSet day2Interval = new BitSet(1440);
        day1Interval.set(0,720);
        day2Interval.set(0,720);
        // testing for when the intevals are the same
        float sameInterval = sriUtils.calculateSRI(day1Interval, day2Interval);
        Assert.assertEquals(1.0, sameInterval, .0001);

        // general test, one interval enclosed inside the other
        BitSet day3Interval = new BitSet(1440);
        day3Interval.set(0,1440);
        Assert.assertEquals(0.5, sriUtils.calculateSRI(day1Interval, day3Interval), .0001);
        Assert.assertEquals(0.5, sriUtils.calculateSRI(day2Interval, day3Interval), .0001);

        // testing when intervals are completely different
        BitSet day4Interval = new BitSet(1440);
        day4Interval.set(720, 1440);
        Assert.assertEquals(0, sriUtils.calculateSRI(day1Interval, day4Interval), .0001);

        // testing when there is some overlap
        BitSet interval5 = new BitSet(1440);
        BitSet interval6 = new BitSet(1440);
        interval5.set(0, 720);
        interval6.set(360, 1440);
        Assert.assertEquals(0.25, sriUtils.calculateSRI(interval5, interval6), .0001);

        // single overlap
        BitSet interval7 = new BitSet(1440);
        BitSet interval8 = new BitSet(1440);
        interval7.set(0, 2);
        interval8.set(1, 2);
        Assert.assertEquals(1 - 1.0/1440.0, sriUtils.calculateSRI(interval7, interval8), .000001);

        // pretty regular
        BitSet interval9 = new BitSet(1440);
        BitSet interval10 = new BitSet(1440);
        interval9.set(0, 720);
        interval10.set(100, 820);
        Assert.assertEquals(1.0 - 200.0/1440.0, sriUtils.calculateSRI(interval9, interval10), .000001);
    }

    @Test
    public void testCalculateAvgSRIBase() {
        ChronoRecords halfDayDiff = new ChronoRecords(Arrays.asList(new ChronoRecord[]{
                //The first 3 records are in UTC timezone
                new ChronoRecord(
                        Date.from(Instant.parse("2018-11-11T01:30:10Z")),
                        Date.from(Instant.parse("2018-11-11T10:00:10Z")),
                        1.5f, 10f, 7f),
                new ChronoRecord(
                        Date.from(Instant.parse("2018-11-12T01:00:10Z")),
                        Date.from(Instant.parse("2018-11-12T10:00:10Z")),
                        1.0f, 10f, 7f),}));
        List<Interval> recordsAsIntervals = sriUtils.convertChronoRecordsToTimeIntervals(halfDayDiff);
        TreeMap<String, BitSet> dayBitMap = sriUtils.createSleepStateByDateMap(recordsAsIntervals);
        float avgSRI = sriUtils.calculateAverageSRI(dayBitMap);
        Assert.assertEquals(1.0-30.0/1440.0, avgSRI, 0.001);
    }

    @Test
    public void overLappingDays() {
        ChronoRecords halfDayDiff = new ChronoRecords(Arrays.asList(new ChronoRecord[]{
                //The first 3 records are in UTC timezone
                new ChronoRecord(
                        Date.from(Instant.parse("2018-11-11T00:00:10Z")),
                        Date.from(Instant.parse("2018-11-11T06:00:10Z")),
                        10f, 6f, 8f),
                new ChronoRecord(
                        Date.from(Instant.parse("2018-11-11T10:00:10Z")),
                        Date.from(Instant.parse("2018-11-12T06:00:10Z")),
                        10f, 6f, 8f),
                new ChronoRecord(
                        Date.from(Instant.parse("2018-11-12T10:30:10Z")),
                        Date.from(Instant.parse("2018-11-13T06:30:10Z")),
                        10.5f, 6.5f, 8f),
                new ChronoRecord(
                        Date.from(Instant.parse("2018-11-13T10:00:10Z")),
                        Date.from(Instant.parse("2018-11-13T23:59:10Z")),
                        10f, 6f, 8f)}));
        List<Interval> recordsAsIntervals = sriUtils.convertChronoRecordsToTimeIntervals(halfDayDiff);
        TreeMap<String, BitSet> dayBitMap = sriUtils.createSleepStateByDateMap(recordsAsIntervals);
        float avgSRI = sriUtils.calculateAverageSRI(dayBitMap);
        Assert.assertEquals(1.0-45.0/1440.0, avgSRI, 0.001);
    }

    @Test
    public void testSkipDays() {
        ChronoRecords halfDayDiff = new ChronoRecords(Arrays.asList(new ChronoRecord[]{
                //The first 3 records are in UTC timezone
                new ChronoRecord(
                        Date.from(Instant.parse("2018-11-11T00:00:10Z")),
                        Date.from(Instant.parse("2018-11-11T06:00:10Z")),
                        10f, 6f, 8f),
                new ChronoRecord(
                        Date.from(Instant.parse("2018-11-12T00:30:10Z")),
                        Date.from(Instant.parse("2018-11-12T06:30:10Z")),
                        10f, 6f, 8f),
                new ChronoRecord(
                        Date.from(Instant.parse("2018-11-14T00:00:10Z")),
                        Date.from(Instant.parse("2018-11-14T06:00:10Z")),
                        10f, 6f, 8f),
                new ChronoRecord(
                        Date.from(Instant.parse("2018-11-15T00:30:10Z")),
                        Date.from(Instant.parse("2018-11-15T06:30:10Z")),
                        10f, 6f, 8f),
        }));
        List<Interval> recordsAsIntervals = sriUtils.convertChronoRecordsToTimeIntervals(halfDayDiff);
        TreeMap<String, BitSet> dayBitMap = sriUtils.createSleepStateByDateMap(recordsAsIntervals);
        float avgSRI = sriUtils.calculateAverageSRI(dayBitMap);

        // SRI Score One
        float numSRIScores = 2;
        float sriScore1 = 1.0f-60.0f/1440.0f;
        float sriScore2 = 1.0f-60.0f/1440.0f;
        float expectedSRI = (sriScore1 + sriScore2)/numSRIScores;
        Assert.assertEquals(expectedSRI, avgSRI, 0.001);
    }

    @Test // does not work when only one ChronoRecord is given
    public void testOneEntrySleepStateMap_SRI() {
        ChronoRecords oneDayRecord = new ChronoRecords(Arrays.asList(new ChronoRecord[]{
                //The first 3 records are in UTC timezone
                new ChronoRecord(
                        Date.from(Instant.parse("2018-11-11T01:30:10Z")),
                        Date.from(Instant.parse("2018-11-11T10:00:10Z")),
                        1.5f, 10f, 7f)}));
        List<Interval> recordsAsIntervals = sriUtils.convertChronoRecordsToTimeIntervals(oneDayRecord);
        TreeMap<String, BitSet> dayBitMap = sriUtils.createSleepStateByDateMap(recordsAsIntervals);
        float avgSRI = sriUtils.calculateAverageSRI(dayBitMap);
        Assert.assertEquals(-1.0, avgSRI, 0.001); // should be -1 when only one chronorecord is given
    }

    @Test
    public void testEmptySleepStatesMap_SRI() {
        ChronoRecords emptyRecord = new ChronoRecords(Arrays.asList(new ChronoRecord[]{}));
        List<Interval> recordsAsIntervals = sriUtils.convertChronoRecordsToTimeIntervals(emptyRecord);
        TreeMap<String, BitSet> dayBitMap = sriUtils.createSleepStateByDateMap(recordsAsIntervals);

        //test SRI calculation
        float avgSRI = sriUtils.calculateAverageSRI(dayBitMap);
        Assert.assertEquals(-1.0, avgSRI, 0.001); // should be -1 when only one chronorecord is given
    }

    @Test
    public void testModifiedSleepRegularityIndex(){
        float SRIScoreOne = 1.0f - (30.0f + 20.0f)/1440.f; // Score from 2018-11-11 and 2018-11-12
        float SRIScoreTwo = 1.0f - (60.0f)/1440.f; // Score from 2018-11-12 and 2018-11-13
        float SRIScoreThree = 1.0f - (60.0f)/1440.f; // Score from 2018-11-24 and 2018-11-25
        float SRIScoreFour = 1.0f - (120.0f)/1440.f; // Score from 2018-11-25 and 2018-11-26

        float mSRIScoreOne = Math.abs(SRIScoreOne - SRIScoreTwo);
        float mSRIScoreTwo = Math.abs(SRIScoreThree - SRIScoreFour);

        float expectedMSRIScore = (mSRIScoreOne + mSRIScoreTwo )/2.0f;
        Assert.assertEquals(expectedMSRIScore, sriUtils.getSleepIrregularityModified(crStats.getRecords()), .0001);
    }

    @Test
    public void testEmptySleepStatesMap_MSRI() {
        ChronoRecords emptyRecord = new ChronoRecords(Arrays.asList(new ChronoRecord[]{}));
        List<Interval> recordsAsIntervals = sriUtils.convertChronoRecordsToTimeIntervals(emptyRecord);
        TreeMap<String, BitSet> dayBitMap = sriUtils.createSleepStateByDateMap(recordsAsIntervals);

        //test MSRI Calculation
        float avgMSRI = sriUtils.calculateAverageMSRI(dayBitMap);
        Assert.assertEquals(-1.0, avgMSRI, 0.001); // should be -1 when only one chronorecord is given
    }

    @Test // does not work when only one ChronoRecord is given
    public void testOneEntrySleepStateMap_MSRI() {
        ChronoRecords oneDayRecord = new ChronoRecords(Arrays.asList(new ChronoRecord[]{
                //The first 3 records are in UTC timezone
                new ChronoRecord(
                        Date.from(Instant.parse("2018-11-11T01:30:10Z")),
                        Date.from(Instant.parse("2018-11-11T10:00:10Z")),
                        1.5f, 10f, 7f)}));
        List<Interval> recordsAsIntervals = sriUtils.convertChronoRecordsToTimeIntervals(oneDayRecord);
        TreeMap<String, BitSet> dayBitMap = sriUtils.createSleepStateByDateMap(recordsAsIntervals);

        float avgMSRI = sriUtils.calculateAverageMSRI(dayBitMap);
        Assert.assertEquals(-1.0, avgMSRI, 0.001); // should be -1 when only one chronorecord is given
    }
}
