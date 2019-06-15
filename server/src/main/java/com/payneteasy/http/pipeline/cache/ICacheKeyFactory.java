package com.payneteasy.http.pipeline.cache;

public interface ICacheKeyFactory {

    CacheKey createKey(String aPath, String aQuery, byte[] aBody);

}
