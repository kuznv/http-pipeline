package com.payneteasy.http.pipeline.client;

import com.payneteasy.http.pipeline.cache.CacheKey;
import com.payneteasy.http.pipeline.cache.ICacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class HttpClientWithCache implements IHttpClient {

    private static final Logger LOG = LoggerFactory.getLogger(HttpClientWithCache.class);

    private final IHttpClient   httpClient;
    private final ICacheManager cacheManager;
    private final int           cacheMaximumBody;

    public HttpClientWithCache(IHttpClient httpClient, ICacheManager cacheManager, int aCacheMaximumBody) {
        this.httpClient = httpClient;
        this.cacheManager = cacheManager;
        cacheMaximumBody = aCacheMaximumBody;
    }

    @Override
    public HttpResponse sendPost(HttpRequest aRequest) throws IOException {
        CacheKey cacheKey = aRequest.getCacheKey();
        if(cacheKey.hasError()) {
            return httpClient.sendPost(aRequest);
        }

        HttpResponse cachedResponse = cacheManager.getResponse(cacheKey);
        if(cachedResponse != null) {
            return cachedResponse;
        } else {
            return makeCallAndPutToCache(aRequest);
        }
    }

    private HttpResponse makeCallAndPutToCache(HttpRequest aRequest) throws IOException {
        HttpResponse httpResponse = httpClient.sendPost(aRequest);

        if(httpResponse.getStatus() != 200) {
            return httpResponse;
        }

        if(httpResponse.getResponseBody() != null && httpResponse.getResponseBody().length < cacheMaximumBody) {
            cacheManager.putResponse(aRequest.getCacheKey(), httpResponse);
        } else {
            LOG.warn("Body size is more than {}", cacheMaximumBody);
        }
        return httpResponse;
    }

}
