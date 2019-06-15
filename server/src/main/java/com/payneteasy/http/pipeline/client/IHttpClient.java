package com.payneteasy.http.pipeline.client;

import java.io.IOException;

public interface IHttpClient {

    HttpResponse sendPost(HttpRequest aRequest) throws IOException;

}
