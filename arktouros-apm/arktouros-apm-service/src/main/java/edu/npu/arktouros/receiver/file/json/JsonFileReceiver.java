package edu.npu.arktouros.receiver.file.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.npu.arktouros.model.exception.ArktourosException;
import edu.npu.arktouros.receiver.DataReceiver;
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

    private final String logDirName;
    private final File logDirFile;
    // 我们默认所有文件的创建时间不同
    private Map<Long, File> createTimeFileMap;
    private File currentFile;
    private Long currentFileCreateTime;
    private Long currentPos;
    private final String indexFilePath;
    private final File indexFile;
    private final String fileType;
    private final TraceQueueService traceQueueService;
    private final SinkService sinkService;
    private final ObjectMapper objectMapper;
    private final int sytelTraceAnalyzerNumber;
    private static final int DEFAULT_CAPACITY = 1000;
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
        this.logDirName = logDir;
        this.logDirFile = new File(logDir);
        this.indexFilePath = indexFilePath;
        this.indexFile = new File(indexFilePath);
        this.fileType = fileType;
        this.traceQueueService = traceQueueService;
        this.sinkService = sinkService;
        this.objectMapper = objectMapper;
        this.sytelTraceAnalyzerNumber = sytelTraceAnalyzerNumber;

        initCreateTimeFileMap();
        try {
            initParamsWithIndex();
        } catch (IOException e) {
            throw new ArktourosException(e);
        }
        // 启动JsonFilePreHandler
        jsonFilePreHandlerExecutor.submit(
                new JsonFilePreHandler(outputCache, fileType, logDirFile,
                        traceQueueService, sinkService, objectMapper,
                        sytelTraceAnalyzerNumber));
    }

    public void initParamsWithIndex() throws IOException {
        // 去logs目录下检索是否有index文件
        // 在执行rename的时候中断程序会出现问题
        File tmpIndexFile = new File(indexFile.toPath() + ".tmp");
        if (tmpIndexFile.exists()) {
            // 优先级更高 用tmpIndexFile的内容覆盖indexFile
            this.indexFile.createNewFile();
            FileUtils.copyFile(tmpIndexFile, indexFile);
            FileUtils.delete(tmpIndexFile);
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
            if (currentPos != currentFile.length()) {
                // 从头开始
                currentPos = 0L;
            }
        } else {
            initEmptyIndexFile();
        }
    }

    /**
     * 不存在索引文件 初始化一个空的索引文件 写入字典序 最小的文件名md5:0
     */
    private void initEmptyIndexFile() {
        try {
            boolean newFile = indexFile.createNewFile();
            if (!newFile) {
                log.warn("Index file already exists");
            }
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
                                FileUtils.delete(tmpIndex);
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

    public void initCreateTimeFileMap() {
        this.createTimeFileMap = new HashMap<>();
        File[] files = logDirFile.listFiles();
        if (files != null) {
            for (File file : files) {
                // 需要排除索引文件
                if (!indexFilePath.toLowerCase(Locale.ROOT).contains(file.getName())) {
                    try {
                        long createTime = encodeFileNameToNumber(file.getName());
                        // 这个是天脉目前的授时问题 所以不得不这样来进行 原来时间戳的方案在没有授时节点的情况下是不可行的
                        log.info("Encode file: {} to md5 number: {}", file.getName(), createTime);
                        this.createTimeFileMap.put(createTime, file);
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

    private void readFile() throws InterruptedException {
        log.info("Continuing reading log: {}, current position: {}", currentFile.getName(), currentPos);
        // 存在逐步写入情况 使用buffer读
        readCurrentFileWithChannel();
        // 如果当前文件读完 尝试切换
        if (currentPos == currentFile.length()) {
            log.info("Read complete, start switching, current file: {}, current position: {}",
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
                log.info("Switch to file: {}, current position: {}", currentFile.getName(), currentPos);
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
        initCreateTimeFileMap();
        try {
            initParamsWithIndex();
        } catch (IOException e) {
            throw new ArktourosException(e);
        }
        // 启动JsonFilePreHandler
        jsonFilePreHandlerExecutor.submit(
                new JsonFilePreHandler(outputCache, fileType, logDirFile,
                        traceQueueService, sinkService,
                        objectMapper, sytelTraceAnalyzerNumber));
        startReadFile();
    }

    private void startReadFile() {
        while (isRunning) {
            try {
                readFile();
            } catch (InterruptedException e) {
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
        // 恢复现场
        JsonFilePreHandler.needCleanWhileShutdown = false;
    }
}
