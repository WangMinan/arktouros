package edu.npu.arktouros.mapper.otel.search;

import edu.npu.arktouros.mapper.otel.search.elasticsearch.ElasticSearchMapper;
import edu.npu.arktouros.mapper.otel.search.h2.H2SearchMapper;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * @author : [wangminan]
 * @description : 搜索Mapper工厂
 */
@Component
public class SearchMapperFactory implements FactoryBean<SearchMapper> {

    @Value("${instance.active.searchMapper}")
    private String activeSearchMapper;

    private SearchMapper searchMapper;

    @Override
    public SearchMapper getObject() throws Exception {
        if (activeSearchMapper.toLowerCase(Locale.ROOT).equals("elasticsearch")) {
            searchMapper = new ElasticSearchMapper();
        } else if (activeSearchMapper.toLowerCase(Locale.ROOT).equals("h2")) {
            searchMapper = new H2SearchMapper();
        } else {
            throw new IllegalArgumentException("Invalid searchMapper type: " + activeSearchMapper);
        }
        return searchMapper;
    }

    @Override
    public Class<?> getObjectType() {
        return (searchMapper != null) ? searchMapper.getClass() : SearchMapper.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
