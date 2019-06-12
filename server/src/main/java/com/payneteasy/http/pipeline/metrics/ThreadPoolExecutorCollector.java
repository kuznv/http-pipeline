package com.payneteasy.http.pipeline.metrics;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import static java.util.Collections.singletonList;

public class ThreadPoolExecutorCollector extends Collector {

    private final String             name;
    private final String             help;
    private final ThreadPoolExecutor target;

    public ThreadPoolExecutorCollector(String name, String help, ThreadPoolExecutor target) {
        this.name = name;
        this.help = help;
        this.target = target;
    }

    @Override
    public List<MetricFamilySamples> collect() {
        GaugeMetricFamily metricFamily = new GaugeMetricFamily(name, help, getName("unit"));
        metricFamily.addMetric(getName("ActiveCount"          ), target.getActiveCount());
        metricFamily.addMetric(getName("CompletedTaskCount"   ), target.getCompletedTaskCount());
        metricFamily.addMetric(getName("CorePoolSize"         ), target.getCorePoolSize());
        metricFamily.addMetric(getName("LargestPoolSize"      ), target.getLargestPoolSize());
        metricFamily.addMetric(getName("MaximumPoolSize"      ), target.getMaximumPoolSize());
        metricFamily.addMetric(getName("PoolSize"             ), target.getPoolSize());
        metricFamily.addMetric(getName("TaskCount"            ), target.getTaskCount());
        metricFamily.addMetric(getName("ActiveCount"          ), target.getActiveCount());
        metricFamily.addMetric(getName("queue_size"              ), target.getQueue().size());
        metricFamily.addMetric(getName("queue_remaining_capacity"), target.getQueue().remainingCapacity());
        return singletonList(metricFamily);
    }

    private List<String> getName(String aName) {
        return singletonList(aName.replace("get", ""));
    }

}
