package com.aimeeting.interview.auth.dao.repository;

import com.aimeeting.interview.auth.dao.entity.UserProfileDO;
import com.aimeeting.interview.auth.dao.mapper.UserProfileMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * 用户资料仓储：封装 {@code t_user_profile} 的数据访问。
 *
 * <p>与 {@link UserRepository} 同属 dao 层，保证 application 层不直接接触 Mapper。
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UserProfileRepository {

    private final UserProfileMapper userProfileMapper;

    /**
     * 按用户 ID 查询资料。
     *
     * @param userId 用户 ID
     * @return 资料 DO，不存在返回 null
     */
    public UserProfileDO findByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfileDO>()
                .eq(UserProfileDO::getUserId, userId)
                .last("LIMIT 1"));
    }

    /**
     * 新增资料。
     *
     * @param profile 资料 DO
     * @return 影响行数
     */
    public int insert(UserProfileDO profile) {
        return userProfileMapper.insert(profile);
    }

    /**
     * 更新资料（null 字段不更新）。
     *
     * @param profile 资料 DO
     * @return 影响行数
     */
    public int updateById(UserProfileDO profile) {
        return userProfileMapper.updateById(profile);
    }

    /**
     * 以 user_id 为条件更新资料（避免越权写入他人资料）。
     *
     * @param profile 资料 DO（必须带 userId 与 id）
     * @return 影响行数
     */
    public int updateByUserId(UserProfileDO profile) {
        LambdaQueryWrapper<UserProfileDO> wrapper = new LambdaQueryWrapper<UserProfileDO>()
                .eq(UserProfileDO::getUserId, profile.getUserId());
        return userProfileMapper.update(profile, wrapper);
    }
}
