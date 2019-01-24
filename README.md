# jLambdaDog
(Apologies for the silly name ) 

Java library for sending Datadog metrics from AWS Lambda via Cloudwatch logs. See https://docs.datadoghq.com/integrations/amazon_lambda/ for details. 

Track counters, gauges and histograms.

Essentially it automates this: 

```
To send custom metrics to Datadog from your Lambda logs, print a log line from your Lambda, using the following format:

MONITORING|<unix_epoch_timestamp>|<value>|<metric_type>|<metric_name>|#<tag_list>
Where:

MONITORING signals to the Datadog integration that it should collect this log entry.

<unix_epoch_timestamp> is in seconds, not milliseconds.

<value> MUST be a number (i.e. integer or float).

<metric_type> is count, gauge, histogram, or check.

<metric_name> uniquely identifies your metric and adheres to the metric naming policy.

<tag_list> is optional, comma separated, and must be preceded by #. The tag function_name:<name_of_the_function> is automatically applied to custom metrics.

Note: The sum for each timestamp is used for counts and the last value for a given timestamp is used for gauges. It is not recommended to print a log statement every time you increment a metric, as this increases the time it takes to parse your logs. Continually update the value of the metric in your code, and print one log statement for that metric before the function finishes.

```

# How to use?

Initialise your DatadogLambdaMetricRegistry instance with default tags and set of Histogram expansions, both optional. 
```java
import com.github.frankfarrell.jlambdadog.*;

public class MyHandler implements RequestStreamHandler {

    private final DatadogLambdaMetricRegistry registry;

    public MyHandler() throws IOException {
        this.registry = new DatadogLambdaMetricRegistry();
        
        //or set the default tags for all metrics
        this.registry = new DatadogLambdaMetricRegistry(getDefaultTags());
        
        //or set the tags and the set of expansions for histograms
        final EnumSet<Expansion> expansions = EnumSet.of(Expansion.MEDIAN);
        this.registry = new DatadogLambdaMetricRegistry(getDefaultTags(), expansions);
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        
        //Do some work
        
        //Increment a counter
        registry.increment("my.lovely.counter", 4L);
        
        //time some method
        final long startTime = System.currentTimeMillis();
        //Some work here
        final long endTime = System.currentTimeMillis();
        final long duration = applicationEndTime - applicationStartTime;
        
        //Track it in an in memory histogram -> Use this if you are processing events in batches
        registry.sample("my.lovely.sample", duration);
        
        //Track it as a gauge -> Use this for once off measured events, or if the timestamp of the event is important
        registry.gauge("my.lovely.gauge", duration, Instant.now().getEpochSecond());
        
        //TODO, yet to be implemented
        //Track it as a histogram -> Use this for once off measured events, or if the timestamp of the event is important
        registry.histogram("my.lovely.historgam", duration);
            
        //Make sure you call this at the end or at any time you want to print metrics. 
        registry.printMetrics();
    }
    
    private Map<String, String> getDefaultTags(){
        final Map<String, String> tags = new HashMap<>();
        tags.put("aws_region", System.getenv("AWS_REGION")); //Good practice to use Env variables for this sort of thing
        return tags;
    }
}

```

# Dependencies

The only dependency is `com.codahale.metrics:metrics-core`