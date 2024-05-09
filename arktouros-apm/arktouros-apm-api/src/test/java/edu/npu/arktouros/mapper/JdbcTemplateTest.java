package edu.npu.arktouros.mapper;

import edu.npu.arktouros.mapper.otel.queue.LogQueueMapper;
import edu.npu.arktouros.model.queue.LogQueueItem;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author : [wangminan]
 * @description : H2配合JdbcTemplate的测试
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Slf4j
class JdbcTemplateTest {

    @Resource
    private LogQueueMapper mapper;

    @Resource
    private DataSource dataSource;

    @Test
    @Timeout(30)
    void testInsert() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "insert into APM_METRICS_QUEUE (DATA) values (?);";
            PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, "123");
            int i = statement.executeUpdate();
            log.info("insert result: {}", i);
            sql = "select count(*) from APM_METRICS_QUEUE;";
            statement = conn.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            log.info("count: {}", resultSet.getInt(1));
            sql = "select * from APM_METRICS_QUEUE;";
            statement = conn.prepareStatement(sql);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                log.info("id: {}, data: {}", resultSet.getInt(1), resultSet.getString(2));
            }
            LogQueueItem logItem = LogQueueItem.builder().data("234").build();
            mapper.add(logItem);
            log.info("logItem: {}", logItem);
            LogQueueItem top = mapper.getTop();
            Assertions.assertEquals("234", top.getData());
            Assertions.assertFalse(mapper.isEmpty());
        }
    }
}
