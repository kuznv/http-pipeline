package com.payneteasy.http.pipeline.metrics;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import static java.util.Collections.singletonList;

public class BlockingQueueCollector extends Collector {

    private final String        name;
    private final String        help;
    private final BlockingQueue target;

    public BlockingQueueCollector(String name, String help, BlockingQueue target) {
        this.name = name;
        this.help = help;
        this.target = target;
    }

    @Override
    public List<MetricFamilySamples> collect() {
        GaugeMetricFamily metricFamily = new GaugeMetricFamily(name, help, singletonList("unit"));
        metricFamily.addMetric(singletonList("size"), target.size());
        metricFamily.addMetric(singletonList("remainingCapacity"), target.remainingCapacity());
        return singletonList(metricFamily);
    }

}
