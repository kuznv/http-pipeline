package com.payneteasy.http.pipeline;

import com.payneteasy.startup.parameters.AStartupParameter;

public interface IStartupConfig {

    @AStartupParameter(name = "WEB_SERVER_PORT", value = "8083")
    int webServerPort();

    @AStartupParameter(name = "WEB_SERVER_CONTEXT", value = "/api")
    String webServerContext();

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

    @AStartupParameter(name = "METRICS_SERVLET_PATH", value = "/metrics")
    String getMetricsServletPath();
}
