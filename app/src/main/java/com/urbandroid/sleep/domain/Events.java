package com.urbandroid.sleep.domain;

import androidx.annotation.NonNull;

import com.urbandroid.common.logging.Logger;
import com.urbandroid.sleep.domain.interval.EventInterval;
import com.urbandroid.sleep.domain.interval.Interval;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

public class Events implements Serializable {

    private final Object sync = new Object();

    private final List<Event> events = new ArrayList<>();

    public Events() {
    }

    public Events(Events other) {
        events.addAll(other.getCopiedEvents());
    }

    @Deprecated
    // Used only in old compatibility mode. Does not preserve values!
    public Events(Map<Long, EventLabel> timestampedEventLabels) {
        // Use tree map to sorted labels.
        TreeMap<Long, EventLabel> treeMap = null;
        if (timestampedEventLabels == null) {
            treeMap = new TreeMap<Long, EventLabel>();
        } else {
            treeMap = new TreeMap<Long, EventLabel>(timestampedEventLabels);
        }
        for (Map.Entry<Long, EventLabel> entry : treeMap.entrySet()) {
            events.add(new Event(entry.getKey(), entry.getValue()));
        }
    }

    public void addEvent(IEvent event) {
        synchronized (sync) {
            addEvent(event.getTimestamp(), event.getLabel(), event.getValue());
        }
    }

    public void addEvent(EventInterval eventInterval) {
        synchronized (sync) {
            addEvent(eventInterval.getFrom().getTimestamp(), eventInterval.getFrom().getLabel(), eventInterval.getFrom().getValue());
            addEvent(eventInterval.getTo().getTimestamp(), eventInterval.getTo().getLabel(), eventInterval.getTo().getValue());
        }
    }

    public void addEvent(long timestamp, @NonNull EventLabel label) {
        synchronized (sync) {
            addEvent(timestamp, label, 0.0f);
        }
    }

    public void addEvent(long timestamp, @NonNull EventLabel label, float value) {
        addEvent(timestamp, label, value, 0);
    }

    public void addEvent(long timestamp, @NonNull EventLabel label, float value, int retry) {
        synchronized (sync) {
            if (label == null) {
                label = EventLabel.UNKNOWN;
            }
//            if (label == EventLabel.AWAKE_START) {
//                Logger.logInfo("Awake: Adding AWAKE_START " + timestamp);
//            } else if (label == EventLabel.AWAKE_END) {
//                Logger.logInfo("Awake: Adding AWAKE_END " + timestamp);
//            } else if (label == EventLabel.TRACKING_PAUSED) {
//                Logger.logInfo("Awake: Adding TRACKING_PAUSED " + timestamp);
//            } else if (label == EventLabel.TRACKING_RESUMED) {
//                Logger.logInfo("Awake: Adding TRACKING_RESUMED " + timestamp);
//            }
            if (events.size() > 0) {
                ListIterator<Event> listIterator = events.listIterator(events.size());
                while (listIterator.hasPrevious()) {
                    Event event = listIterator.previous();
                    final EventLabel currentLabel = event.getLabel() == null ? EventLabel.UNKNOWN : event.getLabel();
                    if (currentLabel.ordinal() == label.ordinal() && event.getTimestamp() == timestamp) {
                        // workaround for https://secure.helpscout.net/conversation/1222245688/105356/
                        if (label == EventLabel.AWAKE_START || label == EventLabel.AWAKE_END || label == EventLabel.TRACKING_PAUSED || label == EventLabel.TRACKING_RESUMED) {
                            if (retry < 30) {
                                Logger.logInfo("Awake: Same timestamp elements, fixing ts=" + (timestamp + 1) + " retry=" + retry);
                                addEvent(timestamp + 1, label, value, retry + 1);
                            } else {
                                listIterator.next();  // So that we add after current element.
                                listIterator.add(new Event(timestamp, label, value));
                            }
                        }
                        return;
                    }
                    if (event.getTimestamp() < timestamp || (event.getTimestamp() == timestamp && currentLabel.ordinal() > label.ordinal())) {
                        listIterator.next();  // So that we add after current element.
                        listIterator.add(new Event(timestamp, label, value));
                        return;
                    }
                }
            }
            // If we got here, there is either no element, or all elements have greater timestamp then currently added element.
            events.add(0, new Event(timestamp, label, value));
        }
    }

    //No duplicity check, no ordering check. Use it only if you really know what you are doing.
    void addEventDirect(Event event) {
        synchronized (sync) {
            events.add(event);
        }
    }

    public List<Event> getCopiedEvents() {
        synchronized (sync) {
            return new ArrayList<>(events);
        }
    }

    public int size() {
        synchronized (sync) {
            return events.size();
        }
    }

    public boolean isEmpty() {
        synchronized (sync) {
            return events.isEmpty();
        }
    }

    // Compatibility method for old functionality
    public HashMap<Long, EventLabel> toHashMap() {
        synchronized (sync) {
            HashMap<Long, EventLabel> map = new HashMap<Long, EventLabel>();
            for (Event event : events) {
                map.put(event.getTimestamp(), event.getLabel());
            }
            return map;
        }
    }

    static EventLabel getEventLabelFromString(String labelString) {
        if (labelString != null) {
            try {
                return EventLabel.valueOf(labelString);
            } catch (Exception ee) {
//                ee.printStackTrace();
                // Intentionally not using Logger here, to keep the class reusable outside of android.
//                System.out.print("Unknown label string: " + labelString);
            }
        }
        return null;
    }

