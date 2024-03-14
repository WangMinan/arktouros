package edu.npu.sink;

/**
 * @author : [wangminan]
 * @description : es数据库操作
 */
public class ESDataOperation implements DataOperation {

    public static class Factory implements DataOperationFactory{

        @Override
        public DataOperation createDataOperation() {
            return new ESDataOperation();
        }
    }
}
