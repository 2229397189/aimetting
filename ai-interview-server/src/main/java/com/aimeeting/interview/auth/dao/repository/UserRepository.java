package com.aimeeting.interview.auth.dao.repository;

import com.aimeeting.interview.auth.dao.entity.UserDO;
import com.aimeeting.interview.auth.dao.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * 用户仓储：封装 {@code t_user} 的全部数据访问细节（LambdaQueryWrapper）。
 *
 * <p>分层约束：application 层只能依赖本类，不得直接使用 Mapper；
 * 逻辑删除由 MyBatis-Plus 全局配置自动追加，本类不感知。
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final UserMapper userMapper;

    /**
     * 按主键查询。
     *
     * @param id 用户 ID
     * @return 用户 DO，不存在返回 null
     */
    public UserDO findById(Long id) {
        if (id == null) {
            return null;
        }
        return userMapper.selectById(id);
    }

    /**
     * 按用户名精确查询。
     *
     * @param username 用户名
     * @return 用户 DO，不存在返回 null
     */
    public UserDO findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return userMapper.selectOne(new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getUsername, username.trim())
                .last("LIMIT 1"));
    }

    /**
     * 按邮箱精确查询。
     *
     * @param email 邮箱
     * @return 用户 DO，不存在返回 null
     */
    public UserDO findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return userMapper.selectOne(new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getEmail, email.trim())
                .last("LIMIT 1"));
    }

    /**
     * 按「用户名或邮箱」查询，支持登录双通道（U-02）。
     *
     * @param account 用户名或邮箱
     * @return 用户 DO，不存在返回 null
     */
    public UserDO findByUsernameOrEmail(String account) {
        if (account == null || account.isBlank()) {
            return null;
        }
        String keyword = account.trim();
        return userMapper.selectOne(new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getUsername, keyword)
                .or()
                .eq(UserDO::getEmail, keyword)
                .last("LIMIT 1"));
    }

    /**
     * 判断用户名是否已存在。
     *
     * @param username 用户名
     * @return 存在返回 true
     */
    public boolean existsByUsername(String username) {
        return countByUsername(username) > 0L;
    }

    /**
     * 判断邮箱是否已被占用（邮箱为空时直接返回 false）。
     *
     * @param email 邮箱
     * @return 已占用返回 true
     */
    public boolean existsByEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return userMapper.selectCount(new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getEmail, email.trim())) > 0L;
    }

    /**
     * 统计同名用户数量（正常情况下应为 0 或 1）。
     *
     * @param username 用户名
     * @return 数量
     */
    public long countByUsername(String username) {
        if (username == null || username.isBlank()) {
            return 0L;
        }
        return userMapper.selectCount(new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getUsername, username.trim()));
    }

    /**
     * 统计全量用户数（排除逻辑删除）。
     *
     * @return 用户总数
     */
    public long countAll() {
        return userMapper.selectCount(null);
    }

    /**
     * 新增用户。
     *
     * @param user 用户 DO
     * @return 影响行数
     */
    public int insert(UserDO user) {
        return userMapper.insert(user);
    }

    /**
     * 更新用户（null 字段不更新）。
     *
     * @param user 用户 DO
     * @return 影响行数
     */
    public int updateById(UserDO user) {
        return userMapper.updateById(user);
    }

    /**
     * 更新最近登录时间。
     *
     * @param userId 用户 ID
     * @param loginTime 登录时间
     * @return 影响行数
     */
    public int updateLastLoginAt(Long userId, LocalDateTime loginTime) {
        UserDO update = new UserDO();
        update.setId(userId);
        update.setLastLoginAt(loginTime);
        return userMapper.updateById(update);
    }

    /**
     * 分页查询用户列表（管理端 M8 使用）。
     *
     * @param offset 偏移量
     * @param limit  条数
     * @return 用户列表
     */
    public List<UserDO> page(long offset, long limit) {
        return userMapper.selectList(new LambdaQueryWrapper<UserDO>()
                .orderByDesc(UserDO::getCreateTime)
                .last("LIMIT " + Math.max(0L, offset) + "," + Math.max(1L, limit)));
    }
}
