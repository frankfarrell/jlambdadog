package com.github.frankfarrell.jlambdadog;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.SlidingTimeWindowReservoir;
import com.codahale.metrics.Snapshot;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static java.util.stream.Collectors.joining;

public class DatadogLambdaMetricRegistry {

    private static final Map<Expansion, Function<Snapshot, Number>> EXPANSION_MAPPING = new HashMap<>();

    static {
        EXPANSION_MAPPING.put(Expansion.MEDIAN, Snapshot::getMedian);
        EXPANSION_MAPPING.put(Expansion.MAX, Snapshot::getMax);
        EXPANSION_MAPPING.put(Expansion.MEAN, Snapshot::getMean);
        EXPANSION_MAPPING.put(Expansion.MIN, Snapshot::getMin);
        EXPANSION_MAPPING.put(Expansion.STD_DEV, Snapshot::getStdDev);
        EXPANSION_MAPPING.put(Expansion.P75, Snapshot::get75thPercentile);
        EXPANSION_MAPPING.put(Expansion.P95, Snapshot::get95thPercentile);
        EXPANSION_MAPPING.put(Expansion.P98, Snapshot::get98thPercentile);
        EXPANSION_MAPPING.put(Expansion.P99, Snapshot::get99thPercentile);
        EXPANSION_MAPPING.put(Expansion.P999, Snapshot::get999thPercentile);
    }

    private static final long MAX_LAMBDA_DURATION = 900L;

    public final EnumSet<Expansion> expansions;

    //Does this need to be AtomicLong?
    protected final ConcurrentMap<TaggedMetricTuple, AtomicLong> counters;
    protected final ConcurrentMap<TaggedMetricTuple, Histogram> histograms;

    protected final ConcurrentMap<TimestampGaugeTuple, Double> gauges;

    private final Map<String, String> defaultTags;

    public DatadogLambdaMetricRegistry(){
        this(Expansion.ALL, Collections.emptyMap());
    }

    public DatadogLambdaMetricRegistry(final EnumSet<Expansion> expansions){
        this(expansions, Collections.emptyMap());
    }

    public DatadogLambdaMetricRegistry(final Map<String, String> defaultTags){
        this(Expansion.ALL, defaultTags);
    }

    public DatadogLambdaMetricRegistry(final EnumSet<Expansion> expansions,
                                       final Map<String, String> defaultTags){
        this.counters = new ConcurrentHashMap<>();
        this.histograms = new ConcurrentHashMap<>();
        this.gauges = new ConcurrentHashMap<>();
        this.expansions = expansions;
        this.defaultTags = defaultTags;
    }

    /**
     * Prints the metrics to the console with System.out.println()
     * and deletes all the counters from history
     *
     * Typically called at the end of a lambda invocation but can be called in the middle
     */
    public void printMetrics(){
        this.printAllCounters();
        this.printAllCounters();
        this.printAllGauges();
    }

    /**
     * Increments a counter by one for the duration of the invocation
     * @param metricName
     */
    public void increment( final String metricName) {
        increment(metricName, new HashMap<>());
    }

    /**
     * Increment a counter by one with a list of tags
     * @param metricName
     * @param tagList
     */
    public void increment( final String metricName,
                           final Map<String, String> tagList) {
        increment(metricName, tagList, 1L);
    }

    /**
     * Increment a counter by value with a list of tags
     * @param metricName
     * @param tagList
     * @param value
     */
    public void increment(final String metricName,
                          final Map<String, String> tagList,
                          final Long value) {
        counters.putIfAbsent(new TaggedMetricTuple(metricName, tagList), new AtomicLong(0));
        counters.get(new TaggedMetricTuple(metricName, tagList))
                .addAndGet(value);
    }

    /**
     * Decrements a counter by one for the duration of the invocation
     * @param metricName
     */
    public void decrement( final String metricName) {
        decrement(metricName, new HashMap<>());
    }

    /**
     * Decrement a counter by one with a list of tags
     * @param metricName
     * @param tagList
     */
    public void decrement( final String metricName,
                           final Map<String, String> tagList) {
        decrement(metricName, tagList, -1L);
    }

    /**
     * Decrement a counter by value with a list of tags
     * @param metricName
     * @param tagList
     * @param value should not be negative
     */
    public void decrement(final String metricName,
                          final Map<String, String> tagList,
                          final Long value) {
        counters.putIfAbsent(new TaggedMetricTuple(metricName, tagList), new AtomicLong(0));
        counters.get(new TaggedMetricTuple(metricName, tagList))
                .addAndGet(-value);
    }

    public void sample(final String metricName,
                       final Long value){
        sample(metricName, new HashMap<>(), value);
    }

    public void sample(final String metricName,
                       final Map<String, String> tagList,
                       final Long value){
        histograms.putIfAbsent(new TaggedMetricTuple(metricName, tagList),
                new Histogram(new SlidingTimeWindowReservoir(MAX_LAMBDA_DURATION, TimeUnit.SECONDS)){
                    @Override
                    public long getCount() {
                        return getSnapshot().size();
                    }
                });
        histograms.get(new TaggedMetricTuple(metricName, tagList)).update(value);
    }

