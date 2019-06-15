package com.payneteasy.http.pipeline.upstream;

import com.payneteasy.http.pipeline.client.HttpRequest;
import com.payneteasy.http.pipeline.client.HttpResponse;
import com.payneteasy.http.pipeline.client.IHttpClient;

import java.util.concurrent.Callable;

public class UpstreamTask implements Callable<HttpResponse> {

    private final HttpRequest request;
    private final String      id;
    private final IHttpClient httpClient;

    public UpstreamTask(String id, HttpRequest httpRequest, IHttpClient aHttpClient) {
        this.request = httpRequest;
        this.id = id;
        httpClient = aHttpClient;

    }

    @Override
    public HttpResponse call() throws Exception {
        Thread.currentThread().setName(id);
        return httpClient.sendPost(request);
    }

    public HttpRequest getHttpRequest() {
        return request;
    }
}
