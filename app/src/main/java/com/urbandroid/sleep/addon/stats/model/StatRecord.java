package com.urbandroid.sleep.addon.stats.model;

import com.urbandroid.common.logging.Logger;
import com.urbandroid.sleep.domain.Event;
import com.urbandroid.sleep.domain.EventLabel;
import com.urbandroid.sleep.domain.Events;
import com.urbandroid.sleep.domain.EventsUtil;
import com.urbandroid.sleep.domain.IEvent;
import com.urbandroid.sleep.domain.SleepRecord;
import com.urbandroid.sleep.domain.tag.Tag;

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

public class StatRecord implements IStatRecord {

    private Date toDate;
    private Date fromDate;

    private TimeZone timeZone;

    private double fromHour;
    private double toHour;

    private float lengthInHours;
    private float trackLengthInHours;
    private float hrv = SleepRecord.INITIAL_UNCOMPUTED_FLOAT;
    private float hrvAfter = SleepRecord.INITIAL_UNCOMPUTED_FLOAT;
    private float hrvBefore = SleepRecord.INITIAL_UNCOMPUTED_FLOAT;
    private float hrvGain = SleepRecord.INITIAL_UNCOMPUTED_FLOAT_NEGATIVE_SAFE;
    private float irregularity = SleepRecord.INITIAL_UNCOMPUTED_FLOAT;
    private float rating = SleepRecord.INITIAL_UNCOMPUTED_FLOAT;
    private float quality = SleepRecord.INITIAL_UNCOMPUTED_QUALITY;
    private float noiseLevel = SleepRecord.INITIAL_UNCOMPUTED_FLOAT;
    private int snore = SleepRecord.INITIAL_UNCOMPUTED_INT;
    private int count = 1;
    private int smart = SleepRecord.INITIAL_UNCOMPUTED_INT;
    private int rdi = SleepRecord.INITIAL_UNCOMPUTED_INT;
    private double cycles = 0d;

    private int snooze = SleepRecord.INITIAL_UNCOMPUTED_INT;

    private float[] data;

    private Set<String> tags = new HashSet<String>();

    public StatRecord() {
    }

    public StatRecord(Date fromDate, Date toDate, TimeZone timeZone, double fromHour, double toHour) {
        this.toDate = toDate;
        this.fromDate = fromDate;
        this.timeZone = timeZone;
        this.fromHour = fromHour;
        this.toHour = toHour;
    }

    public int getSnooze() {
        return snooze;
    }

