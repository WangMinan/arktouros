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

import co.elastic.clients.elasticsearch._types.mapping.DateProperty;
import co.elastic.clients.elasticsearch._types.mapping.DoubleNumberProperty;
import co.elastic.clients.elasticsearch._types.mapping.KeywordProperty;
import co.elastic.clients.elasticsearch._types.mapping.NestedProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Getter
public class Counter extends Metric {

    private double value;
    public static Map<String, Property> documentMap = new HashMap<>();
    static {
        documentMap.putAll(metricBaseMap);
        documentMap.put("value", Property.of(property ->
                property.double_(DoubleNumberProperty.of(
                        doubleProperty ->
                                doubleProperty.index(true).store(true)))
        ));
    }

    @Builder
    public Counter(String name, @Singular Map<String, String> labels,
                   double value, long timestamp) {
        super(name, labels, timestamp);
        this.value = value;
    }

    @Override public Metric sum(Metric m) {
        this.value = this.value + m.value();
        return this;
    }

    @Override public Double value() {
        return this.value;
    }
}
