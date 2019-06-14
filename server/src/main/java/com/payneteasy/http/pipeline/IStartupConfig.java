package com.payneteasy.http.pipeline;

import com.payneteasy.startup.parameters.AStartupParameter;

import java.io.File;

public interface IStartupConfig {

    @AStartupParameter(name = "WEB_SERVER_PORT", value = "8083")
    int webServerPort();

    @AStartupParameter(name = "MANAGEMENT_SERVER_PORT", value = "8084")
    int managementServerPort();

    @AStartupParameter(name = "WEB_SERVER_CONTEXT", value = "/")
    String webServerContext();

    @AStartupParameter(name = "MANAGEMENT_SERVER_CONTEXT", value = "/")
    String managementServerContext();

    @AStartupParameter(name = "UPSTREAM_BASE_URL", value = "http://localhost:8081")
    String getUpstreamBaseUrl();

    @AStartupParameter(name = "UPSTREAM_MAX_CONNECTIONS", value = "20")
    int getUpstreamMaxConnections();

    @AStartupParameter(name = "UPSTREAM_ACTIVE_CONNECTIONS", value = "10")
    int getUpstreamActiveConnections();

    @AStartupParameter(name = "UPSTREAM_CONNECT_TIMEOUT_MS", value = "60000")
    int getUpstreamConnectTimeoutMs();

    @AStartupParameter(name = "UPSTREAM_READ_TIMEOUT_MS", value = "60000")
    int getUpstreamReadTimeoutMs();

    @AStartupParameter(name = "UPSTREAM_WAIT_TIMEOUT_MS", value = "125000")
    int getUpstreamWaitTimeoutMs();

    @AStartupParameter(name = "UPSTREAM_QUEUE_SIZE", value = "1024")
    int getUpstreamQueueSize();

    @AStartupParameter(name = "PIPELINE_SERVLET_PATH", value = "/*")
    String getPipelineServletPath();

    @AStartupParameter(name = "MANAGEMENT_METRICS_PATH", value = "/metrics")
    String getMetricsServletPath();

    /**
     * Jetty max threads. The value could be from 50 to 500 as of https://www.eclipse.org/jetty/documentation/current/high-load.html
     *
     * @return max threads
     */
    @AStartupParameter(name = "JETTY_MAX_THREADS", value = "500")
    int getJettyMaxThreads();

    @AStartupParameter(name = "JETTY_MIN_THREADS", value = "16")
    int getJettyMinThreads();

    @AStartupParameter(name = "JETTY_POOL_IDLE_TIMEOUT_MS", value = "60000")
    int getJettyIdleTimeoutMs();

    @AStartupParameter(name = "UPSTREAM_REQUESTS_BUCKETS", value = "0.005, 0.01, 0.025, 0.05, 0.075, 0.1, 0.25, 0.5, 0.75, 1, 2.5, 5, 7.5, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 140, 180, 240, 300, 400, 600, 700, 800")
    String getUpstreamRequestsBuckets();

    @AStartupParameter(name = "ERROR_DIR", value = "/var/log/pipeline/errors")
    File getErrorDir();
}
