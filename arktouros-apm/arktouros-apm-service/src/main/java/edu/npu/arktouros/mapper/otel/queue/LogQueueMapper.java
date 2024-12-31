package edu.npu.arktouros.mapper.otel.queue;

import edu.npu.arktouros.model.queue.LogQueueItem;
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
 * @description : apm_log_queue表对应的mapper
 */
@Repository
@Slf4j
public class LogQueueMapper extends QueueMapper<LogQueueItem> {

    @Resource
    private DataSource dataSource;

    @Override
    public void add(LogQueueItem logQueueItem) {
        String sql = "insert into PUBLIC.APM_LOG_QUEUE (DATA) values (?);";
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
            log.error("Encounter error when add log to dataSource: {}", e.getMessage());
        }
    }

    @Override
    public LogQueueItem getTop() {
        String sql = "select * from PUBLIC.APM_LOG_QUEUE order by ID limit 1;";
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
            log.error("Encounter error when get top log from dataSource: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public boolean isEmpty() {
        return getSize() == 0;
    }

    @Override
    public long getSize() {
        String sql = "select count(*) from PUBLIC.APM_LOG_QUEUE;";
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
            log.error("Encounter error when get log size from dataSource: {}", e.getMessage());
        }
        return 0;
    }

    @Override
    public void removeTop() {
        String sql = "delete from PUBLIC.APM_LOG_QUEUE where ID = " +
                "(select ID from PUBLIC.APM_LOG_QUEUE order by ID limit 1);";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement =
                    connection.prepareStatement(sql);
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            log.error("Encounter error when remove top from dataSource: {}", e.getMessage());
        }
    }

    @Override
    public boolean prepareTable() {
        String sql = """
                create table if not exists APM_LOG_QUEUE
                (
                    ID   bigint primary key auto_increment(1),
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
            log.error("Encounter error when prepare log table in dataSource: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void clear() {
        String sql = "delete from PUBLIC.APM_LOG_QUEUE;";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement =
                    connection.prepareStatement(sql);
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            log.error("Encounter error when clear log table in dataSource: {}", e.getMessage());
        }
    }
}