    public void setSnooze(int snooze) {
        this.snooze = snooze;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public float[] getData() {
        return data;
    }

    public void setData(float[] data) {
        this.data = data;
    }

    public double getFromHour() {
        return fromHour;
    }

    public void setFromHour(double fromHour) {
        this.fromHour = fromHour;
    }

    public double getToHour() {
        return toHour;
    }

    public void setToHour(double toHour) {
        this.toHour = toHour;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("(SLEEP from ").append(fromDate).append(" to ").append(toDate).append(")").toString();
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public float getLengthInHours() {
        return lengthInHours;
    }

    public void setLengthInHours(float length) {
        this.lengthInHours = length;
    }

    public float getTrackLengthInHours() {
        return trackLengthInHours;
    }

    public void setTrackLengthInHours(float trackLengthInHours) {
        this.trackLengthInHours = trackLengthInHours;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public void addRating(float rating) {
        if (rating > this.rating) {
            this.rating = rating;
        }
    }

    public float getQuality() {
        return quality;
    }

    public void setQuality(float quality) {
        this.quality = quality;
    }

    public int getSnore() {
        return snore;
    }

    public void setSnore(int snore) {
        this.snore = snore;
    }

    public float getNoiseLevel() {
        return noiseLevel;
    }

    public void setNoiseLevel(float noiseLevel) {
        this.noiseLevel = noiseLevel;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public double getCycles() {
        return cycles;
    }

    public void setCycles(double cycles) {
        this.cycles = cycles;
    }

    @Override
    public int getSmart() {
        return smart;
    }

    public void setSmart(int smart) {
        this.smart = smart;
    }

    @Override
    public float getHrv() {
        return hrv;
    }

    public void setHrv(float sdnn) {
        this.hrv = sdnn;
    }

    @Override
    public float getHrvAfter() {
        return hrvAfter;
    }

    public void setHrvAfter(float hrvAfter) {
        this.hrvAfter = hrvAfter;
    }

    @Override
    public float getHrvBefore() {
        return hrvBefore;
    }

    public void setHrvBefore(float hrvBefore) {
        this.hrvBefore = hrvBefore;
    }

    @Override
    public int getRdi() {
        return rdi;
    }

    public void setRdi(int rdi) {
        this.rdi = rdi;
    }

    @Override
    public float getHrvGain() {
        return hrvGain;
    }

    public void setHrvGain(float hrvGain) {
        this.hrvGain = hrvGain;
    }

    @Override
    public float getIrregularityInMinutes() {
        return irregularity;
    }

    public void setIrregularity(float irregularity) {
        this.irregularity = irregularity;
    }

    public static StatRecord fromSleepRecord(com.urbandroid.sleep.domain.SleepRecord record) {

        StatRecord result = new StatRecord();
        result.setToDate(record.getTo());
        result.setFromDate(record.getFrom());
        result.setTimeZone(record.getTimeZone());

        Calendar cal = getCalendar(record);

        cal.setTime(record.getFrom());
        double hour = cal.get(Calendar.HOUR_OF_DAY);
        hour = hour + ((double)cal.get(Calendar.MINUTE) / 60);
        result.setFromHour(hour);

        cal.setTime(record.getTo());
        hour = cal.get(Calendar.HOUR_OF_DAY);
        hour = hour + ((double)cal.get(Calendar.MINUTE) / 60);
        result.setToHour(hour);

        result.setRating(record.getRating());
        result.setQuality(record.getQuality());
        result.setSnore(record.getSnore());
        result.setSnooze(record.getSnoozeTime());

        result.getTags().addAll(Tag.getTags(record.getComment()));


        long trackLenMs = record.getTo().getTime() - record.getFrom().getTime();
        result.setLengthInHours(((trackLenMs / 60000f)  + record.getLenAdjust()) / 60f);
        result.setTrackLengthInHours(trackLenMs / 3600000f);

        if (result.getHrvAfter() > 0 && result.getHrvBefore() > 0) {
            result.setHrvGain(result.getHrvAfter() - result.getHrvBefore());
        }

        long time = System.currentTimeMillis();
        int rdi = -1;
        // TODO compute rdi
        result.setRdi(rdi);
        if (rdi > 0) {
            Logger.logInfo("Stats: Rdi resolve: " + (System.currentTimeMillis() - time) + "ms");
        }
        result.setCycles(record.getCycles());
        result.setNoiseLevel(record.getNoiseLevel());

        List<Event> labels = record.getEvents().getCopiedEvents();
        List<IEvent> alarmLatestEvents = EventsUtil.getEvents(labels, EventLabel.ALARM_LATEST);
        List<IEvent> alarmStartedEvents = EventsUtil.getEvents(labels, EventLabel.ALARM_STARTED);
        if (alarmLatestEvents.size() > 0 && alarmStartedEvents.size() > 0) {
            int beforeAlarmMin = (int) (alarmLatestEvents.get(0).getTimestamp() - alarmStartedEvents.get(0).getTimestamp()) / 60000;
            if (beforeAlarmMin > 0) {
                result.setSmart(beforeAlarmMin);
            }
        }

        return  result;
    }


    private static Calendar getCalendar(com.urbandroid.sleep.domain.SleepRecord sleepRecord) {
        Calendar cal = Calendar.getInstance();
        if ((sleepRecord.getTimezone() != null) && (!sleepRecord.getTimezone().equals(""))) {
            try {
                cal.setTimeZone(TimeZone.getTimeZone(sleepRecord.getTimezone()));
            } catch (Exception e) {
                Logger.logSevere(e);
            }
        }
        return cal;
    }
}


