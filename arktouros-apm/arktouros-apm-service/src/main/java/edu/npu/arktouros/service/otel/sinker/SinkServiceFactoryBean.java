package edu.npu.arktouros.service.otel.sinker;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;

import java.util.Locale;

/**
 * @author : [wangminan]
 * @description : 数据持久化工厂
 */
public class SinkServiceFactoryBean<T> implements FactoryBean<SinkService<T>> {

    @Value("${instance.active.sinker}")
    private String activeSinker;

    @Override
    public SinkService<T> getObject() throws Exception {
        if (activeSinker.toLowerCase(Locale.ROOT).equals("elasticsearch")) {
            return new ElasticSearchSinkService<>();
        } else if (activeSinker.toLowerCase(Locale.ROOT).equals("h2")) {
            return new H2SinkService<>();
        } else {
            throw new IllegalArgumentException("Invalid sinker type: " + activeSinker);
        }
    }

    @Override
    public Class<?> getObjectType() {
        if (activeSinker.toLowerCase(Locale.ROOT).equals("elasticsearch")) {
            return ElasticSearchSinkService.class;
        } else if (activeSinker.toLowerCase(Locale.ROOT).equals("h2")) {
            return H2SinkService.class;
        } else {
            throw new IllegalArgumentException("Invalid sinker type: " + activeSinker);
        }
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
