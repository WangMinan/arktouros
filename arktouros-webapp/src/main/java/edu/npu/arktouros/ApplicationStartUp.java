package edu.npu.arktouros;

import com.linecorp.armeria.common.SessionProtocol;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.file.FileService;
import com.linecorp.armeria.server.file.HttpFile;
import edu.npu.arktouros.config.PropertiesProvider;
import lombok.extern.slf4j.Slf4j;

/**
 * @author : [wangminan]
 * @description : 静态托管http服务主启动类
 */
@Slf4j
public class ApplicationStartUp {

    public static void main(String[] args) {
        PropertiesProvider.init();
        int port = Integer.parseInt(
                PropertiesProvider.getProperty("server.port", "50052")
        );
        log.info("Starting up the application...");
        final HttpService indexPage =
                HttpFile
                        .of(ApplicationStartUp.class.getClassLoader(),
                                "/public/index.html")
                        .asService();
        Server
                .builder()
                .port(port, SessionProtocol.HTTP)
                .serviceUnder("/",
                        FileService.of(
                                        ApplicationStartUp.class.getClassLoader(),
                                        "/public")
                                .orElse(indexPage))
                .build()
                .start()
                .join();
        log.info("Application started successfully.");
        // shutdown hook
        addShutdownHook();
    }

    protected static void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down the application...");
            log.info("Application shutdown successfully.");
        }));
    }
}
