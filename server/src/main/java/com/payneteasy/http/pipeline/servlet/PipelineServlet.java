package com.payneteasy.http.pipeline.servlet;

import com.payneteasy.http.pipeline.client.HttpRequest;
import com.payneteasy.http.pipeline.client.HttpResponse;
import com.payneteasy.http.pipeline.upstream.UpstreamTask;
import com.payneteasy.http.pipeline.upstream.UpstreamExecutors;
import com.payneteasy.http.pipeline.util.InputStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class PipelineServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(PipelineServlet.class);

    private final AtomicLong        serial = new AtomicLong();
    private final String            upstreamBaseUrl;
    private final UpstreamExecutors executors;
    private final int               connectionTimeoutMs;
    private final int               readTimeoutMs;
    private final int               waitingMs;

    public PipelineServlet(String upstreamBaseUrl
            , UpstreamExecutors aExecutors
            , int aConnectionTimeoutMs
            , int aReadTimeoutMs
            , int aWaitingMs
    ) {
        this.upstreamBaseUrl = upstreamBaseUrl;
        executors = aExecutors;
        readTimeoutMs = aReadTimeoutMs;
        connectionTimeoutMs = aConnectionTimeoutMs;
        waitingMs = aWaitingMs;
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
            ExecutorService      executor           = executors.findExecutor(aRequest.getRequestURI() + "/" + aRequest.getQueryString());
            Future<HttpResponse> httpResponseFuture = executor.submit(new UpstreamTask("upstr-" + id, httpRequest));

            try {
                long startupTime = System.currentTimeMillis();
                LOG.debug("Waiting to be executed in {}ms ...", waitingMs);
                HttpResponse upstreamResponse = httpResponseFuture.get(waitingMs, TimeUnit.MILLISECONDS);
                int          code       = upstreamResponse.getStatus();
                LOG.debug("Client result is {}, time is {}ms", code, System.currentTimeMillis() - startupTime);
                if(code == 200) {
                    aResponse.setStatus(code);
                    aResponse.getOutputStream().write(upstreamResponse.getResponseBody());
                } else {
                    aResponse.setStatus(code);
                    if(upstreamResponse.getResponseBody() == null) {
                        LOG.warn("No response content from upstream");
                    } else {
                        String body = new String(upstreamResponse.getResponseBody());
                        LOG.debug("Sending error body {}", body);
                        aResponse.getWriter().write(body);
                    }

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
