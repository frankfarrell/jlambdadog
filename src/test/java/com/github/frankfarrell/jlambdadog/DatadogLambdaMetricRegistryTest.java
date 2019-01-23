package com.github.frankfarrell.jlambdadog;


import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

public class DatadogLambdaMetricRegistryTest {

    private DatadogLambdaMetricRegistry datadogLambdaMetricRegistryUnderTest;

    @Before
    public void setup() {
        datadogLambdaMetricRegistryUnderTest = new DatadogLambdaMetricRegistry(Collections.singletonMap("testTag", "testTagValue"));
    }

    @Test
    public void printAllCountersClearsValuesAfterPrinting() {

        datadogLambdaMetricRegistryUnderTest.increment("test1");
        datadogLambdaMetricRegistryUnderTest.increment("test2");

        assertThat(datadogLambdaMetricRegistryUnderTest.counters).isNotEmpty();

        datadogLambdaMetricRegistryUnderTest.printAllCounters();

        assertThat(datadogLambdaMetricRegistryUnderTest.counters).isEmpty();

    }

    @Test
    public void printAllHistogramsClearsValuesAfterPrinting() {

        datadogLambdaMetricRegistryUnderTest.sample("test1", 1L);
        datadogLambdaMetricRegistryUnderTest.sample("test2", 1L);

        assertThat(datadogLambdaMetricRegistryUnderTest.histograms).isNotEmpty();

        datadogLambdaMetricRegistryUnderTest.printAllHistograms();

        assertThat(datadogLambdaMetricRegistryUnderTest.histograms).isEmpty();

    }

    @Test
    public void incrementTest() {
        datadogLambdaMetricRegistryUnderTest.increment("test1");
        datadogLambdaMetricRegistryUnderTest.increment("test1");
        datadogLambdaMetricRegistryUnderTest.increment("test2");

        assertThat(datadogLambdaMetricRegistryUnderTest.counters).isNotEmpty();
        assertThat(datadogLambdaMetricRegistryUnderTest.counters.size()).isEqualTo(2);

        DatadogLambdaMetricRegistry.TaggedMetricTuple expectedKey =
                new DatadogLambdaMetricRegistry.TaggedMetricTuple("test1", Collections.emptyMap());
        assertThat(datadogLambdaMetricRegistryUnderTest.counters.get(expectedKey).get()).isEqualTo(2L);

    }

    @Test
    public void incrementWithTagsTest() {
        datadogLambdaMetricRegistryUnderTest
                .increment("test1", Collections.singletonMap("instanceTag", "instanceTagValue"));

        assertThat(datadogLambdaMetricRegistryUnderTest.counters).isNotEmpty();
        assertThat(datadogLambdaMetricRegistryUnderTest.counters.size()).isEqualTo(1);
        DatadogLambdaMetricRegistry.TaggedMetricTuple expectedKey =
                new DatadogLambdaMetricRegistry.TaggedMetricTuple("test1",
                        Collections.singletonMap("instanceTag", "instanceTagValue"));
        assertThat(datadogLambdaMetricRegistryUnderTest.counters).containsKeys(expectedKey);

    }

    @Test
    public void incrementWithTagsAndValueTest() {
        datadogLambdaMetricRegistryUnderTest
                .increment("test1", Collections.singletonMap("instanceTag", "instanceTagValue"), 100L);

        assertThat(datadogLambdaMetricRegistryUnderTest.counters).isNotEmpty();
        assertThat(datadogLambdaMetricRegistryUnderTest.counters.size()).isEqualTo(1);
        DatadogLambdaMetricRegistry.TaggedMetricTuple expectedKey =
                new DatadogLambdaMetricRegistry.TaggedMetricTuple("test1",
                        Collections.singletonMap("instanceTag", "instanceTagValue"));
        assertThat(datadogLambdaMetricRegistryUnderTest.counters).containsKeys(expectedKey);

        assertThat(datadogLambdaMetricRegistryUnderTest.counters.get(expectedKey).get()).isEqualTo(100L);
    }

    @Test
    public void createFormattedTagsListCreatesASortedFormattedString() {

        HashMap<String, String> testTags = new HashMap<>();

        testTags.put("BinstanceTag", "instanceTagValue");
        testTags.put("AinstanceTag2", "instanceTagValue2");
        assertThat(DatadogLambdaMetricRegistry.createFormattedTagsList(testTags))
                .isEqualTo("#AinstanceTag2:instanceTagValue2,#BinstanceTag:instanceTagValue");
    }

    @Test
    public void testEqualsContract() {
        EqualsVerifier.forClass(DatadogLambdaMetricRegistry.TaggedMetricTuple.class)
                .suppress(Warning.NULL_FIELDS)
                .verify();
    }
}
