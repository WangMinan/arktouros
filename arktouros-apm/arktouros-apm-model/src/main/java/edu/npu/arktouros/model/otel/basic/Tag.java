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
 */

package edu.npu.arktouros.model.otel.basic;

import co.elastic.clients.elasticsearch._types.mapping.KeywordProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class Tag {
    public static final int TAG_LENGTH = 256;
    private String key;
    private String value;
    public static Map<String, Property> documentMap = new HashMap<>();

    public Tag(edu.npu.arktouros.proto.common.v1.Tag tag) {
        this.key = tag.getKey();
        this.value = tag.getValue();
    }

    static {
        documentMap.put("key", EsProperties.keywordIndexProperty);
        documentMap.put("value", Property.of(property ->
                property.text(EsProperties.textKeywordProperty)
        ));
    }

    @Override
    public String toString() {
        return key + "=" + value;
    }

    public static class Util {
        public static List<String> toStringList(List<Tag> list) {
            if (list.isEmpty()) {
                return Collections.emptyList();
            }
            return list.stream().map(Tag::toString).collect(Collectors.toList());
        }
    }
}
