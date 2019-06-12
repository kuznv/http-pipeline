package com.payneteasy.http.pipeline.upstream;

import com.payneteasy.http.pipeline.client.HttpClient;
import com.payneteasy.http.pipeline.client.HttpRequest;
import com.payneteasy.http.pipeline.client.HttpResponse;

import java.util.concurrent.Callable;

public class UpstreamTask implements Callable<HttpResponse> {

    private final HttpRequest request;
    private final String      id;

    public UpstreamTask(String id, HttpRequest httpRequest) {
        this.request = httpRequest;
        this.id = id;

    }

    @Override
    public HttpResponse call() throws Exception {
        Thread.currentThread().setName(id);
        HttpClient client = new HttpClient();
        return client.sendPost(request);
    }

    public HttpRequest getHttpRequest() {
        return request;
    }
}
