package edu.npu.arktouros.service.otel.sinker;

import edu.npu.arktouros.service.otel.sinker.elasticsearch.ElasticSearchSinkService;
import edu.npu.arktouros.service.otel.sinker.h2.H2SinkService;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;

import java.util.Locale;

/**
 * @author : [wangminan]
 * @description : 数据持久化工厂
 */
public class SinkServiceFactoryBean implements FactoryBean<SinkService> {

    @Value("${instance.active.sinker}")
    private String activeSinker;

    @Override
    public SinkService getObject() throws Exception {
        if (activeSinker.toLowerCase(Locale.ROOT).equals("elasticsearch")) {
            return new ElasticSearchSinkService();
        } else if (activeSinker.toLowerCase(Locale.ROOT).equals("h2")) {
            return new H2SinkService();
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
