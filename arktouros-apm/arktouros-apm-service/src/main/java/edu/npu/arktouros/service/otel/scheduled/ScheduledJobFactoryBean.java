package edu.npu.arktouros.service.otel.scheduled;

import edu.npu.arktouros.service.otel.scheduled.elasticsearch.ElasticsearchScheduledJob;
import edu.npu.arktouros.service.otel.scheduled.h2.H2ScheduledJob;
import edu.npu.arktouros.service.otel.search.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * @author : [wangminan]
 * @description : 定时任务工厂
 */
@Component
@Slf4j
public class ScheduledJobFactoryBean implements FactoryBean<ScheduledJob> {

    @Value("${instance.active.scheduledJob}")
    private String activeScheduledJob;

    private final SearchService searchService;

    private ScheduledJob scheduledJob;

    @Lazy
    public ScheduledJobFactoryBean(SearchService searchService) {
        this.searchService = searchService;
    }

    @Override
    public ScheduledJob getObject() {
        log.info("ScheduledJobFactory init, current scheduledJob:{}", activeScheduledJob);
        if (scheduledJob == null) {
            if (activeScheduledJob.toLowerCase(Locale.ROOT).equals("elasticsearch")) {
                scheduledJob = new ElasticsearchScheduledJob(searchService);
            } else if (activeScheduledJob.toLowerCase(Locale.ROOT).equals("h2")) {
                scheduledJob = new H2ScheduledJob(searchService);
            } else {
                throw new IllegalArgumentException("Invalid scheduledJob type: " + activeScheduledJob);
            }
        }
        log.info("ScheduledJobFactory init, current scheduledJob:{}", activeScheduledJob);
        return scheduledJob;
    }

    @Override
    public Class<?> getObjectType() {
        return (scheduledJob != null) ? scheduledJob.getClass() : ScheduledJob.class;
    }

    @Override
    public boolean isSingleton() {
        return FactoryBean.super.isSingleton();
    }
}
