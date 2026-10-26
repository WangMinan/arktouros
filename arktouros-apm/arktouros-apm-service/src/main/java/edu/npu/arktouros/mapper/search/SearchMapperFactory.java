package edu.npu.arktouros.mapper.search;

import edu.npu.arktouros.mapper.search.sqlite.SqliteSearchMapper;
import edu.npu.arktouros.util.sqlite.SqliteStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import edu.npu.arktouros.mapper.search.elasticsearch.ElasticsearchMapper;
import edu.npu.arktouros.mapper.search.h2.H2SearchMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * @author : [wangminan]
 * @description : 搜索Mapper工厂
 */
@Component
@Slf4j
public class SearchMapperFactory implements FactoryBean<SearchMapper> {

    @Autowired
    private ObjectProvider<SqliteStore> sqliteStore;

    @Value("${instance.active.searchMapper}")
    private String activeSearchMapper;

    private SearchMapper searchMapper;

    @Override
    public SearchMapper getObject() {
        if (activeSearchMapper.toLowerCase(Locale.ROOT).equals("elasticsearch")) {
            searchMapper = new ElasticsearchMapper();
        } else if ("sqlite".equalsIgnoreCase(activeSearchMapper)) {
            searchMapper = new SqliteSearchMapper(sqliteStore.getObject());
        } else if (activeSearchMapper.toLowerCase(Locale.ROOT).equals("h2")) {
            searchMapper = new H2SearchMapper();
        } else {
            throw new IllegalArgumentException("Invalid searchMapper type: " + activeSearchMapper);
        }
        log.info("SearchMapperFactory init, current searchMapper:{}", activeSearchMapper);
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
