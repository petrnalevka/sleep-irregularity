package com.urbandroid.sleep.addon.stats.model.socialjetlag;

import static org.assertj.core.api.Assertions.assertThat;

import com.urbandroid.sleep.addon.stats.model.StatRecord;
import com.urbandroid.util.ScienceUtil;

import org.junit.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

public class SocialJetlagStatsTest {

    private ChronoRecords cr = new ChronoRecords(Arrays.asList(new ChronoRecord[]{
            //The first 3 records are in UTC timezone
            new ChronoRecord(
                    Date.from(Instant.parse("2018-11-11T01:30:10Z")),
                    Date.from(Instant.parse("2018-11-11T10:00:10Z")),
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

    @Test
    public void useUtc() {
        SocialJetlagStats  stats = new SocialJetlagStats(cr, true);
        assertThat(stats.getSleepIrregularity()).isEqualTo(0.42305467f);
    }

    @Test
    public void dontUseUtc() {
        SocialJetlagStats  stats = new SocialJetlagStats(cr, false);
        assertThat(stats.getSleepIrregularity()).isEqualTo(0.7163131f);
    }

    @Test
    public void testGetRecordIrregularity() {

        SocialJetlagStats  stats = new SocialJetlagStats(cr, false);
        assertThat(CyclicFloatKt.center(stats.getRecords().getMidSleeps(), 24)).isEqualTo(6.625f);
        assertThat(ScienceUtil.avg(stats.getRecords().getLengths())).isEqualTo(6.5f);

        StatRecord record = new StatRecord(
                Date.from(Instant.parse("2018-11-11T01:30:10Z")),
                Date.from(Instant.parse("2018-11-11T10:00:10Z")),
                TimeZone.getTimeZone("UTC"),
                0, 7.0);
        record.setTrackLengthInHours(8.5f);

        assertThat(stats.getRecordIrregularity(record)).isEqualTo( ((6.625f - 3.5f) + (8.5f - 6.5f)) / 2f );
    }
}
