package edu.npu.arktouros.mapper;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.sql.*;

/**
 * @author : [wangminan]
 * @description : H2配合JdbcTemplate的测试
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Slf4j
public class JdbcTemplateTest {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private DataSource dataSource;

    @Test
    void testInsert() throws SQLException {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("show schemas");
        ResultSet resultSet = statement.executeQuery();
        resultSet.next();
        log.info("schema: {}", resultSet.getString(1));
    }
}
