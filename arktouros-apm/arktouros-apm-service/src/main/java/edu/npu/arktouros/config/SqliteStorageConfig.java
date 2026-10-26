package edu.npu.arktouros.config;

import edu.npu.arktouros.util.sqlite.SqliteStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Configuration
@ConditionalOnProperty(name = "instance.active.sinker", havingValue = "sqlite")
public class SqliteStorageConfig {
    @Bean(destroyMethod = "close")
    public SqliteStore sqliteStore(Environment environment) throws IOException {
        for (String extension : List.of("sinker", "searchMapper", "scheduledJob", "operation")) {
            if (!"sqlite".equalsIgnoreCase(environment.getProperty("instance.active." + extension))) {
                throw new IllegalArgumentException("SQLite requires all four storage extensions to use sqlite: " + extension);
            }
        }
        return new SqliteStore(Path.of(environment.getProperty("sqlite.path", "./db/arktouros.sqlite")));
    }
}
