package com.payneteasy.http.pipeline.cache;

import com.payneteasy.http.pipeline.client.HttpResponse;

public interface ICacheManager {

    void putResponse(CacheKey aKey, HttpResponse aResponse);

    HttpResponse getResponse(CacheKey aKey);

}
