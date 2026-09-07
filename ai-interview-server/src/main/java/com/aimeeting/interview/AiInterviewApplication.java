package com.aimeeting.interview;

import com.baomidou.mybatisplus.annotation.DbType;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * AI 在线模拟面试平台 - 后端启动类。
 *
 * <p>职责与关键设计：
 * <ul>
 *   <li>{@code @MapperScan("com.aimeeting.interview.**.dao.mapper")}：按架构约定统一扫描各域
 *       {@code dao.mapper} 包，避免每个 Mapper 单独加 {@code @Mapper}。</li>
 *   <li>{@code @EnableAsync}：开启异步执行能力，用于 idempotent 审计落库、AI 调用日志等旁路写。</li>
 *   <li>{@code @SpringBootApplication}：自动装配 Web / MyBatis-Plus / Caffeine 等基础设施。</li>
 * </ul>
 */
@SpringBootApplication
@EnableAsync
@MapperScan("com.aimeeting.interview.**.dao.mapper")
public class AiInterviewApplication {

    /** 当前应用使用的默认数据库方言（分页插件使用；H2 以 MySQL 兼容模式运行）。 */
    public static final DbType DEFAULT_DB_TYPE = DbType.MYSQL;

    public static void main(String[] args) {
        SpringApplication.run(AiInterviewApplication.class, args);
    }
}
