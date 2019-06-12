package com.payneteasy.http.pipeline.client;

import com.payneteasy.http.pipeline.util.InputStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class HttpClient {

    private static final Logger LOG = LoggerFactory.getLogger(HttpClient.class);


    public HttpResponse sendPost(HttpRequest aRequest) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(aRequest.getUrl()).openConnection();
        connection.setConnectTimeout(aRequest.getConnectionTimeout());
        connection.setReadTimeout(aRequest.getReadTimeout());

//        LOG.debug("Connecting to {} ...", aRequest.getUrl());
//        connection.connect();

        for (Map.Entry<String, String> entry : aRequest.getHeaders().entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }



        connection.setDoOutput(true);

        try {
            LOG.debug("Connecting to {}", aRequest.getUrl());
            connection.connect();
        } catch (IOException e) {
            return new HttpResponse(502, InputStreams.readAll(connection.getErrorStream()), "Upstream unavailable: " + e.getMessage());
        }

        LOG.debug("Writing to {}", aRequest.getUrl());
        connection.getOutputStream().write(aRequest.getBody());


        try {
            InputStream inputStream  = connection.getInputStream();
            int         responseCode = connection.getResponseCode();
            
            LOG.debug("Response code is {} and content length is {}", responseCode, connection.getContentLength());
            byte[]      body        = InputStreams.readFully(inputStream, connection.getContentLength());

            return new HttpResponse(responseCode, body, null);
        } catch (FileNotFoundException e) {
            LOG.error("Not found {} {}", aRequest.getUrl(), connection.getErrorStream(), e);
            return new HttpResponse(404, InputStreams.readAll(connection.getErrorStream()), "Not Found in Upstream");

        } catch (Exception e) {
            LOG.error("Cannot execute POST", e);
            return new HttpResponse(connection.getResponseCode(), InputStreams.readAll(connection.getErrorStream()), e.getMessage());
        }
    }
}
