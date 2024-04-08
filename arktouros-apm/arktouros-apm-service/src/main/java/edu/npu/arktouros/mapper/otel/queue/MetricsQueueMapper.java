package edu.npu.arktouros.mapper.otel.queue;

import edu.npu.arktouros.model.queue.MetricsQueueItem;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author : [wangminan]
 * @description : apm_metrics_queue表对应的mapper
 */
@Repository
@Slf4j
public class MetricsQueueMapper extends QueueMapper<MetricsQueueItem> {

    @Resource
    private DataSource dataSource;

    @Override
    public void add(MetricsQueueItem metricsQueueItem) {
        String sql = "insert into PUBLIC.APM_METRICS_QUEUE (DATA) values (?);";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement =
                    connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, metricsQueueItem.getData());
            preparedStatement.executeUpdate();
            ResultSet resultSet = preparedStatement.getGeneratedKeys();
            resultSet.next();
            metricsQueueItem.setId(resultSet.getLong("ID"));
            resultSet.close();
            preparedStatement.close();
        } catch (SQLException e) {
            log.error("Encounter error when get connection from dataSource: {}", e.getMessage());
        }
    }

    @Override
    public MetricsQueueItem getTop() {
        String sql = "select * from PUBLIC.APM_METRICS_QUEUE order by ID limit 1;";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement =
                    connection.prepareStatement(sql);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                MetricsQueueItem metricsQueueItem = new MetricsQueueItem();
                metricsQueueItem.setId(resultSet.getLong("ID"));
                metricsQueueItem.setData(resultSet.getString("DATA"));
                resultSet.close();
                preparedStatement.close();
                return metricsQueueItem;
            }
        } catch (SQLException e) {
            log.error("Encounter error when get connection from dataSource: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public boolean isEmpty() {
        return getSize() == 0;
    }

    @Override
    public long getSize() {
        String sql = "select count(*) from PUBLIC.APM_METRICS_QUEUE;";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement =
                    connection.prepareStatement(sql);
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            long size = resultSet.getLong(1);
            resultSet.close();
            preparedStatement.close();
            return size;
        } catch (SQLException e) {
            log.error("Encounter error when get connection from dataSource: {}", e.getMessage());
        }
        return 0;
    }

    @Override
    public void removeTop() {
        String sql = "delete from PUBLIC.APM_METRICS_QUEUE where ID = " +
                "(select ID from PUBLIC.APM_METRICS_QUEUE order by ID limit 1);";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement =
                    connection.prepareStatement(sql);
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            log.error("Encounter error when get connection from dataSource: {}", e.getMessage());
        }
    }

    @Override
    public boolean prepareTable() {
        String sql = """
                create table if not exists APM_METRICS_QUEUE
                (
                    ID   bigint primary key auto_increment(1), -- 标注1是因为我这儿试出来默认自增是32 太逆天了
                    DATA longtext
                );
                """;
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement =
                    connection.prepareStatement(sql);
            preparedStatement.executeUpdate();
            preparedStatement.close();
            return true;
        } catch (SQLException e) {
            log.error("Encounter error when get connection from dataSource: {}", e.getMessage());
            return false;
        }
    }
}
