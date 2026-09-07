package com.aimeeting.interview.auth.service;

import com.aimeeting.interview.auth.api.io.req.ChangePasswordReq;
import com.aimeeting.interview.auth.api.io.req.UpdateProfileReq;
import com.aimeeting.interview.auth.api.io.resp.UserProfileResp;
import com.aimeeting.interview.auth.api.io.resp.UserStatsResp;

/**
 * 用户服务：资料读写、修改密码、个人数据概览。
 */
public interface UserService {

    /**
     * 为新用户初始化空资料记录。
     *
     * @param userId 用户 ID
     */
    void initProfile(Long userId);

    /**
     * 查询用户资料（合并 user + profile）。
     *
     * @param userId 用户 ID
     * @return 资料返回体
     */
    UserProfileResp getProfileResp(Long userId);

    /**
     * 修改用户资料（仅更新非空字段）。
     *
     * @param userId 用户 ID
     * @param req    修改请求
     * @return 修改后的资料
     */
    UserProfileResp updateProfile(Long userId, UpdateProfileReq req);

    /**
     * 修改密码：校验原密码后更新，并使当前令牌失效提示重新登录（由调用方处理黑名单）。
     *
     * @param userId 用户 ID
     * @param req    修改密码请求
     */
    void changePassword(Long userId, ChangePasswordReq req);

    /**
     * 个人数据概览。
     *
     * @param userId 用户 ID
     * @return 统计信息
     */
    UserStatsResp getStats(Long userId);
}
