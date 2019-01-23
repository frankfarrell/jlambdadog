# jLambdaDog
(Apologies for the silly name ) 

Java library for sending Datadog metrics from AWS Lambda via Cloudwatch logs. See https://docs.datadoghq.com/integrations/amazon_lambda/ for details. 

Call the increment, histogram and gauge methods in your code. 

Call the `printMetrics()` method at the end of our lambda run. 

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

# Dependencies

The only dependency is `com.codahale.metrics:metrics-core`