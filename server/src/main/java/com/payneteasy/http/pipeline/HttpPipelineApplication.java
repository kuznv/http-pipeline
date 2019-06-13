package com.payneteasy.http.pipeline;

import com.payneteasy.http.pipeline.metrics.ThreadPoolExecutorCollector;
import com.payneteasy.http.pipeline.servlet.PipelineServlet;
import com.payneteasy.http.pipeline.servlet.VersionServlet;
import com.payneteasy.http.pipeline.upstream.UpstreamExecutor;
import com.payneteasy.http.pipeline.upstream.UpstreamExecutors;
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
import java.util.EnumSet;

public class HttpPipelineApplication {

    private static final Logger LOG = LoggerFactory.getLogger(HttpPipelineApplication.class);

    public static void main(String[] args) {
        try {
            long startupTime = System.currentTimeMillis();

            IStartupConfig startupConfig = StartupParametersFactory.getStartupParameters(IStartupConfig.class);

            {
                Server jetty = createJettyServer(startupConfig);
                jetty.start();
                jetty.setStopAtShutdown(true);
            }

            {
                Server managementServer = createManagementServer(startupConfig);
                managementServer.start();
                managementServer.setStopAtShutdown(true);
            }

            LOG.info("Servers started time is {}ms", System.currentTimeMillis() - startupTime);
        } catch (Exception e) {
            LOG.error("Cannot start server", e);
            System.exit(-1);
        }
    }

    private static Server createManagementServer(IStartupConfig aStartupConfig) {
        Server                jetty   = new Server(aStartupConfig.managementServerPort());
        ServletContextHandler context = new ServletContextHandler(jetty, aStartupConfig.managementServerContext(), ServletContextHandler.SESSIONS);

        DefaultExports.initialize();

        context.addServlet(new ServletHolder(new VersionServlet()), "/version/*");
        context.addServlet(new ServletHolder(new MetricsServlet()), aStartupConfig.getMetricsServletPath());

        return jetty;
    }

    private static Server createJettyServer(IStartupConfig aConfig) {
        QueuedThreadPool threadPool = new QueuedThreadPool(
                  aConfig.getJettyMaxThreads()
                , aConfig.getJettyMinThreads()
                , aConfig.getJettyIdleTimeoutMs()
        );

        new QueuedThreadPoolStatisticsCollector(threadPool, "server_thread_pool").register();

        Server jetty = new Server(threadPool);

        ServerConnector connector=new ServerConnector(jetty);

        connector.setPort(aConfig.webServerPort());
        jetty.setConnectors(new Connector[]{connector});


        ServletContextHandler context = new ServletContextHandler(jetty, aConfig.webServerContext(), ServletContextHandler.SESSIONS);

        registerJettyMetrics(jetty);

        UpstreamExecutors executors = new UpstreamExecutors(aConfig.getUpstreamMaxConnections(), aConfig.getUpstreamQueueSize());
        registerExecutorsMetrics(executors);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> executors.stop()));

        PipelineServlet pipelineServlet = new PipelineServlet(
                aConfig.getUpstreamBaseUrl()
                , executors
                , aConfig.getUpstreamConnectTimeoutMs()
                , aConfig.getUpstreamReadTimeoutMs()
                , aConfig.getUpstreamWaitTimeoutMs()
        );

        MetricsFilter filter = new MetricsFilter("requests"
                , "The time taken fulfilling servlet requests"
                , -1
                , getUpstreamRequestsBuckets(aConfig.getUpstreamRequestsBuckets())
        );
        context.addFilter(new FilterHolder(filter), "/*", EnumSet.of(DispatcherType.REQUEST));
        context.addServlet(new ServletHolder(pipelineServlet), aConfig.getPipelineServletPath());
        return jetty;
    }

    private static double[] getUpstreamRequestsBuckets(String aText) {
        String[] bucketParams = aText.split(",");
        double[] buckets = new double[bucketParams.length];

        for (int i = 0; i < bucketParams.length; i++) {
            buckets[i] = Double.parseDouble(bucketParams[i]);
        }

        return buckets;
    }

    private static void registerExecutorsMetrics(UpstreamExecutors executors) {
        int i = 0;
        for (UpstreamExecutor upstreamExecutor : executors.geExecutors()) {
            new ThreadPoolExecutorCollector("executor_" + (i++), "help", upstreamExecutor).register();
        }
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
