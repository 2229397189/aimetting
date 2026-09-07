package com.aimeeting.interview.auth.dao.mapper;

import com.aimeeting.interview.auth.dao.entity.UserDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@code t_user} 数据访问接口。
 *
 * <p>由启动类 {@code @MapperScan("com.aimeeting.interview.**.dao.mapper")} 统一扫描注册。
 */
@Mapper
public interface UserMapper extends BaseMapper<UserDO> {
}