    static Event deserializeEvent(long timestamp, int labelOrdinal, String labelString, float value) {
        EventLabel eventLabel = getEventLabelFromString(labelString);
        if (eventLabel == null) {
            try {
                eventLabel = EventLabel.values()[labelOrdinal];
            } catch (Exception e) {
                e.printStackTrace();
                // Intentionally not using Logger here, to keep the class reusable outside of android.
                System.out.print("Unknown label ordinal: " + labelOrdinal);
            }
        }
        if (eventLabel == null) {
            eventLabel = EventLabel.UNKNOWN;
        }
        return new Event(timestamp, eventLabel, eventLabel == EventLabel.UNKNOWN ? labelString : null, value);
    }

    public static Events parseNewFormat(byte[] bytes) {
        Events events = new Events();
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));
            dis.readByte(); // Read version.. ignored now as we have just one

            int recordsCount = dis.readInt();
            for (int i = 0; i < recordsCount; i++) {
                long timestamp = dis.readLong();
                byte labelOrdinal = dis.readByte();
                boolean hasLabelString = dis.readBoolean();
                String labelString = null;
                if (hasLabelString) {
                    labelString = dis.readUTF();
                }
                float value = dis.readFloat();
                events.addEventDirect(deserializeEvent(timestamp, labelOrdinal, labelString, value));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return events;
    }

    private static final byte SERIALIZATION_VERSION = 1;

    public byte[] serializeToBytes() {
        synchronized (sync) {
            try {
                ByteArrayOutputStream bas = new ByteArrayOutputStream();
                DataOutputStream das = new DataOutputStream(bas);
                // First, write a version to allow us doing updates in the future.
                das.writeByte(SERIALIZATION_VERSION);

                das.writeInt(events.size());
                for (Event event : events) {
                    final EventLabel label = event.getLabel() == null ? EventLabel.UNKNOWN : event.getLabel();
                    das.writeLong(event.getTimestamp());
                    das.writeByte(label.ordinal());
                    // Legacy -> We used to write labelStrings only for OTHER, but to be compatible with older back-up addon we need to store it for everything.
                    das.writeBoolean(true);
                    String labelString = event.getLabelString() != null ? event.getLabelString() : label.name();
                    das.writeUTF(labelString);
                    das.writeFloat(event.getValue());
                }

                das.flush();
                return bas.toByteArray();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void clearAll() {
        synchronized (sync) {
            events.clear();
        }
    }

    public void clearLabels(EventLabel ... labels) {
        synchronized (sync) {
            for (Iterator<Event> iterator = events.iterator(); iterator.hasNext(); ) {
                Event event = iterator.next();
                for (EventLabel label : labels) {
                    EventLabel currentLabel = event.getLabel();
                    if (label == currentLabel) {
                        iterator.remove();
                        break;
                    }
                }
            }
        }
    }


    public void clearLabels(long from, long to, EventLabel ... labels) {
        synchronized (sync) {
            for (Iterator<Event> iterator = events.iterator(); iterator.hasNext(); ) {
                Event event = iterator.next();
                for (EventLabel label : labels) {
                    EventLabel currentLabel = event.getLabel();
                    if (label == currentLabel) {
                        if ((event.getTimestamp() >= from) && (event.getTimestamp() <= to)) {
                            if (label == EventLabel.HR) {
                                Logger.logInfo("Delete HR event: " + event);
                            }
                            iterator.remove();
                            break;
                        }
                    }
                }
            }
        }
    }

    public boolean hasLabel(EventLabel ... labels) {
        synchronized (sync) {
            for (Event event : events) {
                for (EventLabel label : labels) {
                    if (label == event.getLabel()) {
                        return true;
                    }
                }
            }
            return false;
        }
    }



    public boolean hasLabel(long from, long to, EventLabel ... labels) {
        synchronized (sync) {
            for (Event event : events) {
                for (EventLabel label : labels) {
                    if (label == event.getLabel()) {
                        if ((event.getTimestamp() >= from) && (event.getTimestamp() <= to)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    public boolean hasLabel(EventLabel label, long timestamp, float value) {
        synchronized (sync) {
            for (Event event : events) {
                if (event.getLabel() == label && event.getTimestamp() == timestamp && event.getValue() == value)
                    return true;
            }
            return false;
        }
    }

    public boolean hasLabel(EventLabel label) {
        synchronized (sync) {
            for (Event event : events) {
                if (label == event.getLabel()) {
                    return true;
                }
            }
            return false;
        }
    }

    public String toString() {
        synchronized (sync) {
            StringBuilder builder = new StringBuilder();
            for (Event event : events) {
                final EventLabel label = event.getLabel() == null ? EventLabel.UNKNOWN : event.getLabel();
                builder .append(label)
                        .append(":")
                        .append(new Date(event.getTimestamp()))
                        .append(label.hasValue() ? ":" + event.getValue() : "")
                        .append(";");
            }
            return builder.toString();
        }
    }

    public String toStringDeterministic() {
        synchronized (sync) {
            StringBuilder builder = new StringBuilder();
            for (Event event : events) {
                final EventLabel label = event.getLabel() == null ? EventLabel.UNKNOWN : event.getLabel();
                builder .append(label)
                        .append(":")
                        .append(event.getTimestamp())
                        .append(label.hasValue() ? ":" + event.getValue() : "")
                        .append(";");
            }
            return builder.toString();
        }
    }
}
