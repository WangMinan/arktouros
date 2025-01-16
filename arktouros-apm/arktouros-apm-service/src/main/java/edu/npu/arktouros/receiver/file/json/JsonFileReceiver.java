package edu.npu.arktouros.receiver.file.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.npu.arktouros.model.exception.ArktourosException;
import edu.npu.arktouros.receiver.DataReceiver;
import edu.npu.arktouros.receiver.file.domain.FileStatus;
import edu.npu.arktouros.receiver.file.domain.JsonFile;
import edu.npu.arktouros.service.queue.TraceQueueService;
import edu.npu.arktouros.service.sinker.SinkService;
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
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    private final File logDirFile;
    // 我们默认所有文件的创建时间不同
    private Map<Long, JsonFile> md5FileMap;
    private JsonFile currentFile;
    private Long currentFileNameMd5;
    private Long currentPos;
    private final String indexFilePath;
    private final File indexFile;
    private final String fileType;
    private final TraceQueueService traceQueueService;
    private final SinkService sinkService;
    private final ObjectMapper objectMapper;
    private final int sytelTraceAnalyzerNumber;
    private static final int DEFAULT_CAPACITY = 1000;
    private static final int DEFAULT_CHANNEL_SIZE = 8 * 1024 * 1024;
    // 阻塞队列 不需要考虑并发问题 用synchronize或者lock画蛇添足会导致线程阻塞
    private final ArrayBlockingQueue<String> outputCache =
            new ArrayBlockingQueue<>(DEFAULT_CAPACITY);
    private ExecutorService jsonFilePreHandlerExecutor = Executors.newSingleThreadExecutor(
            new BasicThreadFactory.Builder()
                    .namingPattern("Json-file-preHandler-%d").build()
    );

    public JsonFileReceiver(@NonNull String logDir, @NonNull String indexFilePath,
                            String fileType, TraceQueueService traceQueueService,
                            SinkService sinkService,
                            ObjectMapper objectMapper, int sytelTraceAnalyzerNumber) {
        this.logDirFile = new File(logDir);
        this.indexFilePath = indexFilePath;
        this.indexFile = new File(indexFilePath);
        this.fileType = fileType;
        this.traceQueueService = traceQueueService;
        this.sinkService = sinkService;
        this.objectMapper = objectMapper;
        this.sytelTraceAnalyzerNumber = sytelTraceAnalyzerNumber;

        initMd5FileMap();
        try {
            initParamsWithIndexFile();
        } catch (IOException e) {
            throw new ArktourosException(e);
        }
        // 启动JsonFilePreHandler
        jsonFilePreHandlerExecutor.submit(
                new JsonFilePreHandler(outputCache, fileType,
                        traceQueueService, sinkService, objectMapper,
                        sytelTraceAnalyzerNumber));
    }

    public void initParamsWithIndexFile() throws IOException {
        // 去logs目录下检索是否有index文件
        // 在执行rename的时候中断程序会出现问题
        File tmpIndexFile = new File(indexFile.toPath() + ".tmp");
        if (tmpIndexFile.exists()) {
            // 优先级更高 用tmpIndexFile的内容覆盖indexFile
            indexFile.deleteOnExit();
            boolean newFile = indexFile.createNewFile();
            if (!newFile) {
                log.warn("Index file already exists, tmp index may fail to cover the original index file.");
            }
            FileUtils.copyFile(tmpIndexFile, indexFile);
            FileUtils.delete(tmpIndexFile);
        } else if (indexFile.exists()) {
            // 没有tmpIndex，但是有indexFile 则读取indexFile
            String line;
            try {
                List<String> lines = FileUtils.readLines(indexFile, StandardCharsets.UTF_8);
                if (lines.isEmpty() || StringUtils.isEmpty(lines.getFirst())) {
                    initParamsWithEmptyIndexFile();
                    return;
                }
                line = lines.getFirst();
            } catch (IOException e) {
                throw new IOException("Failed while reading index file", e);
            }
            // 文件创建时间:读取位点
            String[] split = line.split(":");
            if (split.length != 2) {
                throw new IllegalArgumentException("Index file format error, unable to parse {currentFile}:{currentPos}.");
            }
            currentFileNameMd5 = Long.parseLong(split[0]);
            if (!md5FileMap.containsKey(currentFileNameMd5)) {
                log.warn("Index file contains a file that does not exist, reinitialize the index file.");
                indexFile.deleteOnExit();
                initParamsWithEmptyIndexFile();
                return;
            }
            currentFile = md5FileMap.get(currentFileNameMd5);
            currentPos = Long.parseLong(split[1]);
            if (currentPos != currentFile.getFile().length()) {
                // 从头开始
                currentPos = 0L;
            }
        } else {
            initParamsWithEmptyIndexFile();
        }
    }

    /**
     * 不存在索引文件 初始化一个空的索引文件 写入字典序 最小的文件名md5:0
     */
    private void initParamsWithEmptyIndexFile() {
        try {
            // 在该场景下需要确保indexFile是空的
            boolean newFile = indexFile.createNewFile();
            if (!newFile) {
                log.warn("Index file already exists.");
            }
            md5FileMap.entrySet()
                    .stream()
                    // 从md5FileMap中找到最小的文件名md5
                    .min(Map.Entry.comparingByKey())
                    .ifPresentOrElse(entry -> {
                        // 如果有文件
                        currentFile = entry.getValue();
                        currentPos = 0L;
                        currentFileNameMd5 = entry.getKey();
                        List<String> line =
                                List.of(currentFileNameMd5 + ":" + currentPos);
                        try {
                            Files.write(indexFile.toPath(), line, StandardCharsets.UTF_8);
                        } catch (IOException e) {
                            log.error("Failed to init empty index file while writing.", e);
                            throw new ArktourosException(e);
                        }
                        log.info("Init index file complete");
                    }, () -> {
                        // 如果没有log文件
                        log.info("No log file found, just create empty index file, waiting for new input.");
                    });
        } catch (IOException e) {
            throw new ArktourosException(e);
        }
    }

    public void initMd5FileMap() {
        this.md5FileMap = new HashMap<>();
        File[] files = logDirFile.listFiles();
        if (files != null) {
            for (File file : files) {
                // 需要排除索引文件
                if (!indexFilePath.toLowerCase(Locale.ROOT).contains(file.getName())) {
                    try {
                        long fileNameMd5 = encodeFileNameToNumber(file.getName());
                        // 这个是天脉目前的授时问题 所以不得不这样来进行
                        // 原来时间戳的方案在没有授时节点的情况下是不可行的
                        log.debug("Encode file: {} to md5 number: {}", file.getName(), fileNameMd5);
                        if (!md5FileMap.containsKey(fileNameMd5)) {
                            // 全局唯一写入md5FileMap的位置
                            md5FileMap.put(fileNameMd5, new JsonFile(file));
                        } else {
                            throw new IllegalArgumentException("Duplicate file name md5: " + fileNameMd5);
                        }
                    } catch (NoSuchAlgorithmException e) {
                        throw new ArktourosException(e);
                    }
                }
            }
        }
    }

    public long encodeFileNameToNumber(String fileName) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] hashBytes = digest.digest(fileName.getBytes(StandardCharsets.UTF_8));
        long number = 0;
        for (int i = 0; i < 8; i++) {
            number = (number << 8) | (hashBytes[i] & 0xFF);
        }
        return Math.abs(number);
    }

    private void readFile() throws InterruptedException, IOException {
        if (currentFile == null || currentFile.getFile() == null) {
            // 在初始化的时候文件夹里没有日志文件，初始化的index是空的
            // 重置映射关系
            initMd5FileMap();
            initParamsWithEmptyIndexFile();
        }
        // 如果当前文件读完 尝试切换
        if (currentPos == currentFile.getFile().length()) {
            currentFile.setStatus(FileStatus.READ);
            log.info("Read complete, start switching, current file: {}, current position: {}",
                    currentFile.getFile().getName(), currentPos);
            // 重置映射关系
            initMd5FileMap();
            // 基于md5整段重写打擂逻辑 现在只需要找到当前未读取的文件 然后从中找出md5最小的 读这个就可以了
            md5FileMap.entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().getStatus() == FileStatus.UNREAD)
                    .min(Map.Entry.comparingByKey())
                    .ifPresentOrElse(entry -> {
                        currentFile = entry.getValue();
                        currentFileNameMd5 = entry.getKey();
                        currentPos = 0L;
                        log.info("Switch file complete, current file: {}, current position: {}",
                                currentFile.getFile().getName(), currentPos);
                    }, () -> {
                        // 如果没有未读取的文件
                        log.info("All files have been read. Waiting for new input.");
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            log.warn("JsonFileReceiver is interrupted", e);
                        }
                    });
        } else {
            log.info("Continuing reading log: {}, current position: {}", currentFile.getFile().getName(), currentPos);
            // 存在逐步写入情况 使用buffer读
            readCurrentFileWithChannel();
        }
    }

    private void readCurrentFileWithChannel() {
        currentFile.setStatus(FileStatus.READING);
        try (FileInputStream fis = new FileInputStream(currentFile.getFile());
             FileChannel channel = fis.getChannel()) {
            ByteBuffer buffer = ByteBuffer.allocate(DEFAULT_CHANNEL_SIZE);
            int bytesRead;
            while ((bytesRead = channel.read(buffer, currentPos)) != -1) {
                buffer.flip();
                byte[] bytes = new byte[bytesRead];
                buffer.get(bytes);
                String line = new String(bytes);
                this.outputCache.put(line);
                this.currentPos += bytesRead;
                String indexLine = currentFileNameMd5 + ":" + currentPos;
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
        super.start();
        log.info("JsonFileReceiver start reading log files");
        startReadFile();
    }

    @Override
    public void flushAndStart() {
        log.info("JsonFileReceiver flush and start reading log files");
        // 重新初始化环境
        outputCache.clear();
        jsonFilePreHandlerExecutor = Executors.newSingleThreadExecutor(
                new BasicThreadFactory.Builder()
                        .namingPattern("Json-file-preHandler-%d").build()
        );
        initMd5FileMap();
        try {
            initParamsWithIndexFile();
        } catch (IOException e) {
            throw new ArktourosException(e);
        }
        // 启动JsonFilePreHandler
        jsonFilePreHandlerExecutor.submit(
                new JsonFilePreHandler(outputCache, fileType,
                        traceQueueService, sinkService,
                        objectMapper, sytelTraceAnalyzerNumber));
        startReadFile();
    }

    private void startReadFile() {
        while (isRunning) {
            try {
                readFile();
            } catch (InterruptedException | IOException e) {
                log.warn("JsonLogFileReceiver is interrupted", e);
                break;
            }
        }
    }

    @Override
    public void stop() {
        log.info("JsonFileReceiver stop reading log files");
        super.stop();
        // JsonFilePreHandler调用了sytel的TraceAnalyzer线程池 需要关闭
        jsonFilePreHandlerExecutor.shutdown();
    }

    @Override
    public void stopAndClean() {
        JsonFilePreHandler.needCleanWhileShutdown = true;
        super.stopAndClean();
        stop();
        // 恢复现场
        JsonFilePreHandler.needCleanWhileShutdown = false;
    }
}
