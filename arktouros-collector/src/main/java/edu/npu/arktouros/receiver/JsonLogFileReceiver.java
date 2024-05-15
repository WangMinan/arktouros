package edu.npu.arktouros.receiver;

import edu.npu.arktouros.cache.AbstractCache;
import edu.npu.arktouros.config.PropertiesProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author : [wangminan]
 * @description : Json格式日志文件接收器
 */
@Slf4j
public class JsonLogFileReceiver extends AbstractReceiver {
    private String logDirName;

    private static File logDirFile;

    // 我们默认所有文件的创建时间不同
    private static Map<Long, File> createTimeFileMap;

    private static File currentFile;

    private static Long currentFileCreateTime;

    private static Long currentPos;

    private static File indexFile;

    public JsonLogFileReceiver(AbstractCache outputCache) {
        super(outputCache);
    }

    @Override
    public void run() {
        log.info("this is JsonLogFileReceiver, start working");
        try {
            prepare();
            while (true) {
                readFile();
            }
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void readFile() throws InterruptedException {
        log.info("continuing reading log: {}, current position: {}", currentFile.getName(), currentPos);
        // 存在逐步写入情况 使用buffer读
        readCurrentFileWithChannel();
        // readCurrentFileWithStream();
        // 如果当前文件读完 尝试切换
        if (currentPos == currentFile.length()) {
            log.info("read complete, start switching, current file: {}, current position: {}",
                    currentFile.getName(), currentPos);
            // 重置映射关系
            initCreateTimeFileMap();
            // 找出比当前文件更新的文件
            long gap = Long.MAX_VALUE;
            File lastFile = currentFile;
            File tmpFile = currentFile;
            Long tmpCreateTime = currentFileCreateTime;
            for (Map.Entry<Long, File> entry : createTimeFileMap.entrySet()) {
                if (entry.getKey() > currentFileCreateTime &&
                        entry.getKey() - currentFileCreateTime < gap) {
                    gap = entry.getKey() - currentFileCreateTime;
                    tmpCreateTime = entry.getKey();
                    tmpFile = entry.getValue();
                    currentPos = 0L;
                }
            }
            currentFile = tmpFile;
            currentFileCreateTime = tmpCreateTime;
            if (currentFile.equals(lastFile)) {
                // 没有找到比当前文件更新的文件
                log.info("All files have been read. Waiting for new input.");
                sleep(2000);
            } else {
                log.info("switch to file: {}, current position: {}", currentFile.getName(), currentPos);
            }
        }
    }

    // 定义一个方法，用于读取当前的输入流
    private void readCurrentFileWithStream() {
        try (BufferedInputStream currentStream = new BufferedInputStream(new FileInputStream(currentFile))) {
            // 如果当前的输入流为空，返回
            byte[] buffer = new byte[8 * 1024 * 1024];
            int len;
            currentStream.skip(currentPos);
            while ((len = currentStream.read(buffer)) != -1) {
                String line = new String(buffer, 0, len);
                this.outputCache.put(line);
                currentPos += len;
                String indexLine = currentFileCreateTime + ":" + currentPos;
                Files.write(indexFile.toPath(), List.of(indexLine),
                        StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void readCurrentFileWithChannel() {
        try (FileInputStream fis = new FileInputStream(currentFile);
             FileChannel channel = fis.getChannel()) {
            ByteBuffer buffer = ByteBuffer.allocate(8 * 1024 * 1024);
            int bytesRead;
            while ((bytesRead = channel.read(buffer, currentPos)) != -1) {
                buffer.flip();
                byte[] bytes = new byte[bytesRead];
                buffer.get(bytes);
                String line = new String(bytes);
                this.outputCache.put(line);
                currentPos += bytesRead;
                String indexLine = currentFileCreateTime + ":" + currentPos;
                Files.write(indexFile.toPath(), List.of(indexLine),
                        StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
                buffer.clear();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void prepare() throws IOException {
        log.info("prepare to read logs");
        logDirName =
                PropertiesProvider.getProperty("receiver.otlpLogFile.logDir");
        if (logDirName == null || logDirName.isEmpty()) {
            log.warn("Log dir: {} is not been set in config file, using default 'otel_logs'",
                    logDirName);
            logDirName = "otel_logs";
        }
        logDirFile = new File(logDirName);
        if (!logDirFile.exists()) {
            throw new FileNotFoundException("log dir not found");
        }
        initCreateTimeFileMap();
        initParamsWithIndex();
        log.info("JsonLogFileReceiver prepare complete");
    }

    public void initParamsWithIndex() throws IOException {
        // 去logs目录下检索是否有index文件
        indexFile = new File(logDirName + File.separator + "logs.index");
        // 在执行rename的时候中断程序会出现问题
        File tmpIndexFile = new File(logDirName + File.separator + "logs.index.tmp");
        if (tmpIndexFile.exists()) {
            // 优先级更高 用tmpIndexFile的内容覆盖indexFile
            indexFile.createNewFile();
            FileUtils.copyFile(tmpIndexFile, indexFile);
        } else if (indexFile.exists()) {
            String line;
            try {
                List<String> lines = FileUtils.readLines(indexFile, StandardCharsets.UTF_8);
                if (lines.isEmpty() || StringUtils.isEmpty(lines.getFirst())) {
                    initEmptyIndexFile();
                    return;
                }
                line = lines.getFirst();
            } catch (IOException e) {
                throw new IOException("failed while reading index file", e);
            }
            // 文件创建时间:读取位点
            String[] split = line.split(":");
            if (split.length != 2) {
                throw new IllegalArgumentException("index file format error");
            }
            currentFileCreateTime = Long.parseLong(split[0]);
            currentFile = createTimeFileMap.get(currentFileCreateTime);
            currentPos = Long.parseLong(split[1]);
        } else {
            initEmptyIndexFile();
        }
    }

    private void initEmptyIndexFile() {
        // 初始化索引文件与变量
        try {
            indexFile.createNewFile();
            createTimeFileMap.entrySet()
                    .stream()
                    .min(Map.Entry.comparingByKey())
                    .ifPresentOrElse(entry -> {
                        currentFile = entry.getValue();
                        currentPos = 0L;
                        currentFileCreateTime = entry.getKey();
                        List<String> line = List.of(currentFileCreateTime + ":" + currentPos);
                        File tmpIndex = new File(logDirName + File.separator + "logs.index.tmp");
                        try {
                            tmpIndex.createNewFile();
                            Files.write(tmpIndex.toPath(), line,
                                    StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
                            tmpIndex.renameTo(indexFile);
                            Files.delete(tmpIndex.toPath());
                        } catch (IOException e) {
                            throw new RuntimeException("failed while writing index file", e);
                        }
                        log.info("init index file complete");
                    }, () -> {
                        throw new RuntimeException("the log dir is empty");
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void initCreateTimeFileMap() {
        createTimeFileMap = new HashMap<>();
        File[] files = logDirFile.listFiles();
        if (files != null) {
            for (File file : files) {
                // 需要排除索引文件
                if (!file.getName().toLowerCase(Locale.ROOT).contains("logs.index")) {
                    try {
                        // 获取创建时间
                        Path path = file.toPath();
                        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
                        createTimeFileMap.put(attributes.creationTime().toMillis(), file);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    public static class Factory implements ReceiverFactory {

        @Override
        public AbstractReceiver createReceiver(AbstractCache outputCache) {
            return new JsonLogFileReceiver(outputCache);
        }
    }
}
