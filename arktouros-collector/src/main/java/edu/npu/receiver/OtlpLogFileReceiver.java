package edu.npu.receiver;

import edu.npu.cache.AbstractCache;
import edu.npu.properties.PropertiesProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.BufferedInputStream;
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
import java.util.*;

/**
 * @author : [wangminan]
 * @description : otlp日志文件接收器
 */
@Slf4j
public class OtlpLogFileReceiver extends AbstractReceiver {
    private String LOG_DIR;

    private static File logs;

    // 我们默认所有文件的创建时间不同
    private static Map<Long, File> createTimeFileMap;

    private static File currentFile;

    private static Long currentFileCreateTime;

    private static Long currentPos;

    private static File indexFile;

    public OtlpLogFileReceiver(AbstractCache outputCache) {
        super(outputCache);
    }

    @Override
    public void run() {
        log.info("this is OtlpLogFileReceiver, start working");
        try {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        prepare();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    if (currentFile != null) {
                        this.cancel();
                    } else {
                        log.info("No log file found. Waiting for new input.");
                    }
                }
            }, 0, 2000);

            while (true) {
                readFile();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void readFile() throws InterruptedException {
        log.info("start reading log: {}, current position: {}",
                currentFile.getName(), currentPos);
        // 存在逐步写入情况 使用buffer读
        readCurrentFileWithChannel();
        // readCurrentFileWithStream();
        log.info("read complete, start switching, current file: {}, current position: {}",
                currentFile.getName(), currentPos);
        // 如果当前文件读完 确认是否切换
        if (currentPos == currentFile.length()) {
            // 重置映射关系
            initCreateTimeFileMap();
            readCurrentFileWithStream();
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
                log.info("All files have been read. Waiting for new input.");
                Thread.sleep(2000);
            }
        }
        log.info("switch to file: {}, current position: {}", currentFile.getName(), currentPos);
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

    private static void readCurrentFileWithChannel() {
        try (FileInputStream fis = new FileInputStream(currentFile);
             FileChannel channel = fis.getChannel()) {
            ByteBuffer buffer = ByteBuffer.allocate(8 * 1024 * 1024);
            int bytesRead;
            while ((bytesRead = channel.read(buffer, currentPos)) != -1) {
                buffer.flip();
                byte[] bytes = new byte[bytesRead];
                buffer.get(bytes);
                String line = new String(bytes);
                // System.out.print(line);
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
        LOG_DIR =
                PropertiesProvider.getProperty("receiver.otlpLogFileReceiver.logDir");
        if (LOG_DIR == null || LOG_DIR.isEmpty()) {
            throw new IllegalArgumentException("log dir in config file is empty");
        }
        logs = new File(LOG_DIR);
        if (!logs.exists()) {
            throw new RuntimeException("logs dic " + LOG_DIR +  " does not exists");
        }
        initCreateTimeFileMap();
        initParamsWithIndex();
        log.info("OtlpLogFileReceiver prepare complete");
    }

    public void initParamsWithIndex() throws IOException {
        // 去logs目录下检索是否有index文件
        indexFile = new File(LOG_DIR + "logs.index");
        if (indexFile.exists()) {
            String line;
            try {
                line = FileUtils.readLines(indexFile, StandardCharsets.UTF_8).get(0);
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
            // 初始化索引文件与变量
            try {
                boolean newFile = indexFile.createNewFile();
                if (!newFile) {
                    throw new RuntimeException("create index file failed");
                }
                createTimeFileMap.entrySet()
                        .stream()
                        .min(Map.Entry.comparingByKey())
                        .ifPresentOrElse(entry -> {
                            currentFile = entry.getValue();
                            currentPos = 0L;
                            currentFileCreateTime = entry.getKey();
                            List<String> line = List.of(currentFileCreateTime + ":" + currentPos);
                            File tmpIndex = new File(LOG_DIR + "logs.index.tmp");
                            try {
                                tmpIndex.createNewFile();
                                Files.write(tmpIndex.toPath(), line,
                                        StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
                                tmpIndex.renameTo(indexFile);
                            } catch (IOException e) {
                                throw new RuntimeException("failed while writing index file", e);
                            }
                        }, () -> {
                            throw new RuntimeException("the log dir is empty");
                        });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void initCreateTimeFileMap() {
        createTimeFileMap = new HashMap<>();
        File[] files = logs.listFiles();
        if (files != null) {
            for (File file : files) {
                // 需要排除索引文件
                if (!file.getName().equals("logs.index")) {
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
}
