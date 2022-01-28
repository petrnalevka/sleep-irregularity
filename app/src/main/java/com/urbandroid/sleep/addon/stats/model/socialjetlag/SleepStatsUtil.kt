package com.urbandroid.sleep.addon.stats.model.socialjetlag

import com.urbandroid.sleep.addon.stats.model.StatRecord

//Skip too short or too long sleeps.
fun filterByGrossLength(records: Collection<StatRecord>, minLen: Double, maxLen: Double)
        = records.filter { it.trackLengthInHours in minLen..maxLen }.toList()

