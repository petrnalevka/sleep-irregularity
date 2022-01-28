package com.urbandroid.sleep.domain.interval;


import androidx.annotation.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class Interval {
    private long from;
    private long to;

    public Interval(long from, long to) {
        if (from > to) {
            this.from = to;
            this.to = from;
        } else {
            this.from = from;
            this.to = to;
        }
    }

    public Interval(Date from, Date to) {
        this(from.getTime(), to.getTime());
    }

    public long getFrom() {
        return from;
    }

    public long getTo() {
        return to;
    }



    public long getLength() {
        return to - from;
    }

    public boolean isIn(long time) {
        return time >= from && time <= to;
    }

    public Interval getIntersection(Interval i) {
        if (from >= i.from && to <= i.to) {
            return this;
        } else if (i.from >= from && i.to <= to) {
            return i;
        } if (from >= i.from && from <= i.to) {
            return new Interval(from, i.to);
        } else if (i.from >= from && i.from <= to) {
            return new Interval(i.from, to);
        }
        return null;
    }

    public boolean hasIntersection(Interval i){
        return getIntersection(i) != null;
    }

    public Interval getUnion(Interval i) {
        Interval intersection = getIntersection(i);
        if (intersection == null) {
            return null;
        }

        return new Interval(Math.min(i.getFrom(), getFrom()), Math.max(i.getTo(), to));
    }

    public long getIntersectionLength(Interval i) {
        Interval overlap = getIntersection(i);
        if (overlap != null) {
            return overlap.getLength();
        }
        return 0;
    }

    public boolean contains(@NonNull Interval interval) {
        return from <= interval.from && to >= interval.to;
    }

    public Collection<Interval> subtract(Interval other) {

        Interval intersection = getIntersection(other);

        if (intersection == null) {
            return Collections.singleton(this);
        }

        List result = new LinkedList();

        if (intersection.getTo() != getTo()) {
            result.add(new Interval(intersection.getTo(), getTo()));
        }

        if (intersection.getFrom() != getFrom()) {
            result.add(new Interval(getFrom(), intersection.getFrom()));
        }

        return result;
    }

    public static long getSum(List<Interval> intervals) {
        long sum = 0;

        if (intervals == null) {
            return sum;
        }

        for (Interval interval : intervals) {
            sum = sum + interval.getLength();
        }
        return sum;
    }

    @Override
    public String toString() {
        return "["+from+"-"+to+"]("+to+")";
    }

    public String toStringMillis() {
        return "["+from+", "+to+"]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Interval)) return false;

        Interval interval = (Interval) o;

        if (from != interval.from) return false;
        if (to != interval.to) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (from ^ (from >>> 32));
        result = 31 * result + (int) (to ^ (to >>> 32));
        return result;
    }
}