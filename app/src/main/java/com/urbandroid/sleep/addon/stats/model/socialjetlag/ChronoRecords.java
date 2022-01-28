package com.urbandroid.sleep.addon.stats.model.socialjetlag;

import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

public class ChronoRecords {

    private final Date from, to;
    private final TreeMap<Date,ChronoRecord> records;

    public ChronoRecords() {
        this(Collections.<ChronoRecord>emptyList());
    }

    public ChronoRecords(Collection<? extends ChronoRecord> recs) {
        this(recs, null, null);
    }

    public ChronoRecords(Collection<? extends ChronoRecord> recs, Date from, Date to) {

        records = new TreeMap<>();
        for(ChronoRecord rec : recs) {
            records.put(rec.getTo(), rec);
        }

        if (from == null) {
            if (records.isEmpty()) {
                this.from = new Date();
            } else {
                this.from = records.firstKey();
            }
        } else {
            if (!records.isEmpty() && records.firstKey().before(from)) {
                throw new IllegalArgumentException(records.firstKey() + " " + from);
            } else {
                this.from = from;
            }
        }

        if (to == null) {
            if (records.isEmpty()) {
                this.to = this.from;
            } else {
                this.to = records.lastKey();
            }
        } else {
            if (!records.isEmpty() && records.lastKey().after(to)) {
                throw new IllegalArgumentException(records.lastKey()+" "+to);
            } else {
                this.to = to;
            }
        }

        if (this.to.before(this.from)) {
            throw new IllegalArgumentException(this.from+" "+this.to);
        }
    }

    public int size() {
        return records.size();
    }

    public Date getFrom() {
        return from;
    }

    public Date getTo() {
        return to;
    }

    public float[] getFromHours() {
        return toFloats(new RecordToFloat() {
            @Override
            public float apply(ChronoRecord record) {
                return record.getFromHour();
            }
        });
    }

    public float[] getToHours() {
        return toFloats(new RecordToFloat() {
            @Override
            public float apply(ChronoRecord record) {
                return record.getToHour();
            }
        });
    }

    public float[] getMidSleeps() {
        return toFloats(new RecordToFloat() {
            @Override
            public float apply(ChronoRecord record) {
                return record.getMidSleep();
            }
        });
    }

    public float[] getMidSleepUTC() {
        return toFloats(new RecordToFloat() {
            @Override
            public float apply(ChronoRecord record) {
                return record.getMidSleepUTC();
            }
        });
    }

    public float[] getLengths() {
        return toFloats(new RecordToFloat() {
            @Override
            public float apply(ChronoRecord record) {
                return record.getLength();
            }
        });
    }

    public float[] toFloats(RecordToFloat converter) {
        int size = records.size();
        float[] result = new float[size];
        int i=0;
        for(ChronoRecord record : records.values()) {
            result[i++] = converter.apply(record);
        }
        return result;
    }

    public Pair<ChronoRecords, ChronoRecords> split(RecordToBool predicate) {
        List<ChronoRecord> trueList = new ArrayList<>();
        List<ChronoRecord> falseList = new ArrayList<>();
        for (ChronoRecord record : records.values()) {
            if (predicate.apply(record)) {
                trueList.add(record);
            } else {
                falseList.add(record);
            }
        }
        return Pair.create(new ChronoRecords(trueList, from, to), new ChronoRecords(falseList, from, to));
    }

    public ChronoRecords narrow(Date from, Date to) {
        return new ChronoRecords(records.subMap(from,to).values(), from, to);
    }

    public List<ChronoRecords> splitByMonth(int fragmentLenghtMonths, int stepMonths) {

        List<ChronoRecords> result = new ArrayList<>();

        Date start = records.firstKey();
        Date end = new Date(records.lastKey().getTime() + 1);

        Calendar c = Calendar.getInstance();
        c.setTime(start);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        Date fragmentStart;
        Date fragmentEnd;
        do {
            fragmentStart = c.getTime();

            c.add(Calendar.MONTH, fragmentLenghtMonths);
            fragmentEnd = c.getTime();

            result.add(narrow(fragmentStart, fragmentEnd));

            c.add(Calendar.MONTH, stepMonths - fragmentLenghtMonths);

        } while (fragmentEnd.before(end));

        return result;
    }

    public List<ChronoRecords> splitByDays(int fragmentLenghtDays, int stepDays) {

        List<ChronoRecords> result = new ArrayList<>();
        if (records.isEmpty()) {
            return result;
        }

        Date start = records.firstKey();
        Date end = new Date(records.lastKey().getTime() + 1);

        Calendar c = Calendar.getInstance();
        c.setTime(start);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        Date fragmentStart;
        Date fragmentEnd;
        do {
            fragmentStart = c.getTime();

            c.add(Calendar.DAY_OF_YEAR, fragmentLenghtDays);
            fragmentEnd = c.getTime();

            result.add(narrow(fragmentStart, fragmentEnd));

            c.add(Calendar.DAY_OF_YEAR, stepDays - fragmentLenghtDays);

        } while (fragmentEnd.before(end));

        return result;
    }

    /**
     * @param dayOfWeek a constant from java.util.Calendar, e.g. Calendar.FRIDAY
     * @return
     */
    public ChronoRecords filterByDayOfWeek(int dayOfWeek) {
        List<ChronoRecord> result = new ArrayList<>();
        for(ChronoRecord r: records.values()) {
            if (r.getEndDayOfWeek() == dayOfWeek) {
                result.add(r);
            }
        }
        return new ChronoRecords(result);
    }

    public TreeMap<Date, ChronoRecord> getRecords() {
        return new TreeMap<>(records);
    }

    public List<ChronoRecord> getRecordsList() {
        return new ArrayList<>(records.values());
    }

    public interface RecordToFloat {
        float apply(ChronoRecord record);
    }

    public interface RecordsToFloat {
        float apply(ChronoRecords records);
    }

    public interface RecordToBool {
        boolean apply(ChronoRecord record);
    }
}
