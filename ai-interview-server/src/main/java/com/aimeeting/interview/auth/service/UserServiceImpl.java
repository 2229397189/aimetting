package com.aimeeting.interview.auth.service;

import com.aimeeting.interview.auth.api.io.req.ChangePasswordReq;
import com.aimeeting.interview.auth.api.io.req.UpdateProfileReq;
import com.aimeeting.interview.auth.api.io.resp.UserProfileResp;
import com.aimeeting.interview.auth.api.io.resp.UserStatsResp;
import com.aimeeting.interview.auth.dao.entity.UserDO;
import com.aimeeting.interview.auth.dao.entity.UserProfileDO;
import com.aimeeting.interview.auth.dao.mapper.StatsMapper;
import com.aimeeting.interview.auth.dao.repository.UserProfileRepository;
import com.aimeeting.interview.auth.dao.repository.UserRepository;
import com.aimeeting.interview.auth.domain.PasswordPolicy;
import com.aimeeting.interview.common.convention.errorcode.BaseErrorCode;
import com.aimeeting.interview.common.convention.exception.ClientException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现：资料读写（U-06）、修改密码（U-07）、数据概览（U-08）。
 *
 * <p>资料表与用户表为 1:1，查询时做「缺失即补」处理，保证
 * {@code GET /api/user/profile} 永远返回完整结构。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /** 趋势展示天数。 */
    private static final int TREND_DAYS = 7;

    private final UserRepository userRepository;

    private final UserProfileRepository userProfileRepository;

    private final StatsMapper statsMapper;

    @Override
    public void initProfile(Long userId) {
        if (userId == null || userProfileRepository.findByUserId(userId) != null) {
            return;
        }
        UserProfileDO profile = new UserProfileDO();
        profile.setUserId(userId);
        profile.setWorkYears(0);
        userProfileRepository.insert(profile);
    }

    @Override
    public UserProfileResp getProfileResp(Long userId) {
        UserDO user = requireUser(userId);
        UserProfileDO profile = userProfileRepository.findByUserId(userId);
        if (profile == null) {
            initProfile(userId);
            profile = userProfileRepository.findByUserId(userId);
        }
        return assemble(user, profile);
    }

    @Override
    public UserProfileResp updateProfile(Long userId, UpdateProfileReq req) {
        UserDO user = requireUser(userId);

        // 只更新非空字段，便于前端局部保存
        UserDO userUpdate = new UserDO();
        userUpdate.setId(userId);
        boolean needUpdateUser = false;
        if (req.getNickname() != null && !req.getNickname().isBlank()) {
            userUpdate.setNickname(req.getNickname().trim());
            needUpdateUser = true;
        }
        if (req.getAvatar() != null && !req.getAvatar().isBlank()) {
            userUpdate.setAvatar(req.getAvatar().trim());
            needUpdateUser = true;
        }
        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            String email = req.getEmail().trim();
            if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
                throw new ClientException("邮箱已被占用", BaseErrorCode.USER_EXIST);
            }
            userUpdate.setEmail(email);
            needUpdateUser = true;
        }
        if (needUpdateUser) {
            userRepository.updateById(userUpdate);
        }

        // 资料表 upsert
        UserProfileDO profile = userProfileRepository.findByUserId(userId);
        if (profile == null) {
            initProfile(userId);
            profile = userProfileRepository.findByUserId(userId);
        }
        UserProfileDO profileUpdate = new UserProfileDO();
        profileUpdate.setId(profile == null ? null : profile.getId());
        profileUpdate.setUserId(userId);
        if (req.getTargetPosition() != null) {
            profileUpdate.setTargetPosition(req.getTargetPosition().trim());
        }
        if (req.getWorkYears() != null) {
            profileUpdate.setWorkYears(req.getWorkYears());
        }
        if (req.getIntro() != null) {
            profileUpdate.setIntro(req.getIntro().trim());
        }
        if (req.getPhone() != null) {
            profileUpdate.setPhone(req.getPhone().trim());
        }
        if (profileUpdate.getId() == null) {
            profileUpdate.setWorkYears(profileUpdate.getWorkYears() == null ? 0 : profileUpdate.getWorkYears());
            userProfileRepository.insert(profileUpdate);
        } else {
            userProfileRepository.updateById(profileUpdate);
        }

        log.info("[User] 资料更新成功, userId={}", userId);
        return getProfileResp(userId);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordReq req) {
        UserDO user = requireUser(userId);
        if (!PasswordPolicy.matches(req.getOldPassword(), user.getPasswordHash())) {
            throw new ClientException("原密码不正确", BaseErrorCode.PASSWORD_ERROR);
        }
        PasswordPolicy.validate(req.getNewPassword());
        if (PasswordPolicy.matches(req.getNewPassword(), user.getPasswordHash())) {
            throw new ClientException("新密码不能与原密码相同", BaseErrorCode.PARAM_ERROR);
        }
        UserDO update = new UserDO();
        update.setId(userId);
        update.setPasswordHash(PasswordPolicy.encode(req.getNewPassword()));
        userRepository.updateById(update);
        log.info("[User] 密码修改成功, userId={}", userId);
    }

    @Override
    public UserStatsResp getStats(Long userId) {
        requireUser(userId);

        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusDays(TREND_DAYS - 1).atStartOfDay();

        Long totalSessions = statsMapper.countSessions(userId);
        Long completedSessions = statsMapper.countCompletedSessions(userId);
        BigDecimal averageScore = statsMapper.averageScore(userId).setScale(1, RoundingMode.HALF_UP);
        Long totalQuestions = statsMapper.countAnswers(userId);
        Long resumeCount = statsMapper.countResumes(userId);

        Map<String, StatsMapper.TrendRow> rowMap = statsMapper.dailyTrend(userId, start).stream()
                .collect(Collectors.toMap(r -> r.day, r -> r));
        List<UserStatsResp.TrendPoint> trend = new ArrayList<>(TREND_DAYS);
        for (int i = TREND_DAYS - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String day = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            StatsMapper.TrendRow row = rowMap.get(day);
            Long count = row == null || row.cnt == null ? 0L : row.cnt;
            BigDecimal score = row == null || row.avgScore == null
                    ? BigDecimal.ZERO : row.avgScore.setScale(1, RoundingMode.HALF_UP);
            trend.add(UserStatsResp.TrendPoint.builder()
                    .date(day)
                    .count(count)
                    .score(score)
                    .build());
        }

        return UserStatsResp.builder()
                .totalSessions(totalSessions)
                .completedSessions(completedSessions)
                .averageScore(averageScore)
                .totalQuestions(totalQuestions)
                .resumeCount(resumeCount)
                .trend(trend)
                .build();
    }

    /**
     * 校验用户存在性，不存在抛业务异常。
     *
     * @param userId 用户 ID
     * @return 用户 DO
     */
    private UserDO requireUser(Long userId) {
        UserDO user = userRepository.findById(userId);
        if (user == null) {
            throw new ClientException("用户不存在", BaseErrorCode.PARAM_ERROR);
        }
        return user;
    }

    /**
     * 装配资料返回体。
     *
     * @param user    用户 DO
     * @param profile 资料 DO，允许为 null
     * @return 资料返回体
     */
    private UserProfileResp assemble(UserDO user, UserProfileDO profile) {
        UserProfileResp.UserProfileRespBuilder builder = UserProfileResp.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .email(user.getEmail())
                .role(user.getRole())
                .lastLoginAt(user.getLastLoginAt())
                .createTime(user.getCreateTime());
        if (profile != null) {
            builder.targetPosition(profile.getTargetPosition())
                    .workYears(profile.getWorkYears() == null ? 0 : profile.getWorkYears())
                    .intro(profile.getIntro())
                    .phone(profile.getPhone());
        } else {
            builder.workYears(0);
        }
        return builder.build();
    }
}
