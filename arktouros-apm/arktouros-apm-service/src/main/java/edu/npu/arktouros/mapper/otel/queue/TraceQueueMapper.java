package edu.npu.arktouros.mapper.otel.queue;

import edu.npu.arktouros.model.queue.TraceQueueItem;
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
 * @description : 链路追踪队列对应的mapper
 */
@Repository
@Slf4j
public class TraceQueueMapper extends QueueMapper<TraceQueueItem> {

    @Resource
    private DataSource dataSource;

    @Override
    public void add(TraceQueueItem traceQueueItem) {
        String sql = "insert into PUBLIC.APM_TRACE_QUEUE (DATA) values (?);";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement =
                    connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, traceQueueItem.getData());
            preparedStatement.executeUpdate();
            ResultSet resultSet = preparedStatement.getGeneratedKeys();
            resultSet.next();
            traceQueueItem.setId(resultSet.getLong("ID"));
            resultSet.close();
            preparedStatement.close();
        } catch (SQLException e) {
            log.error("Encounter error when add span to dataSource: {}", e.getMessage());
        }
    }

    @Override
    public TraceQueueItem getTop() {
        String sql = "select * from PUBLIC.APM_TRACE_QUEUE order by ID limit 1;";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement =
                    connection.prepareStatement(sql);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                TraceQueueItem traceQueueItem = new TraceQueueItem();
                traceQueueItem.setId(resultSet.getLong("ID"));
                traceQueueItem.setData(resultSet.getString("DATA"));
                resultSet.close();
                preparedStatement.close();
                return traceQueueItem;
            }
        } catch (SQLException e) {
            log.error("Encounter error when get top span from dataSource: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public boolean isEmpty() {
        return getSize() == 0;
    }

    @Override
    public long getSize() {
        String sql = "select count(*) from PUBLIC.APM_TRACE_QUEUE;";
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
            log.error("Encounter error when get span size from dataSource: {}", e.getMessage());
        }
        return 0;
    }

    @Override
    public void removeTop() {
        String sql = "delete from PUBLIC.APM_TRACE_QUEUE where ID = " +
                "(select ID from PUBLIC.APM_TRACE_QUEUE order by ID limit 1);";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement =
                    connection.prepareStatement(sql);
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            log.error("Encounter error when remove top span from dataSource: {}", e.getMessage());
        }
    }

    @Override
    public boolean prepareTable() {
        String sql = """
                create table if not exists APM_TRACE_QUEUE
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
            log.error("Encounter error when prepare span table from dataSource: {}", e.getMessage());
            return false;
        }
    }
}
