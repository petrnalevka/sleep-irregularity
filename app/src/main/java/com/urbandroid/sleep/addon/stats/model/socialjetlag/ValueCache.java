package com.urbandroid.sleep.addon.stats.model.socialjetlag;

import java.util.HashMap;
import java.util.Map;

public class ValueCache {

    private final Map<String,Object> cache = new HashMap<>();

    public void put(String key, Object value) {
        cache.put(key, value);
    }

    public <T> T computeIfAbsent(String key, Supplier valueSupplier) {
        if (!cache.containsKey(key)) {
            cache.put(key, valueSupplier.get());
        }
        return (T) cache.get(key);
    }

    public <T> T get(String key) {
        return (T) cache.get(key);
    }

    public interface Supplier {
        Object get();
    }
}
