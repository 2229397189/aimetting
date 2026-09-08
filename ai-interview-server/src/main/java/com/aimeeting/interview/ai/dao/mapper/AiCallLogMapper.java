package com.aimeeting.interview.ai.dao.mapper;

import com.aimeeting.interview.ai.log.AiCallLogDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * AI 调用日志 Mapper（MyBatis-Plus）。
 *
 * <p>随 {@code @MapperScan("com.aimeeting.interview.**.dao.mapper")} 自动装配，
 * 落在 {@code ai.dao.mapper} 子包下以保证被扫描到（否则注入失败）。
 */
public interface AiCallLogMapper extends BaseMapper<AiCallLogDO> {
}
