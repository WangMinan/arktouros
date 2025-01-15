package edu.npu.arktouros.service.operation;

import edu.npu.arktouros.receiver.DataReceiver;
import edu.npu.arktouros.service.operation.elasticsearch.ElasticsearchOperationService;
import edu.npu.arktouros.service.operation.h2.H2OperationService;
import edu.npu.arktouros.service.scheduled.ScheduledJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author : [wangminan]
 * @description : 用于装配DataOperationService的工厂bean
 */
@Component
@Slf4j
public class DataOperationServiceFactoryBean implements FactoryBean<DataOperationService> {

    @Value("${instance.active.operation}")
    private String activeDataOperation;

    private final DataReceiver dataReceiver;
    private final ScheduledJob scheduledJob;

    private DataOperationService dataOperationService;

    @Lazy
    public DataOperationServiceFactoryBean(DataReceiver dataReceiver,
                                           ScheduledJob scheduledJob) {
        this.dataReceiver = dataReceiver;
        this.scheduledJob = scheduledJob;
    }

    @Override
    public DataOperationService getObject() throws Exception {
        log.info("DataOperationServiceFactory init, current dataOperation:{}", activeDataOperation);
        if (dataOperationService == null) {
            if (activeDataOperation.equals("elasticsearch")) {
                dataOperationService = new ElasticsearchOperationService(dataReceiver, scheduledJob);
            } else if (activeDataOperation.equals("h2")) {
                dataOperationService = new H2OperationService(dataReceiver, scheduledJob);
            } else {
                throw new IllegalArgumentException("Invalid data operation type: " + activeDataOperation);
            }
        }
        return dataOperationService;
    }

    @Override
    public Class<?> getObjectType() {
        return (dataOperationService != null) ? dataOperationService.getClass() : DataOperationService.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
