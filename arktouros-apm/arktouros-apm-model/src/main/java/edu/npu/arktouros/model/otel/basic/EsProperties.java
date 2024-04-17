package edu.npu.arktouros.model.otel.basic;

import co.elastic.clients.elasticsearch._types.mapping.KeywordProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TextProperty;

import java.util.Map;

/**
 * @author : [wangminan]
 * @description : ElasticSearch常用的properties
 */
public class EsProperties {

    public static Property keywordIndexProperty = Property.of(property ->
            property.keyword(KeywordProperty.of(
                    keywordProperty -> keywordProperty.index(true))));

    public static TextProperty textKeywordProperty = TextProperty.of(
            textProperty -> textProperty.index(true)
                    .fields(Map.of("keyword", Property.of(
                            property -> property.keyword(
                                    KeywordProperty.of(
                                            keywordProperty -> keywordProperty
                                                    .index(true).ignoreAbove(256)
                                    )
                            )

                    )))
    );
}
