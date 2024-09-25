package edu.npu.arktouros.service.otel.sinker;

import edu.npu.arktouros.service.otel.sinker.elasticsearch.ElasticsearchSinkService;
import edu.npu.arktouros.service.otel.sinker.h2.H2SinkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * @author : [wangminan]
 * @description : 数据持久化工厂
 */
@Component
@Slf4j
public class SinkServiceFactoryBean implements FactoryBean<SinkService> {

    @Value("${instance.active.sinker}")
    private String activeSinker;

    @Value("${sinker.span.timeout}")
    private int spanTimeout;

    private SinkService sinkService;

    @Override
    public SinkService getObject() {
        if (sinkService == null) {
            if (activeSinker.toLowerCase(Locale.ROOT).equals("elasticsearch")) {
                sinkService = new ElasticsearchSinkService(spanTimeout);
            } else if (activeSinker.toLowerCase(Locale.ROOT).equals("h2")) {
                sinkService = new H2SinkService();
            } else {
                throw new IllegalArgumentException("Invalid sinker type: " + activeSinker);
            }
        }
        // 这个日志实在打的太多了 所以换成debug
        log.debug("SinkServiceFactory init, current sinker:{}", activeSinker);
        return sinkService;
    }

    @Override
    public Class<?> getObjectType() {
        return (sinkService != null) ? sinkService.getClass() : SinkService.class;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }
}
