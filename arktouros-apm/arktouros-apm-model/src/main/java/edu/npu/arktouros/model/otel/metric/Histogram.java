/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package edu.npu.arktouros.model.otel.metric;

import co.elastic.clients.elasticsearch._types.mapping.DoubleNumberProperty;
import co.elastic.clients.elasticsearch._types.mapping.LongNumberProperty;
import co.elastic.clients.elasticsearch._types.mapping.NestedProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Getter
public class Histogram extends Metric {

    private long sampleCount;
    private double sampleSum;
    private Map<Double, Long> buckets;

    private static final Map<String, Property> bucketsMap = new HashMap<>();

    public static Map<String, Property> documentMap = new HashMap<>();

    static {
        bucketsMap.put("key", Property.of(property ->
                property.double_(DoubleNumberProperty.of(
                        doubleProperty ->
                                doubleProperty.index(true).store(true)))
        ));
        bucketsMap.put("value", Property.of(property ->
                property.long_(LongNumberProperty.of(
                        longProperty ->
                                longProperty.index(true).store(true)))
        ));
        documentMap.putAll(metricBaseMap);
        documentMap.put("sampleCount", Property.of(property ->
                property.long_(LongNumberProperty.of(
                        longProperty ->
                                longProperty.index(true).store(true)))
        ));
        documentMap.put("sampleSum", Property.of(property ->
                property.double_(DoubleNumberProperty.of(
                        doubleProperty ->
                                doubleProperty.index(true).store(true)))
        ));
        documentMap.put("buckets", Property.of(property ->
                property.nested(NestedProperty.of(
                        nestedProperty -> nestedProperty.properties(bucketsMap)
                ))
        ));
    }

    @Builder
    public Histogram(String name, @Singular Map<String, String> labels,
                     long sampleCount, double sampleSum,
                     @Singular Map<Double, Long> buckets, long timestamp) {
        super(name, labels, timestamp);
        getLabels().remove("le");
        this.sampleCount = sampleCount;
        this.sampleSum = sampleSum;
        this.buckets = buckets;
    }

    @Override
    public Metric sum(Metric m) {
        Histogram h = (Histogram) m;
        this.buckets = Stream.concat(getBuckets().entrySet().stream(), h.getBuckets().entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Long::sum, TreeMap::new));
        this.sampleSum = this.sampleSum + h.sampleSum;
        this.sampleCount = this.sampleCount + h.sampleCount;
        return this;
    }

    @Override
    public Double value() {
        return this.getSampleSum() * 1000 / this.getSampleCount();
    }
}
