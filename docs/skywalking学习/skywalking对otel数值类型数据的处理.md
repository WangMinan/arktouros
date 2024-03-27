# 对Metrics类型数据的处理

Otel对Metrics类型数据的定义如下所示：

```protobuf
message Metric {
  reserved 4, 6, 8;

  // name of the metric, including its DNS name prefix. It must be unique.
  string name = 1;

  // description of the metric, which can be used in documentation.
  string description = 2;

  // unit in which the metric value is reported. Follows the format
  // described by http://unitsofmeasure.org/ucum.html.
  string unit = 3;

  // Data determines the aggregation type (if any) of the metric, what is the
  // reported value type for the data points, as well as the relatationship to
  // the time interval over which they are reported.
  oneof data {
    Gauge gauge = 5;
    Sum sum = 7;
    Histogram histogram = 9;
    ExponentialHistogram exponential_histogram = 10;
    Summary summary = 11;
  }
}
```

核心部分就是`data`

- **Gauge**：Gauge是一种度量类型，它表示一个可以随时间变化的值。例如，CPU使用率或内存使用量就可以用Gauge来表示。Gauge的值可以增加，也可以减少。

- **Sum**：Sum是一种度量类型，它表示一个累积的值。Sum有两种形式：增量（Delta）和累积（Cumulative）。增量Sum表示在一段时间内的增量，例如在过去的5分钟内接收到的请求数。累积Sum表示从某个固定起点开始的累积值，例如从服务器启动开始到现在接收到的总请求数。

- **Histogram**：Histogram是一种度量类型，它表示一组值的分布情况。Histogram将值的范围划分为多个桶（Bucket），每个桶代表一定范围的值。Histogram会记录每个桶中的值的数量，从而得到值的分布情况。例如，请求延迟的分布就可以用Histogram来表示。

- **ExponentialHistogram**：ExponentialHistogram是Histogram的一种变体，它使用指数级的桶来表示一组值的分布情况。这使得它能够在广泛的值范围内提供有意义的数据。ExponentialHistogram的桶的范围是按照指数级别划分的，这意味着每个桶的范围都是前一个桶范围的倍数。

- **Summary**：Summary是一种度量类型，它表示一组值的统计摘要。Summary通常会提供一组值的最小值、最大值、平均值以及百分位数。例如，请求延迟的统计摘要就可以用Summary来表示。

## Gauge

skywalking将Otel类型的Gauge转换为prometheus类型的gauge

```java
if (metric.hasGauge()) {
    return metric.getGauge().getDataPointsList().stream()
                 .map(point -> new Gauge(
                     metric.getName(),
                     mergeLabels(
                         nodeLabels,
                         buildLabels(point.getAttributesList())
                     ),
                     point.hasAsDouble() ? point.getAsDouble()
                         : point.getAsInt(),
```

在OpenTelemetry的Metrics数据模型中，每个Metric都可以包含多个DataPoint，每个DataPoint都可以有一组标签（Labels）。这些标签用于描述这个DataPoint的一些属性，例如，它可能表示这个DataPoint是从哪个服务或哪个实例收集的。  在上述代码中，nodeLabels是从ResourceMetrics中获取的，它包含了一些全局的标签，例如服务名或实例名。这些标签对于同一个ResourceMetrics中的所有Metric和DataPoint都是相同的。pointLabels是从每个DataPoint中获取的，它包含了一些特定于这个DataPoint的标签。例如，如果一个Metric表示了每个HTTP请求的延迟，那么每个DataPoint可能会有一个标签表示这个请求的HTTP方法（GET、POST等）。 

在Prometheus的数据模型中，Gauge是一种简单的度量类型，它只有以下四个属性：  

+ name：度量的名称，例如"cpu_usage"。
+ labels：度量的标签，这是一个键值对的映射，用于描述度量的一些属性，例如{"instance": "localhost:9090"}。
+ timestamp：度量的时间戳，表示这个度量值是何时被收集的。
+ value：度量的值，这是一个浮点数，表示这个度量的当前值。
  这些属性在Metric类中被定义，而Gauge类继承自Metric类。在Gauge类中，还定义了一些操作这个度量值的方法，例如inc()（增加1）、dec()（减少1）和setValue(double value)（设置值）。

整个方法相当于在这两个gauge之间进行了映射。



## Sum

在`OpenTelemetryMetricRequestProcessor`中，对于OpenTelemetry的`Sum`类型，处理方式取决于`Sum`的`AggregationTemporality`属性和`isMonotonic`属性。

- 如果`AggregationTemporality`属性为`AGGREGATION_TEMPORALITY_DELTA`，则将`Sum`类型的数据转换为SkyWalking的`Gauge`类型。这是因为`AGGREGATION_TEMPORALITY_DELTA`表示这是一个增量值，例如在过去的5分钟内接收到的请求数，这种情况下，使用`Gauge`类型更合适。

