package edu.npu.arktouros.util.elasticsearch.pool;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import edu.npu.arktouros.model.exception.ArktourosException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;

/**
 * @author : [wangminan]
 * @description : ElasticsearchClient池
 */
@Slf4j
public class ElasticsearchClientPool {
    protected static ElasticsearchClientPoolFactory factory =
            new ElasticsearchClientPoolFactory();
    protected static GenericObjectPool<ElasticsearchClient> pool =
            new GenericObjectPool<>(factory, factory.getPoolConfig());

    private ElasticsearchClientPool() {
        throw new UnsupportedOperationException("ElasticsearchClientPool is a utility class and cannot be instantiated");
    }

    public static GenericObjectPool<ElasticsearchClient> getInstance() {
        return pool;
    }

    public static ElasticsearchClient getClient() {
        try {
            return pool.borrowObject();
        } catch (Exception e) {
            log.error("Failed to get ElasticsearchClient from pool: {}", e.getMessage());
            throw new ArktourosException(e, "Failed to get ElasticsearchClient from pool");
        }
    }

    public static void returnClient(ElasticsearchClient client) {
        pool.returnObject(client);
    }
}
