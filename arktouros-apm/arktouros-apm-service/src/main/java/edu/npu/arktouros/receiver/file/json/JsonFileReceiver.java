package edu.npu.arktouros.receiver.file.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.npu.arktouros.model.exception.ArktourosException;
import edu.npu.arktouros.receiver.DataReceiver;
import edu.npu.arktouros.service.otel.sinker.SinkService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.io.File;
import java.io.FileInputStream;
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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author : [wangminan]
 * @description : Json文件接收器
 */
@Slf4j
public class JsonFileReceiver extends DataReceiver {

    private final String logDirName;
    private final File logDirFile;
    // 我们默认所有文件的创建时间不同
    private Map<Long, File> createTimeFileMap;
    private File currentFile;
    private Long currentFileCreateTime;
    private Long currentPos;
    private final String indexFilePath;
    private File indexFile;
    private static final int DEFAULT_CAPACITY = 1000;
    // 阻塞队列 不需要考虑并发问题 用synchronize或者lock画蛇添足会导致线程阻塞
    private final ArrayBlockingQueue<String> outputCache =
            new ArrayBlockingQueue<>(DEFAULT_CAPACITY);
    private final ExecutorService jsonFilePreHandlerExecutor = Executors.newSingleThreadExecutor(
            new BasicThreadFactory.Builder()
                    .namingPattern("Json-file-preHandler-%d").build()
    );

    public JsonFileReceiver(@NonNull String logDir, @NonNull String indexFilePath,
                            String fileType, SinkService sinkService, ObjectMapper objectMapper) {
        this.logDirName = logDir;
        this.logDirFile = new File(logDir);
        this.indexFilePath = indexFilePath;
        this.indexFile = new File(indexFilePath);
        initCreateTimeFileMap();
        try {
            initParamsWithIndex();
        } catch (IOException e) {
            throw new ArktourosException(e);
        }
        // 启动JsonFilePreHandler
        jsonFilePreHandlerExecutor.submit(
                new JsonFilePreHandler(outputCache, fileType, sinkService, objectMapper));
    }

    public void initCreateTimeFileMap() {
        this.createTimeFileMap = new HashMap<>();
        File[] files = logDirFile.listFiles();
        if (files != null) {
            for (File file : files) {
                // 需要排除索引文件
                if (!indexFilePath.toLowerCase(Locale.ROOT).contains(file.getName())) {
                    try {
                        // 获取创建时间
                        Path path = file.toPath();
                        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
                        this.createTimeFileMap.put(attributes.creationTime().toMillis(), file);
                    } catch (IOException e) {
                        throw new ArktourosException(e);
                    }
                }
            }
        }
    }

    public void initParamsWithIndex() throws IOException {
        // 去logs目录下检索是否有index文件
        // 在执行rename的时候中断程序会出现问题
        File tmpIndexFile = new File(indexFile.toPath().toString() + ".tmp");
        if (tmpIndexFile.exists()) {
            // 优先级更高 用tmpIndexFile的内容覆盖indexFile
            this.indexFile.createNewFile();
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
                            if (tmpIndex.exists()) {
                                Files.delete(tmpIndex.toPath());
                            }
                        } catch (IOException e) {
                            throw new ArktourosException(e, "failed while writing index file");
                        }
                        log.info("init index file complete");
                    }, () -> {
                        throw new ArktourosException("the log dir is empty");
                    });
        } catch (IOException e) {
            throw new ArktourosException(e);
        }
    }

    private void readFile() throws InterruptedException {
        log.info("continuing reading log: {}, current position: {}", currentFile.getName(), currentPos);
        // 存在逐步写入情况 使用buffer读
        readCurrentFileWithChannel();
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
                Thread.sleep(2000);
            } else {
                log.info("switch to file: {}, current position: {}", currentFile.getName(), currentPos);
            }
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
                this.currentPos += bytesRead;
                String indexLine = currentFileCreateTime + ":" + currentPos;
                Files.write(indexFile.toPath(), List.of(indexLine),
                        StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
                buffer.clear();
            }
        } catch (IOException | InterruptedException e) {
            throw new ArktourosException(e);
        }
    }

    @Override
    public void start() {
        log.info("JsonFileReceiver start reading log files");
        while (true) {
            try {
                readFile();
            } catch (InterruptedException e) {
                log.error("JsonLogFileReceiver run failed", e);
                throw new ArktourosException(e);
            }
        }
    }

    @Override
    public void stop() {
        log.info("JsonFileReceiver stop reading log files");
        jsonFilePreHandlerExecutor.shutdown();
    }
}
