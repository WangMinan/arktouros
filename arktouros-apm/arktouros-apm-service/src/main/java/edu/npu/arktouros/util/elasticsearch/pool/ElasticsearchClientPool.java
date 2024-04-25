package edu.npu.arktouros.util.elasticsearch.pool;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;

/**
 * @author : [wangminan]
 * @description : ElasticsearchClient池
 */
@Slf4j
public class ElasticsearchClientPool {
    private static final ElasticsearchClientPoolFactory factory =
            new ElasticsearchClientPoolFactory();
    private static final GenericObjectPool<ElasticsearchClient> pool =
            new GenericObjectPool<>(factory, factory.getPoolConfig());

    public static GenericObjectPool<ElasticsearchClient> getInstance() {
        return pool;
    }

    public static ElasticsearchClient getClient() {
        try {
            return pool.borrowObject();
        } catch (Exception e) {
            log.error("Failed to get ElasticsearchClient from pool: {}", e.getMessage());
            throw new RuntimeException("Failed to get ElasticsearchClient from pool", e);
        }
    }

    public static void returnClient(ElasticsearchClient client) {
        pool.returnObject(client);
    }
}
