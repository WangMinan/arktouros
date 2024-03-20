package edu.npu.arktouros.mapper.otel.queue;

import edu.npu.arktouros.model.queue.MetricsQueueItem;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;

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
        String sql = "insert into APM_METRICS_QUEUE (DATA) values (?);";
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
            log.error("Encounter error when get connection from dataSource", e);
        }
    }

    @Override
    public MetricsQueueItem getTop() {
        String sql = "select * from APM_METRICS_QUEUE order by ID limit 1;";
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
            log.error("Encounter error when get connection from dataSource", e);
        }
        return null;
    }

    @Override
    public boolean isEmpty() {
        return getSize() == 0;
    }

    @Override
    public long getSize() {
        String sql = "select count(*) from APM_METRICS_QUEUE;";
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
            log.error("Encounter error when get connection from dataSource", e);
        }
        return 0;
    }
}
