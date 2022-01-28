package com.payneteasy.http.pipeline.upstream;

import com.payneteasy.http.pipeline.client.HttpRequest;
import com.payneteasy.http.pipeline.client.HttpResponse;
import com.payneteasy.http.pipeline.client.IHttpClient;

import java.util.concurrent.Callable;

public class UpstreamTask implements Callable<HttpResponse> {

    private final HttpRequest request;
    private final String      id;
    private final IHttpClient httpClient;
    private final String      user;

    public UpstreamTask(String id, HttpRequest httpRequest, IHttpClient aHttpClient, String user) {
        this.request = httpRequest;
        this.id = id;
        httpClient = aHttpClient;
        this.user = user;
    }

    @Override
    public HttpResponse call() throws Exception {
        Thread.currentThread().setName(id);
        return httpClient.sendPost(request);
    }

    public HttpRequest getHttpRequest() {
        return request;
    }

    public String getUser() {
        return user;
    }
}
