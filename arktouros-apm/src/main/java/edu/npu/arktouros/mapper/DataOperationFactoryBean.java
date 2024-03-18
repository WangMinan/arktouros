package edu.npu.arktouros.mapper;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * @author : [wangminan]
 * @description : 数据库操作工厂
 */
@Component
public class DataOperationFactoryBean implements FactoryBean<DataOperation> {

    @Value("${instance.active.dataOperation}")
    private String activeDataOperation;

    @Override
    public DataOperation getObject() {
        if (activeDataOperation.toLowerCase(Locale.ROOT).contains("h2")) {
            return new H2DataOperation();
        } else if (activeDataOperation.toLowerCase(Locale.ROOT).contains("es")){
            return new ESDataOperation();
        } else {
            throw new IllegalArgumentException("can not find data operation type from profile");
        }
    }

    @Override
    public Class<?> getObjectType() {
        return DataOperation.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
