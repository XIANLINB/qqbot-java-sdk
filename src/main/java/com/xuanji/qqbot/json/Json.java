package com.xuanji.qqbot.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.xuanji.qqbot.exception.QqBotException;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * 共享 Jackson 映射器。
 * <p>
 * 官方字段为 snake_case，模型通过 {@code @JsonProperty} 对齐；
 * 忽略未知字段以兼容官方后续扩展。
 */
public final class Json {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private Json() {
    }

    /**
     * @return 全局 ObjectMapper 单例
     */
    public static ObjectMapper mapper() {
        return MAPPER;
    }

    /**
     * 序列化为 JSON 字符串。
     *
     * @param value 任意对象
     * @return JSON 文本
     */
    public static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (IOException e) {
            throw new UncheckedIOException("JSON 序列化失败", e);
        }
    }

    /**
     * 序列化为 UTF-8 字节。
     *
     * @param value 任意对象
     * @return JSON 字节
     */
    public static byte[] writeBytes(Object value) {
        try {
            return MAPPER.writeValueAsBytes(value);
        } catch (IOException e) {
            throw new UncheckedIOException("JSON 序列化失败", e);
        }
    }

    /**
     * 从 JSON 字符串反序列化。
     *
     * @param json JSON 文本
     * @param type 目标类型
     * @return 对象实例
     */
    public static <T> T read(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (IOException e) {
            throw new QqBotException("JSON 反序列化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从 JSON 字节反序列化。
     *
     * @param json JSON 字节（通常 UTF-8）
     * @param type 目标类型
     * @return 对象实例
     */
    public static <T> T read(byte[] json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (IOException e) {
            throw new QqBotException("JSON 反序列化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从 JSON 字符串反序列化为泛型类型。
     *
     * @param json JSON 文本
     * @param type 类型引用，如 {@code new TypeReference<List<X>>(){}}
     * @return 对象实例
     */
    public static <T> T read(String json, TypeReference<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (IOException e) {
            throw new QqBotException("JSON 反序列化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从 JSON 字节反序列化为泛型类型。
     *
     * @param json JSON 字节
     * @param type 类型引用
     * @return 对象实例
     */
    public static <T> T read(byte[] json, TypeReference<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (IOException e) {
            throw new QqBotException("JSON 反序列化失败: " + e.getMessage(), e);
        }
    }
}