    public void gauge(final String metricName,
                      final Double value,
                      final Long timestamp){
        gauge(metricName, new HashMap<>(), value, timestamp);
    }

    public void gauge(final String metricName,
                      final Double value){
        gauge(metricName, new HashMap<>(), value);
    }

    public void gauge(final String metricName,
                      final Map<String, String> tagList,
                      final Double value){
        gauge(metricName, tagList, value, Instant.now().getEpochSecond());
    }

    public void gauge(final String metricName,
                      final Map<String, String> tagList,
                      final Double value,
                      final Long timestamp){
        /*
        For a given timestamp it overrides ny previous gauges measured for this metric+tag pair
         */
        gauges.put(new TimestampGaugeTuple(new TaggedMetricTuple(metricName, tagList), timestamp), value);
    }

    protected void printAllCounters() {
        final Long nowAsEpochSecond = Instant.now().getEpochSecond();
        counters.forEach((key, value) ->
                printDatadogMetrics(key.metricName,
                        MetricType.COUNT,
                        key.tags,
                        defaultTags,
                        value.get(),
                        nowAsEpochSecond
                ));
        counters.clear();
    }

    //Inspired by https://github.com/coursera/metrics-datadog/blob/master/metrics-datadog/src/main/java/org/coursera/metrics/datadog/DatadogReporter.java#L163
    protected void printAllHistograms() {

        final Long nowAsEpochSecond = Instant.now().getEpochSecond();
        histograms.forEach((key, histogram)-> {
                    final Snapshot snapshot = histogram.getSnapshot();

                    final Map<Expansion, Number> values = new HashMap<>();

                    expansions.forEach(expansion -> {
                        switch (expansion){
                            case COUNT:
                                values.put(Expansion.COUNT, histogram.getCount());
                                break;
                            default:
                                values.put(expansion,EXPANSION_MAPPING.get(expansion).apply(snapshot));
                                break;
                        }
                    });

                    values.forEach((expansion, value) -> {
                        printDatadogMetrics(
                                appendExpansionSuffix(key.metricName, expansion.toString()),
                                MetricType.GAUGE,
                                key.tags,
                                defaultTags,
                                value.longValue(), //Is this right?
                                nowAsEpochSecond
                        );
                    });
                }
        );
        histograms.clear();
    }

    protected void printAllGauges() {
        gauges.forEach((key, value) ->
                printDatadogMetrics(key.taggedMetricTuple.metricName,
                        MetricType.GAUGE,
                        key.taggedMetricTuple.tags,
                        defaultTags,
                        value,
                        key.timestamp
                ));
        gauges  .clear();
    }

    private String appendExpansionSuffix( final String metricName,
                                          final String expansion){
        return metricName + "." + expansion;
    }

    /*
    NB This assumes that a datadog agent is tailing the cloudwatch logs
     */
    private void printDatadogMetrics( final String metricName,
                                      final MetricType metricType,
                                      final Map<String, String> tagList,
                                      final Map<String, String> defaultTags,
                                      final Number value,
                                      final Long epochSecond) {
        final HashMap<String, String> allTags = new HashMap<>();
        allTags.putAll(tagList);
        allTags.putAll(defaultTags);
        System.out.println(
                String.format("MONITORING|%s|%s|%s|%s|%s",
                        String.valueOf(epochSecond),
                        value,
                        metricType.toString(),
                        metricName,
                        createFormattedTagsList(allTags)
                ));
    }

    protected static String createFormattedTagsList( final Map<String, String> tagList) {

        return tagList.entrySet()
                .stream()
                .map(entry ->
                        String.format("#%s:%s",
                                entry.getKey(),
                                entry.getValue()))
                .sorted()
                .collect(joining(","));
    }

    /*
    Inner class used as Key in the map for collecting counters
     */
    public final static class TaggedMetricTuple {
        final String metricName;
        final Map<String, String> tags;

        public TaggedMetricTuple(final String metricName,
                                 final Map<String, String> tags) {
            this.metricName = metricName;
            this.tags = tags;
        }

        @Override
        public final boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final TaggedMetricTuple that = (TaggedMetricTuple) o;

            if (metricName != null ? !metricName.equals(that.metricName) : that.metricName != null) return false;

            //We just want to compare based on the string representation of the tags
            return tags != null ? createFormattedTagsList(tags).equals(createFormattedTagsList(that.tags)) :
                    that.tags == null;
        }

        @Override
        public final int hashCode() {
            int result = metricName != null ? metricName.hashCode() : 0;
            result = 31 * result + (tags != null ? createFormattedTagsList(tags).hashCode() : 0);
            return result;
        }
    }

    public final static class TimestampGaugeTuple {

        final TaggedMetricTuple taggedMetricTuple;
        final Long timestamp;

        public TimestampGaugeTuple(final TaggedMetricTuple taggedMetricTuple,
                                   final Long timestamp) {
            this.taggedMetricTuple = taggedMetricTuple;
            this.timestamp = timestamp;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TimestampGaugeTuple that = (TimestampGaugeTuple) o;
            return Objects.equals(taggedMetricTuple, that.taggedMetricTuple) &&
                    Objects.equals(timestamp, that.timestamp);
        }

        @Override
        public int hashCode() {
            return Objects.hash(taggedMetricTuple, timestamp);
        }
    }
}