- 如果`isMonotonic`属性为`true`，则将`Sum`类型的数据转换为SkyWalking的`Counter`类型。这是因为`isMonotonic`为`true`表示这是一个只能增加的值，例如从服务器启动开始到现在接收到的总请求数，这种情况下，使用`Counter`类型更合适。

具体的代码实现如下：

```java
if (metric.hasSum()) {
    final Sum sum = metric.getSum();
    if (sum.getAggregationTemporality() == AGGREGATION_TEMPORALITY_UNSPECIFIED) {
        return Stream.empty();
    }
    if (sum.getAggregationTemporality() == AGGREGATION_TEMPORALITY_DELTA) {
        return sum.getDataPointsList().stream()
                  .map(point -> new Gauge(
                      metric.getName(),
                      mergeLabels(
                          nodeLabels,
                          buildLabels(point.getAttributesList())
                      ),
                      point.hasAsDouble() ? point.getAsDouble()
                          : point.getAsInt(),
                      point.getTimeUnixNano() / 1000000
                  ));
    }
    if (sum.getIsMonotonic()) {
        return sum.getDataPointsList().stream()
                  .map(point -> new Counter(
                      metric.getName(),
                      mergeLabels(
                          nodeLabels,
                          buildLabels(point.getAttributesList())
                      ),
                      point.hasAsDouble() ? point.getAsDouble()
                          : point.getAsInt(),
                      point.getTimeUnixNano() / 1000000
                  ));
    } else {
        return sum.getDataPointsList().stream()
                  .map(point -> new Gauge(
                      metric.getName(),
                      mergeLabels(
                          nodeLabels,
                          buildLabels(point.getAttributesList())
                      ),
                      point.hasAsDouble() ? point.getAsDouble()
                          : point.getAsInt(),
                      point.getTimeUnixNano() / 1000000
                  ));
    }
}
```

这段代码首先检查`Sum`的`AggregationTemporality`属性，如果为`AGGREGATION_TEMPORALITY_UNSPECIFIED`，则不进行任何处理。如果为`AGGREGATION_TEMPORALITY_DELTA`，则将`Sum`转换为`Gauge`。然后，检查`isMonotonic`属性，如果为`true`，则将`Sum`转换为`Counter`，否则，将`Sum`转换为`Gauge`。

上文中提到的Counter数据其实也是Prometheus的一种数据类型。在Prometheus的数据模型中，Counter是一种度量类型，它表示一个只能增加的值。例如，从服务器启动开始到现在接收到的总请求数就可以用Counter来表示。  Counter有以下几个属性：  

+ name：度量的名称，例如"total_requests"。
+ labels：度量的标签，这是一个键值对的映射，用于描述度量的一些属性，例如{"instance": "localhost:9090"}。
+ timestamp：度量的时间戳，表示这个度量值是何时被收集的。
+ value：度量的值，这是一个浮点数，表示这个度量的当前值。

这些属性在Metric类中被定义，而Counter类继承自Metric类。在Counter类中，还定义了一些操作这个度量值的方法，例如inc()（增加1）和inc(double value)（增加指定的值）。

## Histogram与ExponentialHistogram

是的，你的理解是正确的。在`OpenTelemetryMetricRequestProcessor`中，OpenTelemetry的`Histogram`和`ExponentialHistogram`类型都被转换为了Prometheus的`Histogram`类型。

对于OpenTelemetry的`Histogram`类型，处理方式如下：

- 首先，从每个`DataPoint`中获取样本数量（`count`）、样本总和（`sum`）、桶（`bucket`）以及时间戳（`timestamp`）。
- 然后，将这些信息用于创建一个新的Prometheus的`Histogram`实例。

对于OpenTelemetry的`ExponentialHistogram`类型，处理方式如下：

- 首先，从每个`DataPoint`中获取样本数量（`count`）、样本总和（`sum`）、正负桶（`positive`和`negative`）以及时间戳（`timestamp`）。
- 然后，使用`buildBucketsFromExponentialHistogram`方法将正负桶转换为Prometheus的桶。
- 最后，将这些信息用于创建一个新的Prometheus的`Histogram`实例。

具体的代码实现如下：

```java
if (metric.hasHistogram()) {
    return metric.getHistogram().getDataPointsList().stream()
                 .map(point -> new Histogram(
                     metric.getName(),
                     mergeLabels(
                         nodeLabels,
                         buildLabels(point.getAttributesList())
                     ),
                     point.getCount(),
                     point.getSum(),
                     buildBuckets(
                         point.getBucketCountsList(),
                         point.getExplicitBoundsList()
                     ),
                     point.getTimeUnixNano() / 1000000
                 ));
}
if (metric.hasExponentialHistogram()) {
    return metric.getExponentialHistogram().getDataPointsList().stream()
                 .map(point -> new Histogram(
                     metric.getName(),
                     mergeLabels(
                         nodeLabels,
                         buildLabels(point.getAttributesList())
                     ),
                     point.getCount(),
                     point.getSum(),
                     buildBucketsFromExponentialHistogram(
                         point.getPositive().getOffset(),
                         point.getPositive().getBucketCountsList(),
                         point.getNegative().getOffset(),
                         point.getNegative().getBucketCountsList(),
                         point.getScale()
                     ),
                     point.getTimeUnixNano() / 1000000
                 ));
}
```

