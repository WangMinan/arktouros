package edu.npu.arktouros.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import edu.npu.arktouros.model.exception.ArktourosException;
import org.apache.commons.lang3.time.DateUtils;

import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * LocalDateTime 反序列化器
 * <p>
 * 说明:
 * 1. 借助commons工具类进行转换
 *
 * @author MoCha-WangMinan
 * @date 2019/11/30
 */
public class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String dateStr = parser.getText().replace("T", " ");
        Date date;
        try {
            date = DateUtils.parseDate(dateStr,
                    "yyyy-MM-dd",
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd HH:mm:ss.SSS"
            );
        } catch (ParseException ex) {
            throw new ArktourosException(ex, "Wrong date format");
        }

        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    @Override
    public Class<?> handledType() {
        // 关键
        return LocalDateTime.class;
    }
}
