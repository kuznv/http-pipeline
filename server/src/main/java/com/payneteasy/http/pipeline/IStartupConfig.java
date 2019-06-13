package com.payneteasy.http.pipeline;

import com.payneteasy.startup.parameters.AStartupParameter;

public interface IStartupConfig {

    @AStartupParameter(name = "WEB_SERVER_PORT", value = "8083")
    int webServerPort();

    @AStartupParameter(name = "MANAGEMENT_SERVER_PORT", value = "8084")
    int managementServerPort();

    @AStartupParameter(name = "WEB_SERVER_CONTEXT", value = "/")
    String webServerContext();

    @AStartupParameter(name = "MANAGEMENT_SERVER_CONTEXT", value = "/")
    String managementServerContext();

    @AStartupParameter(name = "UPSTREAM_BASE_URL", value = "http://localhost:8084")
    String getUpstreamBaseUrl();

    @AStartupParameter(name = "UPSTREAM_MAX_CONNECTIONS", value = "10")
    int getUpstreamMaxConnections();

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
}
