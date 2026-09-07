package com.aimeeting.interview.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/**
 * MyBatis-Plus 自动填充处理器：统一维护 {@code create_time} / {@code update_time}。
 *
 * <p>使用 {@code strictXxxFill} 严格模式：仅当字段类型为 {@link LocalDateTime}
 * 且当前值为 null 时填充，避免覆盖业务显式写入的时间。
 */
@Slf4j
@Component
public class MetaObjectHandlerImpl implements MetaObjectHandler {

    /** 创建时间字段名（DO 驼峰）。 */
    private static final String CREATE_TIME = "createTime";

    /** 更新时间字段名（DO 驼峰）。 */
    private static final String UPDATE_TIME = "updateTime";

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, CREATE_TIME, LocalDateTime.class, now);
        this.strictInsertFill(metaObject, UPDATE_TIME, LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, UPDATE_TIME, LocalDateTime.class, LocalDateTime.now());
    }
}
