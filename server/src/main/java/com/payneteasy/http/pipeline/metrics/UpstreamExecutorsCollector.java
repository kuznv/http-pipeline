package com.payneteasy.http.pipeline.metrics;

import com.payneteasy.http.pipeline.upstream.UpstreamExecutor;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

import java.util.List;

import static java.util.Collections.singletonList;

public class UpstreamExecutorsCollector extends Collector {

    private final UpstreamExecutor executor;

    public UpstreamExecutorsCollector(UpstreamExecutor executor) {
        this.executor = executor;
    }

    @Override
    public List<MetricFamilySamples> collect() {
        GaugeMetricFamily metricFamily = new GaugeMetricFamily("upstream-executor", "help", getName("unit"));
        metricFamily.addMetric(getName("ActiveCount"             ), executor.getExecutor().getActiveCount());
        metricFamily.addMetric(getName("CompletedTaskCount"      ), executor.getExecutor().getCompletedTaskCount());
        metricFamily.addMetric(getName("CorePoolSize"            ), executor.getExecutor().getCorePoolSize());
        metricFamily.addMetric(getName("LargestPoolSize"         ), executor.getExecutor().getLargestPoolSize());
        metricFamily.addMetric(getName("MaximumPoolSize"         ), executor.getExecutor().getMaximumPoolSize());
        metricFamily.addMetric(getName("PoolSize"                ), executor.getExecutor().getPoolSize());
        metricFamily.addMetric(getName("TaskCount"               ), executor.getExecutor().getTaskCount());
        metricFamily.addMetric(getName("ActiveCount"             ), executor.getExecutor().getActiveCount());
        metricFamily.addMetric(getName("queue_size"              ), executor.getQueueSize());

        return singletonList(metricFamily);
    }

    private List<String> getName(String aName) {
        return singletonList(aName);
    }

}
