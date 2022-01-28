package com.urbandroid.sleep.addon.stats.model;

import java.util.Date;
import java.util.Set;

/**
 * @author petr
 */
public interface IStatRecord extends IMeasureRecord {

    Date getToDate();

    Date getFromDate();

    double getFromHour();

    double getToHour();

    Set<String> getTags();

    int getCount();
}