这段代码首先检查`Metric`是否有`Histogram`或`ExponentialHistogram`，如果有，则将其转换为Prometheus的`Histogram`。在转换过程中，会将`DataPoint`的标签和`ResourceMetrics`的标签合并，如果两者有相同的键，那么`DataPoint`的标签会覆盖`ResourceMetrics`的标签。这是因为`DataPoint`的标签比`ResourceMetrics`的标签具有更高的优先级。

`buildBuckets`函数和`buildBucketsFromExponentialHistogram`函数都是用于构建Prometheus的Histogram类型的桶（buckets）的。

`buildBuckets`函数接收两个参数：`bucketCounts`和`explicitBounds`。`bucketCounts`是一个包含每个桶中的样本数量的列表，`explicitBounds`是一个包含每个桶的上界的列表。这个函数通过遍历`explicitBounds`列表，并将每个上界与对应的样本数量映射到一个Map中，来构建桶。最后，它将正无穷大映射到`bucketCounts`列表的最后一个元素，表示所有大于最大上界的样本都会被放入这个桶中。

```java
private static Map<Double, Long> buildBuckets(
    final List<Long> bucketCounts,
    final List<Double> explicitBounds) {

    final Map<Double, Long> result = new HashMap<>();
    for (int i = 0; i < explicitBounds.size(); i++) {
        result.put(explicitBounds.get(i), bucketCounts.get(i));
    }
    result.put(Double.POSITIVE_INFINITY, bucketCounts.get(explicitBounds.size()));
    return result;
}
```

`buildBucketsFromExponentialHistogram`函数用于处理OpenTelemetry的`ExponentialHistogram`类型。这个函数接收五个参数：`positiveOffset`、`positiveBucketCounts`、`negativeOffset`、`negativeBucketCounts`和`scale`。这些参数分别对应于`ExponentialHistogram`的正桶偏移、正桶计数、负桶偏移、负桶计数和比例。这个函数首先计算基数（base），然后使用公式`base^(offset+index+1)`和`-base^(offset+index)`分别计算正桶和负桶的上界，并将它们映射到一个Map中。最后，它将正无穷大映射到`positiveBucketCounts`列表的最后一个元素。

```java
private static Map<Double, Long> buildBucketsFromExponentialHistogram(
    int positiveOffset, final List<Long> positiveBucketCounts,
    int negativeOffset, final List<Long> negativeBucketCounts, int scale) {

    final Map<Double, Long> result = new HashMap<>();
    double base = Math.pow(2.0, Math.pow(2.0, -scale));
    double upperBound;
    for (int i = 0; i < negativeBucketCounts.size(); i++) {
        upperBound = -Math.pow(base, negativeOffset + i);
        result.put(upperBound, negativeBucketCounts.get(i));
    }
    for (int i = 0; i < positiveBucketCounts.size() - 1; i++) {
        upperBound = Math.pow(base, positiveOffset + i + 1);
        result.put(upperBound, positiveBucketCounts.get(i));
    }
    result.put(Double.POSITIVE_INFINITY, positiveBucketCounts.get(positiveBucketCounts.size() - 1));
    return result;
}
```

这两个函数都返回一个Map，其中键是桶的上界，值是该桶中的样本数量。

## Summary

在`OpenTelemetryMetricRequestProcessor`中，对于OpenTelemetry的`Summary`类型，处理方式如下：

- 首先，从每个`DataPoint`中获取样本数量（`count`）、样本总和（`sum`）、分位数值（`quantileValues`）以及时间戳（`timestamp`）。
- 然后，将这些信息用于创建一个新的Prometheus的`Summary`实例。

具体的代码实现如下：

```java
if (metric.hasSummary()) {
    return metric.getSummary().getDataPointsList().stream()
                 .map(point -> new Summary(
                     metric.getName(),
                     mergeLabels(
                         nodeLabels,
                         buildLabels(point.getAttributesList())
                     ),
                     point.getCount(),
                     point.getSum(),
                     point.getQuantileValuesList().stream().collect(
                         toMap(
                             SummaryDataPoint.ValueAtQuantile::getQuantile,
                             SummaryDataPoint.ValueAtQuantile::getValue
                         )),
                     point.getTimeUnixNano() / 1000000
                 ));
}
```

这段代码首先检查`Metric`是否有`Summary`，如果有，则将其转换为Prometheus的`Summary`。在转换过程中，会将`DataPoint`的标签和`ResourceMetrics`的标签合并，如果两者有相同的键，那么`DataPoint`的标签会覆盖`ResourceMetrics`的标签。这是因为`DataPoint`的标签比`ResourceMetrics`的标签具有更高的优先级。