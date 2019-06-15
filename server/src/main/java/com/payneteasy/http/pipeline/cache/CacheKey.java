package com.payneteasy.http.pipeline.cache;

public class CacheKey {

    static final CacheKey ERROR = new CacheKey(null, true);

    private final String  key;
    private final boolean error;

    private CacheKey(String key, boolean aError) {
        this.key = key;
        error = aError;
    }


    CacheKey(String key) {
        this(key, false);
    }

    public String getKey() {
        return key;
    }

    public boolean hasError() {
        return error;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CacheKey cacheKey = (CacheKey) o;

        return key.equals(cacheKey.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return "CacheKey{" +
                "key='" + key + '\'' +
                '}';
    }
}
