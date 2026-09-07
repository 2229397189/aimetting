package com.aimeeting.interview.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Jackson 封装工具。
 *
 * <p>关键点：
 * <ul>
 *   <li>单例 {@link ObjectMapper}，注册 {@code JavaTimeModule}，时间按
 *       {@code yyyy-MM-dd HH:mm:ss} 序列化，禁用时间戳输出。</li>
 *   <li>支持 {@link TypeReference}，用于反序列化复杂泛型（如幂等回放结果）。</li>
 *   <li>解析失败返回 {@code null} 或默认值而不是抛异常，避免 AI 输出脏数据打断主流程。</li>
 * </ul>
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonUtil {

    /** 全局共享的 ObjectMapper。 */
    public static final ObjectMapper MAPPER = buildMapper();

    private static ObjectMapper buildMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return mapper;
    }

    /**
     * 对象序列化为 JSON 字符串，失败返回空字符串。
     *
     * @param value 待序列化对象
     * @return JSON 字符串，异常时返回 {@code null}
     */
    public static String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("[JsonUtil] 序列化失败, type={}, err={}", value.getClass().getName(), e.getMessage());
            return null;
        }
    }

    /**
     * 反序列化为目标类型。
     *
     * @param json  JSON 字符串
     * @param type  目标类型
     * @param <T>   目标类型
     * @return 反序列化结果，解析失败返回 {@code null}
     */
    public static <T> T parse(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            log.warn("[JsonUtil] 反序列化失败, type={}, err={}", type.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * 反序列化为复杂泛型类型。
     *
     * @param json  JSON 字符串
     * @param type  {@link TypeReference}，如 {@code new TypeReference<Result<Xxx>>() {}}
     * @param <T>   目标类型
     * @return 反序列化结果，解析失败返回 {@code null}
     */
    public static <T> T parse(String json, TypeReference<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            log.warn("[JsonUtil] 反序列化失败, typeRef={}, err={}", type.getType(), e.getMessage());
            return null;
        }
    }

    /**
     * 解析失败时返回默认值。
     *
     * @param json         JSON 字符串
     * @param type         目标类型
     * @param defaultValue 兜底值
     * @param <T>          目标类型
     * @return 反序列化结果或兜底值
     */
    public static <T> T parseOrDefault(String json, Class<T> type, T defaultValue) {
        T value = parse(json, type);
        return value == null ? defaultValue : value;
    }

    /**
     * 解析失败时用 Supplier 生成兜底值（懒加载）。
     *
     * @param json          JSON 字符串
     * @param type          目标类型
     * @param defaultSupplier 兜底值提供者
     * @param <T>           目标类型
     * @return 反序列化结果或兜底值
     */
    public static <T> T parseOrGet(String json, Class<T> type, Supplier<T> defaultSupplier) {
        T value = parse(json, type);
        return value == null ? defaultSupplier.get() : value;
    }

    /**
     * 深拷贝（经 JSON 中转），拷贝失败返回 {@code null}。
     *
     * @param source 源对象
     * @param type   目标类型
     * @param <T>    目标类型
     * @return 拷贝结果
     */
    public static <T> T deepCopy(T source, Class<T> type) {
        return parse(toJson(source), type);
    }
}
