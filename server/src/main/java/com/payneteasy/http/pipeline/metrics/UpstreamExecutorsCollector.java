package com.payneteasy.http.pipeline.metrics;

import com.payneteasy.http.pipeline.upstream.UpstreamExecutors;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

import java.util.List;

import static java.util.Collections.singletonList;

public class UpstreamExecutorsCollector extends Collector {

    private final UpstreamExecutors upstreamExecutors;

    public UpstreamExecutorsCollector(UpstreamExecutors upstreamExecutors) {
        this.upstreamExecutors = upstreamExecutors;
    }

    @Override
    public List<MetricFamilySamples> collect() {
        GaugeMetricFamily metricFamily = new GaugeMetricFamily("upstream-executor", "help", getName("unit"));
        metricFamily.addMetric(getName("active-executors"), upstreamExecutors.getActiveExecutorsCount());
        metricFamily.addMetric(getName("max-executors"   ), upstreamExecutors.getMaxExecutorsCount());
        return singletonList(metricFamily);
    }

    private List<String> getName(String aName) {
        return singletonList(aName);
    }

}
