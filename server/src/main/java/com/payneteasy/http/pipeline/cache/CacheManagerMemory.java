package com.payneteasy.http.pipeline.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.payneteasy.http.pipeline.client.HttpResponse;
import io.prometheus.client.Gauge;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class CacheManagerMemory implements ICacheManager {

    private static final Logger LOG = LoggerFactory.getLogger(CacheManagerMemory.class);

    private final Gauge allGauge  = Gauge.build("cache_all", "All gets").create().register();
    private final Gauge hitsGauge = Gauge.build("cache_hits", "Hits").create().register();

    private final Cache<CacheKey, HttpResponse> cache;

    public CacheManagerMemory(int aMaximumSize, int aTimeToLiveMs) {
         cache = CacheBuilder.newBuilder()
                .maximumSize(aMaximumSize)
                .expireAfterWrite(aTimeToLiveMs, TimeUnit.MILLISECONDS)
                .removalListener((RemovalListener<CacheKey, HttpResponse>) aNotification -> LOG.debug("Removed {}", aNotification.getKey()))
                .build();
    }

    @Override
    public void putResponse(CacheKey aKey, HttpResponse aResponse) {
        cache.put(aKey, aResponse);
    }

    @Override
    public HttpResponse getResponse(CacheKey aKey) {
        @Nullable HttpResponse ret = cache.getIfPresent(aKey);
        allGauge.inc();
        if(ret != null) {
            hitsGauge.inc();
        }
        return ret;
    }
}
