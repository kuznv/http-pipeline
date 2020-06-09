package com.payneteasy.http.pipeline.proxy;

import com.payneteasy.http.pipeline.util.InputStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpstreamHttpClient {
    private static final Logger LOG = LoggerFactory.getLogger(UpstreamHttpClient.class);

    public ProxyResponse executeRequest(String aBaseUrl, ProxyRequest aRequest) throws IOException {
        String url = aBaseUrl + aRequest.getFullPath();

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(30_000);
        connection.setReadTimeout(30_000);

        LOG.debug("Connecting to {} ...", url);
        connection.setRequestMethod(aRequest.getMethod());
        aRequest.getHeaders().writeToConnection(connection);

        writeBody(connection, aRequest);

        return readResponse(url, connection);

    }

    private ProxyResponse readResponse(String aUrl, HttpURLConnection aConnection) {
        try {
            int         responseCode = aConnection.getResponseCode();
            InputStream inputStream  = responseCode >= 400 ? aConnection.getErrorStream() : aConnection.getInputStream();

            LOG.debug("Response code is {} and content length is {}", responseCode, aConnection.getContentLength());
            byte[] body = InputStreams.readAll(inputStream);

            return new ProxyResponse(responseCode, HttpHeaders.createFromConnection(aConnection), body);
        } catch (FileNotFoundException e) {
            LOG.error("Not found {}", aUrl, e);
            return new ProxyResponse(404, HttpHeaders.createEmpty(), new byte[0]);

        } catch (Exception e) {
            LOG.error("Cannot execute POST", e);
            return new ProxyResponse(503, HttpHeaders.createEmpty(), new byte[0]);
        }
    }

    private void writeBody(HttpURLConnection aConnection, ProxyRequest aRequest) throws IOException {
        if (aRequest.getBody() == null || aRequest.getBody().length == 0) {
            return;
        }

        aConnection.setDoOutput(true);
        aConnection.connect();
        aConnection.getOutputStream().write(aRequest.getBody());
    }
}
