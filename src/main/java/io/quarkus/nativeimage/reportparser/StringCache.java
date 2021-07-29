package io.quarkus.nativeimage.reportparser;

import java.util.HashMap;
import java.util.Map;

public class StringCache {

    final Map<String, String> cache = new HashMap<>();

    public String intern(String s) {
        if (cache.containsKey(s)) {
            return cache.get(s);
        }
        cache.put(s, s);
        return s;
    }
}
