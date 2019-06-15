package com.payneteasy.http.pipeline.client;

import com.payneteasy.http.pipeline.cache.CacheKey;

import java.util.Map;

public class HttpRequest {

    private final String              url;
    private final     Map<String, String> headers;
    private final     byte[]              body;
    private final     int                 connectionTimeout;
    private final     int                 readTimeout;
    private final CacheKey                cacheKey;

    public HttpRequest(String url, Map<String, String> headers, byte[] body, int connectionTimeout, int readTimeout, CacheKey aCacheKey) {
        this.url = url;
        this.headers = headers;
        this.body = body;
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
        cacheKey = aCacheKey;
    }

    public String getUrl() {
        return url;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public CacheKey getCacheKey() {
        return cacheKey;
    }
}
