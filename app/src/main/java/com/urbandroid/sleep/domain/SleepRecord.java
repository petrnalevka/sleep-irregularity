package com.urbandroid.sleep.domain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.urbandroid.common.logging.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class SleepRecord implements TimeRecord {

    public static final int V9 = 10008;

    public static final int COMPRESSION = 100;

    public static final float INITIAL_UNCOMPUTED_QUALITY = -2.0f;
    public static final int INITIAL_UNCOMPUTED_INT = -1;
    public static final float INITIAL_UNCOMPUTED_FLOAT = -1.0f;
    public static final float INITIAL_UNCOMPUTED_FLOAT_NEGATIVE_SAFE = -1e+21f;
    public static final float NOT_APPLICABLE_QUALITY = -1.0f;

    // Tracking was broken, we have to fill the value manually.
    public static final float BROKEN_VALUE = -0.001f;
    // Value used for suspend tracking, value-in-middle removal, ...
    public static final float REMOVED_VALUE = -0.01f;

    public static final long MIN_AWAKE_INTERVAL = 5 * 60000;
    public static final long MIN_SNOOZE_AWAKE_INTERVAL = 2 * 60000;

    /** Date when sleeping measurement started */
    @NonNull
    private Date from;
    /** Date when the sleeper was woken-up. Can be set even during tracking and updated later when more data are available. */
    @Nullable
    private Date to;
    /** Latest date, when the sleeper could be potentially woken up */
    @Nullable
    private Date lastestTo;
    /** Timezone of this record */
    private String timezone;

    /** Geocell with resolution 7 **/
    private String geo;

    /** Internal tracking, whether this record is already finished (either alarm, or user exist action). */
    private boolean finished;

    /** User rating of how well the slept */
    private float rating;
    /** User comment about his the sleep */
    private String comment;

    /** average noise level as % of maximum amplitude level Short.MAX_VALUE */
    private float noiseLevel = INITIAL_UNCOMPUTED_FLOAT;

    private float lastEntry;

    /**
     * Adjustments to sleep duration, either done but shrinking the graph or other edits or
     * because of fall a sleep detecting or pause sleep tracking.
     */
    private int lenAdjust = 0;

    /**
     * deep sleep %
     */
    private float quality = INITIAL_UNCOMPUTED_QUALITY;

    /**
     * number of detected deep sleep cycles
     */
    private int cycles = INITIAL_UNCOMPUTED_INT;

    /**
     * snoring length in seconds
     */
    private int snore = INITIAL_UNCOMPUTED_INT;

    /**
     AHI (Apnea-hypopnea index) is an index for measuring the severity of sleep apnea. It is defined as the total number of apnea and hyponea occurred per hour of sleep. Each apnea must last for at least 10 seconds and affect the blood oxygenation. The following table shows the classifications.

     AHI	Classification
     5 - 15	Mild
     15 - 30	Moderate
     >30	Severe
     Besides using an oximeter as a screening tool, one can use the oximeter to monitor the effect of treatment and to see how one is doing.

     Here shows some of the criterions used in various studies on detecting an apnea event.

    /** In milliseconds. WARN: This has meaning only before any filtering is applied, later this value becomes useless.. */
    private int version;

    public static final int MIN_PAUSED_ADD_GAPS_INTERVAL = 0;
    public static final int MIN_PAUSED_GAP_INTERVAL = 750000;
    public static final int REM_INTERVAL = 1200000;
    public static final int QUALITY_MIN_LENGTH = 1200000;

    private List<Float> history = new ArrayList<>();

    /** Noise level records */
    private List<Float> noiseHistory = new ArrayList<>();

    private List<Float> hrHistory = new ArrayList<>();

    private Events events = new Events();

    /**
     * Non-aggregated raw history to show in the beginning of sleep tracking
     */
    private List<Float> tempHistory = new ArrayList<>();

    // Are the raw data loaded in new format?
    private boolean rawtimestampedEventLabelsNewFormat = false;

    // Temporary non-decoded field containing timestampedEventLabels loaded from db.
    // This is deserialized on first access as deserialization seems to cost something and it is not always required.
    private byte[] rawtimestampedEventLabels;

    private boolean hideSleepAnalysis = false;

    //Used for exact mapping of raw activity values to the real clock time.
    //Not persistent, used only hypnogram calculation when the record is finished.
    private long lastActivityUpdate;

    public SleepRecord(@NonNull Date from, @Nullable Date to, int version) {
        this(null, from, to, version);
    }

    public SleepRecord(String timezone, @NonNull Date from, @NonNull Date lastestTo, int version) {
        if ( lastestTo == null )
            throw new IllegalArgumentException("Latest to date must not be null");
        if (from == null)
            throw new IllegalArgumentException("From must not be null");
        this.timezone = timezone;
        this.from = from;
        this.lastestTo = lastestTo;
        this.version = version;
        this.comment = "";
        this.rating = 0.0f;
        this.lenAdjust = 0;
    }

    // Copy constructor
    public SleepRecord(@NonNull SleepRecord original, boolean filterCopy) {
        if (original.from == null) {
            throw new IllegalArgumentException("Original from should never by null.");
        }
        from = new Date(original.from.getTime());
        to = original.to == null ? null : new Date(original.to.getTime());
        finished = original.finished;
        lastestTo = original.lastestTo == null ? null : new Date(original.lastestTo.getTime());
        timezone = original.timezone;
        version = original.version;
        comment = original.comment;
        tempHistory = original.tempHistory;
        rating = original.getRating();
        lastEntry = original.getLastEntry();
        if (filterCopy) {
            history = original.getFilteredHistory();
        } else {
            history = original.getHistory();
        }
        noiseHistory = original.getNoiseHistory();
        lenAdjust = original.getLenAdjust();
        quality = original.getQuality();
        cycles = original.getCycles();
        snore = original.getSnore();
        noiseLevel = original.getNoiseLevel();
        events = new Events(original.events);
        geo = original.getGeo();
        lastActivityUpdate = original.lastActivityUpdate;
    }

    @Override
    @NonNull
    public Date getFrom() {
        return from;
    }

    @Override
    @Nullable
    public Date getTo() {
        return to;
    }

    @Nullable
    public Date getLastestTo() {
        return lastestTo;
    }

    public String getTimezone() {
        return timezone;
    }

    public TimeZone getTimeZone() {
        if (timezone == null) {
            return TimeZone.getDefault();
        }

        try {
            return TimeZone.getTimeZone(timezone);
        } catch (Exception e) {
            Logger.logWarning("Failed to parse timezone: " + timezone, e);
            return TimeZone.getDefault();
        }
    }

    public String getGeo() {
        return geo;
    }

    public boolean isFinished() {
        return finished;
    }

    public float getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public float getNoiseLevel() {
        return noiseLevel;
    }

    public float getLastEntry() {
        return lastEntry;
    }

    public int getLenAdjust() {
        return lenAdjust;
    }

    public float getQuality() {
        return quality;
    }

    public int getCycles() {
        return cycles;
    }

    public int getSnore() {
        return snore;
    }

    public int getVersion() {
        return version;
    }

    public List<Float> getHistory() {
        return history;
    }

    public List<Float> getNoiseHistory() {
        return noiseHistory;
    }

    public List<Float> getHrHistory() {
        return hrHistory;
    }

    public List<Float> getTempHistory() {
        return tempHistory;
    }

    public List<Float> getFilteredHistory() {
        return history;
    }

    public boolean isRawtimestampedEventLabelsNewFormat() {
        return rawtimestampedEventLabelsNewFormat;
    }

    public byte[] getRawtimestampedEventLabels() {
        return rawtimestampedEventLabels;
    }

    public boolean isHideSleepAnalysis() {
        return hideSleepAnalysis;
    }

    public long getLastActivityUpdate() {
        return lastActivityUpdate;
    }

    public long getFromTime() {
        return from.getTime();
    }

    public long getToTime() {
        return getNotNullTo().getTime();
    }

    public Date getNotNullTo() {
        return getTo() == null ? new Date() : getTo();
    }


    public synchronized Events getEvents() {
        return events;
    }

    public int getSnoozeTime() {
        if (!getEvents().hasLabel(EventLabel.ALARM_STARTED)) {
            return -1;
        }
        List<IEvent> events = EventsUtil.getEvents(getEvents().getCopiedEvents(), EventLabel.ALARM_SNOOZE);
        if (events.size() > 0) {
            Collections.sort(events, new Comparator<IEvent>() {
                @Override
                public int compare(IEvent e1, IEvent e2) {
                    return Long.valueOf(e1.getTimestamp()).compareTo(Long.valueOf(e2.getTimestamp()));
                }
            });
            return (int) (getToTime() - events.get(0).getTimestamp()) / 60000;
        }
        return 0;
    }


}