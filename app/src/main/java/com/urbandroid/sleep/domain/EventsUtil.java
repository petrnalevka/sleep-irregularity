package com.urbandroid.sleep.domain;

import androidx.annotation.NonNull;

import com.urbandroid.common.logging.Logger;
import com.urbandroid.sleep.domain.interval.EventInterval;
import com.urbandroid.sleep.domain.interval.EventPair;
import com.urbandroid.sleep.domain.interval.Interval;
import com.urbandroid.sleep.domain.interval.ValueInterval;
import com.urbandroid.util.ScienceUtil;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class EventsUtil {

    public static List<Interval> getIntervals(List<? extends IEvent> events, EventPair pair) {
        return getIntervals(events, pair.getStart(), pair.getEnd());
    }

    public static List<Interval> getIntervals(List<? extends IEvent> events, EventLabel start, EventLabel end) {
        List<Interval> intervals = new ArrayList<Interval>();

        LinkedList<Long> startStack = new LinkedList<>();

        for (IEvent event : events) {
            if (event.getLabel() == start) {
                startStack.add(event.getTimestamp());
            } else if (event.getLabel() == end) {

                if (startStack.size() > 0) {
                    intervals.add(new Interval(startStack.removeLast(), event.getTimestamp()));
                }
            }
        }

//        Collections.sort(intervals, new Comparator<Interval>() {
//            @Override
//            public int compare(Interval i1, Interval i2) {
//                return Long.valueOf(i1.getFrom()).compareTo(i2.getFrom());
//            }
//        });

        return intervals;
    }

    public static List<EventInterval> getEventIntervals(List<? extends Event> events, EventLabel start, EventLabel end) {
        List<EventInterval> intervals = new ArrayList<>();
        LinkedList<Event> startStack = new LinkedList<>();
        for (Event event : events) {
            if (event.getLabel() == start) {
                startStack.add(event);
            } else if (event.getLabel() == end) {
                if (!startStack.isEmpty()) {
                    intervals.add(new EventInterval(startStack.removeFirst(), event));
                }
            }
        }
        return intervals;
    }

    @NotNull
    public static List<EventInterval> getSameEventIntervals(List<? extends Event> events, EventLabel startAndEnd) {
        List<EventInterval> intervals = new ArrayList<>();

        Event lastEvent = null;

        for (Event event : events) {
            if (event.getLabel() == startAndEnd) {
                if (lastEvent != null) {
                    intervals.add(new EventInterval(lastEvent, event));
                }
                lastEvent = event;
            }
        }
        return intervals;
    }

    public static List<IEvent> getEvents(List<? extends IEvent> events, long from, long to, EventLabel ... labels) {
        List<IEvent> result = new ArrayList<>();

        for (IEvent event : events) {
            for (EventLabel label : labels) {
                if (label == event.getLabel()) {
                    if ((event.getTimestamp() >= from) && (event.getTimestamp() <= to)) {
                        result.add(event);
                    }
                }
            }
        }
        return result;
    }


    public static List<ValueInterval> getSameValueIntervals(List<? extends IEvent> events, EventLabel label) {
        if (!label.hasValue()) {
            throw new IllegalArgumentException("Label " + label + " has no values.");
        }

        List<ValueInterval> intervals = new ArrayList<ValueInterval>();

        long fromTimestamp = -1;
        float value = -1;

        for (IEvent event : events) {
            if (event.getLabel() == label) {

                if (fromTimestamp == -1) {
                    fromTimestamp = event.getTimestamp();
                    value = event.getValue();
                } else if (event.getValue() != value) {
                    intervals.add(new ValueInterval(fromTimestamp, event.getTimestamp(), value));
                    fromTimestamp = event.getTimestamp();
                    value = event.getValue();
                }
            }
        }

        return intervals;
    }

    public static List<Interval> getIntervals(List<? extends IEvent> events, EventLabel label) {
        List<Interval> intervals = new ArrayList<Interval>();

        for (IEvent event : events) {
            if (event.getLabel() == label) {
                intervals.add(new Interval(event.getTimestamp(), event.getTimestamp()));
            }
        }

        return intervals;
    }

    public static List<EventLabel> getEventLabels(List<? extends IEvent> events, EventLabel... labels) {
        List<EventLabel> found = new ArrayList<EventLabel>();

        for (Iterator<? extends IEvent> iterator = events.iterator(); iterator.hasNext(); ) {
            IEvent event = iterator.next();
            for (EventLabel label : labels) {
                if  (event.getLabel() == label) {
                    found.add(label);
                }
            }
        }

        return found;
    }

    @NonNull
    public static List<IEvent> getEvents(List<? extends IEvent> events, EventLabel... labels) {
        final List<IEvent> found = new ArrayList<IEvent>();

        for (Iterator<? extends IEvent> iterator = events.iterator(); iterator.hasNext(); ) {
            IEvent event = iterator.next();
            if (event == null) {
                continue;
            }
            for (EventLabel label : labels) {
                if  (event.getLabel() == label) {
                    found.add(event);
                }
            }
        }

        return found;
    }

    public static void clearSleepRecordOverlaps(long sleepFrom, long sleepTo, Events events, EventPair pair) {
        //Logger.logInfo("Awake clearOverlap adding " +pair);

        List<Interval> intervals = getIntervals(events.getCopiedEvents(), pair.getStart(), pair.getEnd());
        events.clearLabels(pair.getStart(), pair.getEnd());

        Interval sleepTime = new Interval(sleepFrom, sleepTo);
        // make sure awake does not overlap sleep time
        for (Interval interval : intervals) {
            Interval intersection = sleepTime.getIntersection(interval);
            if (intersection != null) {
//                if (pair.getStart() == EventLabel.AWAKE_START) {
//                    Logger.logInfo("Awake clearOverlap adding " +pair + " is"+ intersection + " i" + interval);
//                }
//                System.out.println("Awake adding " + intersection + " interval " + interval);
                events.addEvent(intersection.getFrom(), pair.getStart());
                events.addEvent(intersection.getTo(), pair.getEnd());
            } else {
//                Logger.logInfo("clearOverlap no interval " + pair + " i" + interval);
//                System.out.println("Awake no intersection " + interval);
            }
        }
    }

    public static void clearSleepRecordOverlaps(long sleepFrom, long sleepTo, Events events, EventLabel... label) {
        List<IEvent> allEvents = getEvents(events.getCopiedEvents(), label);

        events.clearLabels(label);

        Interval sleepTime = new Interval(sleepFrom, sleepTo);
        // make sure awake does not overlap sleep time
        for (IEvent e : allEvents) {
            if (sleepTime.isIn(e.getTimestamp())) {
                events.addEvent(e.getTimestamp(), e.getLabel(), e.getValue());
            }
        }
    }

    public static void mergeUnionInterval(Events events, Interval interval, EventPair pair) {

        Interval union = getUnionInterval(events, interval, pair);
        Logger.logInfo("Union, clear: " + union + " " + pair.getStart() + " <-> " + pair.getEnd());

        events.clearLabels(union.getFrom(), union.getTo(), pair.getStart(), pair.getEnd());

        events.addEvent(union.getFrom(), pair.getStart());
        events.addEvent(union.getTo(), pair.getEnd());
    }

    public static Interval getUnionInterval(Events events, Interval interval, EventPair pair) {

        List<Interval> eventIntervals = EventsUtil.getIntervals(events.getCopiedEvents(), pair.getStart(), pair.getEnd());

        //Forward union.
        for (Interval i : eventIntervals) {
            Interval union = interval.getUnion(i);
            if (union != null) {
                interval = union;
            }
        }

        //Backwards union. Needed for overlapping intervals.
        Collections.reverse(eventIntervals);
        for (Interval i : eventIntervals) {
            Interval union = interval.getUnion(i);
            if (union != null) {
                interval = union;
            }
        }

        return interval;
    }

    public static boolean hasOverlap(Events events, Interval i, EventPair pair) {
        List<Interval> intervals = EventsUtil.getIntervals(events.getCopiedEvents(), pair.getStart(), pair.getEnd());
        for (Interval interval : intervals) {
            if (interval.hasIntersection(i)) {
                return true;
            }
        }

        return false;
    }

    public static List<Interval> getOverlappingIntervalsWithIntersection(Events events, Interval i, EventPair pair) {
        List<Interval> overlaps = new ArrayList<>();

        List<Interval> intervals = EventsUtil.getIntervals(events.getCopiedEvents(), pair.getStart(), pair.getEnd());
        for (Interval interval : intervals) {
            if (interval.hasIntersection(i)) {
                overlaps.add(interval.getIntersection(i));
            }
        }

        return overlaps;
    }

    public static List<Interval> getOverlappingIntervals(Events events, Interval i, EventPair pair) {
        List<Interval> overlaps = new ArrayList<>();

        List<Interval> intervals = EventsUtil.getIntervals(events.getCopiedEvents(), pair.getStart(), pair.getEnd());
        for (Interval interval : intervals) {
            if (interval.hasIntersection(i)) {
                overlaps.add(interval);
            }
        }

        return overlaps;
    }


    public static void clearIntervalOverlaps(Events events, EventPair top, EventPair bottom) {

        //Logger.logInfo("Awake clearIntervalOverlaps " + top + " -> " + bottom);

        List<Event> copiedEvents = events.getCopiedEvents();
        List<Interval> topIntervals = getIntervals(copiedEvents, top.getStart(), top.getEnd());
        List<Interval> bottomIntervals = getIntervals(copiedEvents, bottom.getStart(), bottom.getEnd());

        events.clearLabels(bottom.getStart(), bottom.getEnd());

        List<Interval> toRemove = new ArrayList<>();
        List<Interval> toAdd = new ArrayList<>();

        for (Interval topInterval : topIntervals) {
            for (Interval bottomInterval : bottomIntervals) {
                if (bottomInterval.hasIntersection(topInterval)) {
                    Collection<Interval> difference = bottomInterval.subtract(topInterval);
                    if (difference != null && !difference.contains(bottomInterval)) {
                        // subtract did something
                        toAdd.addAll(difference);
                        toRemove.add(bottomInterval);
                    }
                }
            }
        }

        bottomIntervals.addAll(toAdd);
        bottomIntervals.removeAll(toRemove);

        for (Interval interval : bottomIntervals) {
            events.addEvent(interval.getFrom(), bottom.getStart());
            events.addEvent(interval.getTo(), bottom.getEnd());
        }
    }

    public static void normalizeOverlaps(long sleepFrom, long sleepTo, Events events) {
        clearSleepRecordOverlaps(sleepFrom, sleepTo, events, new EventPair(EventLabel.TRACKING_PAUSED, EventLabel.TRACKING_RESUMED));
        clearSleepRecordOverlaps(sleepFrom, sleepTo, events, new EventPair(EventLabel.LIGHT_START, EventLabel.LIGHT_END));
        clearSleepRecordOverlaps(sleepFrom, sleepTo, events, new EventPair(EventLabel.DEEP_START, EventLabel.DEEP_END));
        clearSleepRecordOverlaps(sleepFrom, sleepTo, events, new EventPair(EventLabel.REM_START, EventLabel.REM_END));
        clearSleepRecordOverlaps(sleepFrom, sleepTo, events, new EventPair(EventLabel.AWAKE_START, EventLabel.AWAKE_END));
        clearSleepRecordOverlaps(sleepFrom, sleepTo, events, new EventPair(EventLabel.WALKING_START, EventLabel.WALKING_END));
        clearSleepRecordOverlaps(sleepFrom, sleepTo, events, new EventPair(EventLabel.HR_HIGH_START, EventLabel.HR_HIGH_END));

        clearIntervalOverlaps(events, new EventPair(EventLabel.REM_START, EventLabel.REM_END), new EventPair(EventLabel.LIGHT_START, EventLabel.LIGHT_END));
        clearIntervalOverlaps(events, new EventPair(EventLabel.REM_START, EventLabel.REM_END), new EventPair(EventLabel.DEEP_START, EventLabel.DEEP_END));
        clearIntervalOverlaps(events, new EventPair(EventLabel.TRACKING_PAUSED, EventLabel.TRACKING_RESUMED), new EventPair(EventLabel.DEEP_START, EventLabel.DEEP_END));
        clearIntervalOverlaps(events, new EventPair(EventLabel.TRACKING_PAUSED, EventLabel.TRACKING_RESUMED), new EventPair(EventLabel.REM_START, EventLabel.REM_END));
        clearIntervalOverlaps(events, new EventPair(EventLabel.TRACKING_PAUSED, EventLabel.TRACKING_RESUMED), new EventPair(EventLabel.LIGHT_START, EventLabel.LIGHT_END));
        clearIntervalOverlaps(events, new EventPair(EventLabel.TRACKING_PAUSED, EventLabel.TRACKING_RESUMED), new EventPair(EventLabel.AWAKE_START, EventLabel.AWAKE_END));
        clearIntervalOverlaps(events, new EventPair(EventLabel.TRACKING_PAUSED, EventLabel.TRACKING_RESUMED), new EventPair(EventLabel.HR_HIGH_START, EventLabel.HR_HIGH_END));

        clearIntervalOverlaps(events, new EventPair(EventLabel.AWAKE_START, EventLabel.AWAKE_END), new EventPair(EventLabel.DEEP_START, EventLabel.DEEP_END));
        clearIntervalOverlaps(events, new EventPair(EventLabel.AWAKE_START, EventLabel.AWAKE_END), new EventPair(EventLabel.REM_START, EventLabel.REM_END));
        clearIntervalOverlaps(events, new EventPair(EventLabel.AWAKE_START, EventLabel.AWAKE_END), new EventPair(EventLabel.LIGHT_START, EventLabel.LIGHT_END));
    }

    public static void compressEventLabel(SleepRecord record, EventLabel label) {
        compressEventLabel(record, label, 300000, false);
    }

    public static void compressEventLabel(SleepRecord record, EventLabel label, long interval, boolean overlap) {
        compressEventLabel(record, label, interval, interval, false);
    }

    public static void filterRare(SleepRecord record, EventLabel label, long interval) {


        List<Event> copiedEvents = record.getEvents().getCopiedEvents();
        List<IEvent> events = EventsUtil.getEvents(copiedEvents, label);

        record.getEvents().clearLabels(label);

        long minDiff = -1;
        for (int i = 0; i < events.size(); i++) {

            long diffLeft = events.get(Math.min(i + 1, events.size() - 1)).getTimestamp() - events.get(i).getTimestamp();
            long diffRight = events.get(i).getTimestamp() - events.get(Math.max(0, i - 1)).getTimestamp();

            minDiff = Math.min(diffLeft, diffRight);

            if (minDiff == 0) {
                minDiff = Math.max(diffLeft, diffRight);
            }

            if (minDiff < interval)  {
                record.getEvents().addEvent(events.get(i));
            }
        }

    }


    public static void compressEventLabel(SleepRecord record, EventLabel label, long interval, long avgInterval, boolean overlap) {

        long halfInterval = interval / 2;

        long eventInterval = overlap ? halfInterval : interval;

        long from = record.getFromTime();
        long to = record.getToTime();

        List<Event> copiedEvents = record.getEvents().getCopiedEvents();
        List<IEvent> events = EventsUtil.getEvents(copiedEvents, label);


        long length = to - from;

        long intervalExt = Math.abs(interval - avgInterval) / 2;

        if (events.size() < 20) {
            return;
        }

        long minDiff = -1;
        for (int i = 1; i < events.size() - 1; i++) {
            long tsDiffRight = events.get(i + 1).getTimestamp() - events.get(i).getTimestamp();
            long tsDiffLeft = events.get(i).getTimestamp() - events.get(i - 1).getTimestamp();
            if (minDiff == -1) {
                minDiff = tsDiffLeft;
            } else if (tsDiffRight < minDiff) {
                minDiff = tsDiffRight;
            } else if (tsDiffLeft < minDiff) {
                minDiff = tsDiffLeft;
            }
        }

        if (minDiff == -1 || minDiff >= eventInterval - 1) {
            Logger.logInfo("SleepRecord: not doing compression " + minDiff);
            return;
        }

        // compress when record is at least 1 hour long
        if (length > 3600000 && (length / interval) < 200) {
            Logger.logInfo("SleepRecord: compression " + label + " minDiff " + minDiff);
            record.getEvents().clearLabels(label);


            for (long i = from; i <= to; i = i + eventInterval) {
                events = EventsUtil.getEvents(copiedEvents, i - intervalExt + 1, i + interval + intervalExt, label);

                if (events != null && events.size() > 0) {
                    if (label.hasValue()) {
                        float[] values = new float[events.size()];
                        for (int j = 0; j < values.length; j++) {
                            values[j] = events.get(j).getValue();
                        }
                        record.getEvents().addEvent(i+halfInterval, label, ScienceUtil.avg(values));
                    } else {
                        record.getEvents().addEvent(i+halfInterval, label);
                    }
                }
            }
        }
    }


    public static void mergeOverlaps(SleepRecord sleepRecord, EventPair pair, long minGap) {
        mergeOverlaps(sleepRecord, pair, minGap, -1);
    }

    public static void mergeOverlaps(SleepRecord sleepRecord, EventPair pair, long minGap, long minLength) {

        Logger.logInfo("Event: merge " + pair);

        List<Interval> intervals = EventsUtil.getIntervals(sleepRecord.getEvents().getCopiedEvents(), pair);

        for (Interval interval : intervals) {
            Logger.logInfo("Event: merge Before " + interval);
        }

        if (intervals.size() > 1) {
            sleepRecord.getEvents().clearLabels(pair.getStart(), pair.getEnd());
            List<EventInterval> result = new ArrayList<>();

            Interval previousInterval = intervals.get(0);

            for (int i = 1; i < intervals.size(); i++) {
                Interval currentInterval = intervals.get(i);

                if ((currentInterval.getFrom() - previousInterval.getTo()) <= minGap) {
                    currentInterval = new Interval(previousInterval.getFrom(), currentInterval.getTo());
                } else {
                    long from = previousInterval.getFrom();
                    long to = previousInterval.getTo();
                    if ((minLength > 0) && previousInterval.getLength() < minLength) {
                        from = Math.max(to - minLength, sleepRecord.getFromTime());
                    }
                    result.add(new EventInterval(new Event(from, pair.getStart(), 0), new Event(to, pair.getEnd(), 0)));
                }
                previousInterval = currentInterval;
            }
            long from = previousInterval.getFrom();
            long to = previousInterval.getTo();
            if ((minLength > 0) && previousInterval.getLength() < minLength) {
                from = Math.max(to - minLength, sleepRecord.getFromTime());
            }
            result.add(new EventInterval(new Event(from, pair.getStart(), 0), new Event(to, pair.getEnd(), 0)));

            for (EventInterval eventInterval : result) {
                Logger.logInfo("Event: merge After " + eventInterval.toInterval());

                sleepRecord.getEvents().addEvent(eventInterval);
            }
        }
    }

    public static String marshal(Event e) {
        return +e.getTimestamp()+";"+e.getLabel()+";"+e.getValue();
    }

    public static Event unmarshal(String s) {
        String[] parts = s.split(";");
        if (parts.length != 3) {
            throw new IllegalArgumentException(s);
        }
        return new Event(Long.parseLong(parts[0]), EventLabel.valueOf(parts[1]), Float.parseFloat(parts[2]));
    }
}