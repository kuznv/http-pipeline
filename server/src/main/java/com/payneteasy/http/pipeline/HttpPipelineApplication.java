package com.payneteasy.http.pipeline;

import com.payneteasy.http.pipeline.cache.CacheManagerMemory;
import com.payneteasy.http.pipeline.cache.ICacheKeyFactory;
import com.payneteasy.http.pipeline.cache.ICacheManager;
import com.payneteasy.http.pipeline.cache.PathQueryBodyKeyFactory;
import com.payneteasy.http.pipeline.client.HttpClient;
import com.payneteasy.http.pipeline.client.HttpClientWithCache;
import com.payneteasy.http.pipeline.client.IHttpClient;
import com.payneteasy.http.pipeline.metrics.ThreadPoolExecutorCollector;
import com.payneteasy.http.pipeline.metrics.UpstreamExecutorsCollector;
import com.payneteasy.http.pipeline.proxy.ProxyServlet;
import com.payneteasy.http.pipeline.servlet.ChangeQueueTimeoutServlet;
import com.payneteasy.http.pipeline.servlet.ChangeWriteHttpBodyServlet;
import com.payneteasy.http.pipeline.servlet.PipelineServlet;
import com.payneteasy.http.pipeline.servlet.VersionServlet;
import com.payneteasy.http.pipeline.upstream.UpstreamExecutor;
import com.payneteasy.startup.parameters.StartupParametersFactory;
import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.filter.MetricsFilter;
import io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.client.jetty.JettyStatisticsCollector;
import io.prometheus.client.jetty.QueuedThreadPoolStatisticsCollector;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpPipelineApplication {

    private static final Logger LOG = LoggerFactory.getLogger(HttpPipelineApplication.class);

    public static void main(String[] args) {
        try {
            long startupTime = System.currentTimeMillis();

            IStartupConfig startupConfig = StartupParametersFactory.getStartupParameters(IStartupConfig.class);

            AtomicInteger queueWaitTimeoutMs = new AtomicInteger(startupConfig.getUpstreamWaitTimeoutMs());
            AtomicBoolean enableHttpBodyLog = new AtomicBoolean(false);


            UpstreamExecutor executor = new UpstreamExecutor(
                    startupConfig.getUpstreamActiveConnectionsPerUser(),
                    startupConfig.getUpstreamActiveConnections()
            );
            registerExecutorsMetrics(executor);
            Runtime.getRuntime().addShutdownHook(new Thread(executor::stop));

            {
                Server jetty = createJettyServer(startupConfig, executor, queueWaitTimeoutMs, enableHttpBodyLog);
                jetty.start();
                jetty.setStopAtShutdown(true);
            }

            {
                Server managementServer = createManagementServer(startupConfig, executor, queueWaitTimeoutMs, enableHttpBodyLog);
                managementServer.start();
                managementServer.setStopAtShutdown(true);
            }

            LOG.info("Servers started time is {}ms", System.currentTimeMillis() - startupTime);
        } catch (Exception e) {
            LOG.error("Cannot start server", e);
            System.exit(-1);
        }
    }

    private static Server createManagementServer(IStartupConfig aStartupConfig, UpstreamExecutor executor, AtomicInteger aQueueWaitTimeout, AtomicBoolean enableHttpBodyLog) {
        Server                jetty   = new Server(aStartupConfig.managementServerPort());
        ServletContextHandler context = new ServletContextHandler(jetty, aStartupConfig.managementServerContext(), ServletContextHandler.SESSIONS);

        DefaultExports.initialize();

        context.addServlet(new ServletHolder(new VersionServlet()), "/version/*");
        context.addServlet(new ServletHolder(new MetricsServlet()), aStartupConfig.getMetricsServletPath());
        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.getWriter().println(executor.getActiveTasksCount());
            }
        }), "/active-executors/*");

        context.addServlet(new ServletHolder(new ChangeQueueTimeoutServlet(aQueueWaitTimeout)), "/queue-wait-timeout/*");
        context.addServlet(new ServletHolder(new ChangeWriteHttpBodyServlet(enableHttpBodyLog)), "/write-http-body/*");


        return jetty;
    }

    private static Server createJettyServer(IStartupConfig aConfig, UpstreamExecutor executor, AtomicInteger aQeueuWaitTimeoutMs, AtomicBoolean enableHttpBodyLog) {
        QueuedThreadPool threadPool = new QueuedThreadPool(
                  aConfig.getJettyMaxThreads()
                , aConfig.getJettyMinThreads()
                , aConfig.getJettyIdleTimeoutMs()
        );
        threadPool.setName("server_thread_pool");

        new QueuedThreadPoolStatisticsCollector(threadPool, "server_thread_pool").register();

        Server jetty = new Server(threadPool);

        ServerConnector connector = new ServerConnector(jetty);

        connector.setPort(aConfig.webServerPort());
        jetty.setConnectors(new Connector[]{connector});


        ServletContextHandler context = new ServletContextHandler(jetty, aConfig.webServerContext(), ServletContextHandler.SESSIONS);

        registerJettyMetrics(jetty);

        ThreadPoolExecutor logExecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(aConfig.getUpstreamQueueSize()));
        registerLogExecutorMetrics(logExecutor);

        ICacheKeyFactory cacheKeyFactory = new PathQueryBodyKeyFactory();
        ICacheManager    cacheManager    = new CacheManagerMemory(aConfig.getCacheMemoryMaximumSize(), aConfig.getCacheMemoryTtlMs());
        IHttpClient      httpClient      = new HttpClientWithCache(new HttpClient(), cacheManager, aConfig.getCacheMaximumBody());

        PipelineServlet pipelineServlet = new PipelineServlet(
                aConfig.getUpstreamBaseUrl()
                , executor
                , logExecutor
                , aConfig.getUpstreamConnectTimeoutMs()
                , aConfig.getUpstreamReadTimeoutMs()
                , aQeueuWaitTimeoutMs
                , aConfig.getErrorDir()
                , enableHttpBodyLog
                , cacheKeyFactory
                , cacheManager
                , httpClient
        );

        MetricsFilter filter = new MetricsFilter("requests"
                , "The time taken fulfilling servlet requests"
                , -1
                , getUpstreamRequestsBuckets(aConfig.getUpstreamRequestsBuckets())
        );

        ProxyServlet proxyServlet = new ProxyServlet(aConfig.getProxyLogDir(), aConfig.getUpstreamBaseUrl(), toList(aConfig.getProxyUpstreamHeaders()));

        context.addFilter(new FilterHolder(filter), "/*", EnumSet.of(DispatcherType.REQUEST));
        context.addServlet(new ServletHolder(pipelineServlet), aConfig.getPipelineServletPath());
        context.addServlet(new ServletHolder(proxyServlet)    , aConfig.getProxyServletPath());
        return jetty;
    }

    private static List<String> toList(String aLine) {
        List<String> ret = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(aLine, " \t,;");
        while(st.hasMoreTokens()) {
            ret.add(st.nextToken());
        }
        return ret;
    }

    private static double[] getUpstreamRequestsBuckets(String aText) {
        String[] bucketParams = aText.split(",");
        double[] buckets = new double[bucketParams.length];

        for (int i = 0; i < bucketParams.length; i++) {
            buckets[i] = Double.parseDouble(bucketParams[i]);
        }

        return buckets;
    }

    private static void registerExecutorsMetrics(UpstreamExecutor executor) {
        new UpstreamExecutorsCollector(executor).register();
    }

    private static void registerLogExecutorMetrics(ThreadPoolExecutor executor) {
        new ThreadPoolExecutorCollector("log_executor", "help", executor).register();
    }

    private static void registerJettyMetrics(Server jetty) {
        // Configure StatisticsHandler.
        StatisticsHandler stats = new StatisticsHandler();
        stats.setHandler(jetty.getHandler());
        jetty.setHandler(stats);
        // Register collector.
        new JettyStatisticsCollector(stats).register();
    }
}
