package com.aimeeting.interview.config;

import com.aimeeting.interview.AiInterviewApplication;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置：分页插件。
 *
 * <p>分页方言统一使用 MySQL；H2 以 {@code MODE=MySQL} 运行，同样支持
 * {@code LIMIT ?, ?} 语法，因此一套配置覆盖两个 profile。
 */
@Configuration
public class MybatisPlusConfig {

    /** 单页最大条数，防止大分页拖垮数据库。 */
    private static final long MAX_LIMIT = 500L;

    /**
     * MyBatis-Plus 插件链。
     *
     * @return 拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor paginationInterceptor =
                new PaginationInnerInterceptor(AiInterviewApplication.DEFAULT_DB_TYPE);
        paginationInterceptor.setMaxLimit(MAX_LIMIT);
        paginationInterceptor.setOverflow(false);
        interceptor.addInnerInterceptor(paginationInterceptor);
        return interceptor;
    }
}
