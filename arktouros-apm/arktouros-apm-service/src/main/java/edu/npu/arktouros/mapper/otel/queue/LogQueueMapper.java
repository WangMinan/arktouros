package edu.npu.arktouros.mapper.otel.queue;

import edu.npu.arktouros.model.queue.LogQueueItem;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;

/**
 * @author : [wangminan]
 * @description : apm_log_queue表对应的mapper
 */
@Repository
@Slf4j
public class LogQueueMapper extends QueueMapper<LogQueueItem> {

    @Resource
    private DataSource dataSource;

    @Override
    public void add(LogQueueItem logQueueItem) {
        String sql = "insert into APM_LOG_QUEUE (DATA) values (?);";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement =
                    connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, logQueueItem.getData());
            preparedStatement.executeUpdate();
            ResultSet resultSet = preparedStatement.getGeneratedKeys();
            resultSet.next();
            logQueueItem.setId(resultSet.getLong("ID"));
            resultSet.close();
            preparedStatement.close();
        } catch (SQLException e) {
            log.error("Encounter error when get connection from dataSource", e);
        }
    }

    @Override
    public LogQueueItem getTop() {
        String sql = "select * from APM_LOG_QUEUE order by ID limit 1;";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement =
                    connection.prepareStatement(sql);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                LogQueueItem logQueueItem = new LogQueueItem();
                logQueueItem.setId(resultSet.getLong("ID"));
                logQueueItem.setData(resultSet.getString("DATA"));
                resultSet.close();
                preparedStatement.close();
                return logQueueItem;
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
        String sql = "select count(*) from APM_LOG_QUEUE;";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement =
                    connection.prepareStatement(sql);
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            long count = resultSet.getLong(1);
            resultSet.close();
            preparedStatement.close();
            return count;
        } catch (SQLException e) {
            log.error("Encounter error when get connection from dataSource", e);
        }
        return -1;
    }
}
