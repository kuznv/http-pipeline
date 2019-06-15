package com.payneteasy.http.pipeline.servlet;

import com.payneteasy.http.pipeline.client.HttpRequest;
import com.payneteasy.http.pipeline.client.HttpResponse;
import com.payneteasy.http.pipeline.log.HttpBodyLog;
import com.payneteasy.http.pipeline.upstream.UpstreamTask;
import com.payneteasy.http.pipeline.upstream.UpstreamExecutors;
import com.payneteasy.http.pipeline.util.InputStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PipelineServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(PipelineServlet.class);

    private final AtomicLong        serial = new AtomicLong();
    private final String            upstreamBaseUrl;
    private final UpstreamExecutors executors;
    private final int               connectionTimeoutMs;
    private final int               readTimeoutMs;
    private final AtomicInteger     waitingMs;
    private final File              errorDir;
    private final AtomicBoolean     writeHttpBody;
    private final HttpBodyLog       httpBodyLog;

    public PipelineServlet(String upstreamBaseUrl
            , UpstreamExecutors aExecutors
            , int aConnectionTimeoutMs
            , int aReadTimeoutMs
            , AtomicInteger aWaitingMs
            , File aErrorDir
            , AtomicBoolean aWriteHttpBody
    ) {
        this.upstreamBaseUrl = upstreamBaseUrl;
        executors = aExecutors;
        readTimeoutMs = aReadTimeoutMs;
        connectionTimeoutMs = aConnectionTimeoutMs;
        waitingMs = aWaitingMs;
        errorDir = aErrorDir;
        writeHttpBody = aWriteHttpBody;
        httpBodyLog = new HttpBodyLog(aErrorDir);
    }

    @Override
    protected void doPost(HttpServletRequest aRequest, HttpServletResponse aResponse) throws ServletException, IOException {
        LOG.debug("Received {}?{}", aRequest.getRequestURI(), aRequest.getQueryString());

        String id = aRequest.getRemoteAddr() + "-" + aRequest.getRequestURI() + "?" + aRequest.getQueryString() + "-" + serial.incrementAndGet();
        Thread currentThread = Thread.currentThread();
        String oldThreadName = currentThread.getName();
        currentThread.setName("jetty-" + id);

        try {
            String              query       = aRequest.getQueryString();
            Map<String, String> headers     = extractHeaders(aRequest);
            String              upstreamUrl = createUpstreamUrl(upstreamBaseUrl, aRequest.getRequestURI(), query);

            HttpRequest httpRequest = new HttpRequest(upstreamUrl
                    , headers
                    , InputStreams.readFully(aRequest.getInputStream(), aRequest.getContentLength())
                    , connectionTimeoutMs
                    , readTimeoutMs);

            LOG.debug("Queuing {} ...", upstreamUrl);
            ExecutorService      executor           = executors.findExecutor(generateExecutorKey(aRequest));
            Future<HttpResponse> httpResponseFuture = executor.submit(new UpstreamTask("upstr-" + id, httpRequest));

            try {
                long startupTime = System.currentTimeMillis();
                LOG.debug("Waiting to be executed in {}ms ...", waitingMs);
                HttpResponse upstreamResponse = httpResponseFuture.get(waitingMs.get(), TimeUnit.MILLISECONDS);
                int          code       = upstreamResponse.getStatus();
                LOG.debug("Client result is {}, time is {}ms", code, System.currentTimeMillis() - startupTime);
                if(code == 200) {
                    aResponse.setStatus(code);
                    aResponse.getOutputStream().write(upstreamResponse.getResponseBody());
                    if(writeHttpBody.get()) {
                        String path = (aRequest.getRequestURI() + "-"+aRequest.getQueryString())
                                .replace('=', '-')
                                .replace('/', '-');
                        httpBodyLog.log(path, httpRequest.getBody(), upstreamResponse.getResponseBody());
                    }
                } else {
                    aResponse.setStatus(code);
                    if(upstreamResponse.getResponseBody() == null) {
                        LOG.warn("No response content from upstream");
                    } else {
                        String body = new String(upstreamResponse.getResponseBody(), StandardCharsets.UTF_8);
                        LOG.debug("Sending error body {}", body);
                        aResponse.getWriter().write(body);
                    }

                    saveError(httpRequest, upstreamResponse);
                }


            } catch (InterruptedException e) {

                currentThread.interrupt();
                LOG.warn("Interrupted", e);
                aResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);

            } catch (ExecutionException e) {

                LOG.error("Task aborted", e);
                aResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);

            } catch (TimeoutException e) {

                boolean cancelResult = httpResponseFuture.cancel(false);
                LOG.error("Timeout exception: cancel={}", cancelResult, e);
                aResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);

            }
        } finally {
            currentThread.setName(oldThreadName);
        }

    }

    private String generateExecutorKey(HttpServletRequest aRequest) {
        return aRequest.getRequestURI() + "/" + aRequest.getQueryString();
    }

    private void saveError(HttpRequest aRequest, HttpResponse aResponse) {
        try {
            try (PrintWriter out = new PrintWriter(new FileWriter(new File(errorDir, System.currentTimeMillis() + ".txt")))) {
                out.println(aRequest.getUrl());
                out.println("Request:");
                out.println(new String(aRequest.getBody(), StandardCharsets.UTF_8));
                out.println("Response:");
                out.println(aResponse.getStatus());
                out.println(aResponse.getErrorMessage());
                out.println(new String(aResponse.getResponseBody(), StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            LOG.error("Cannot save log", e);
        }
    }

    private String createUpstreamUrl(String aBaseUrl, String aRequestUri, String aQuery) {
        return aBaseUrl + aRequestUri + '?' + aQuery;
    }

    private Map<String, String> extractHeaders(HttpServletRequest aRequest) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> en = aRequest.getHeaderNames();
        while (en.hasMoreElements()) {
            String name = en.nextElement();
            headers.put(name, aRequest.getHeader(name));
        }
        return headers;
    }
}
