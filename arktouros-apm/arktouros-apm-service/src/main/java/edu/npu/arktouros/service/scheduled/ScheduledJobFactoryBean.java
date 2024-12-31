package edu.npu.arktouros.service.scheduled;

import edu.npu.arktouros.service.scheduled.elasticsearch.ElasticsearchScheduledJob;
import edu.npu.arktouros.service.scheduled.h2.H2ScheduledJob;
import edu.npu.arktouros.service.search.SearchService;
import edu.npu.arktouros.service.sinker.SinkService;
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
    private final SinkService sinkService;

    private ScheduledJob scheduledJob;

    @Lazy
    public ScheduledJobFactoryBean(SearchService searchService,
                                   SinkService sinkService) {
        this.searchService = searchService;
        this.sinkService = sinkService;
    }

    @Override
    public ScheduledJob getObject() {
        log.info("ScheduledJobFactory init, current scheduledJob:{}", activeScheduledJob);
        if (scheduledJob == null) {
            if (activeScheduledJob.toLowerCase(Locale.ROOT).equals("elasticsearch")) {
                scheduledJob = new ElasticsearchScheduledJob(searchService, sinkService);
            } else if (activeScheduledJob.toLowerCase(Locale.ROOT).equals("h2")) {
                scheduledJob = new H2ScheduledJob(searchService, sinkService);
            } else {
                throw new IllegalArgumentException("Invalid scheduledJob type: " + activeScheduledJob);
            }
        }
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
