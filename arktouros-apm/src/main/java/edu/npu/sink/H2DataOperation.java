package edu.npu.sink;

/**
 * @author : [wangminan]
 * @description : h2数据库操作
 */
public class H2DataOperation implements DataOperation {

    public static class Factory implements DataOperationFactory{
        public DataOperation createDataOperation() {
            return new H2DataOperation();
        }
    }
}
