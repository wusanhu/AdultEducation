package com.adultedu.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 兼容管理端、移动端与第三方调用中常见的日期时间格式：
 * yyyy-MM-dd HH:mm:ss / yyyy-MM-dd'T'HH:mm:ss / yyyy-MM-dd HH:mm / yyyy-MM-dd
 */
public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
    private static final DateTimeFormatter SPACE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String text = parser.getValueAsString();
        if (text == null || text.isBlank()) {
            return null;
        }
        String value = text.trim();
        if (value.length() >= 19 && (value.charAt(10) == 'T' || value.charAt(10) == ' ')) {
            value = value.substring(0, 10) + " " + value.substring(11);
        }
        if (value.length() > 19) {
            value = value.substring(0, 19);
        }
        try {
            return LocalDateTime.parse(value, SPACE);
        } catch (Exception ignored) {
            // try other supported formats
        }
        try {
            return LocalDateTime.parse(value, MINUTE);
        } catch (Exception ignored) {
            // try date only
        }
        try {
            return LocalDate.parse(value).atStartOfDay();
        } catch (Exception e) {
            throw new IOException("日期时间格式不正确，应为 yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd'T'HH:mm:ss");
        }
    }
}
