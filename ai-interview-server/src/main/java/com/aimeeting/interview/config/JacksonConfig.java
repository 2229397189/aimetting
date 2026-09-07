package com.aimeeting.interview.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 全局配置。
 *
 * <p>约定：
 * <ul>
 *   <li>时间统一 {@code yyyy-MM-dd HH:mm:ss}（前端与 DB 语义一致），禁用时间戳输出。</li>
 *   <li>忽略未知字段，容忍前端多传字段。</li>
 *   <li>保留 null 字段（{@code default-property-inclusion: always}），
 *       保证统一返回体 4 个字段始终出现，便于前端与脚本断言。</li>
 * </ul>
 */
@Configuration
public class JacksonConfig {

    /** 全局时间格式。 */
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 定制 Spring 管理的 ObjectMapper。
     *
     * @return 定制器
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        return builder -> builder
                .modules(new JavaTimeModule()
                        .addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter))
                        .addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter)))
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .featuresToDisable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }
}
