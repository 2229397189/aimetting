package com.aimeeting.interview.auth.dao.mapper;

import com.aimeeting.interview.auth.dao.entity.UserProfileDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@code t_user_profile} 数据访问接口。
 */
@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfileDO> {
}
