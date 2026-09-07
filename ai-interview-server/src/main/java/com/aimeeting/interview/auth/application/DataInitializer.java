package com.aimeeting.interview.auth.application;

import com.aimeeting.interview.auth.dao.entity.UserDO;
import com.aimeeting.interview.auth.dao.entity.UserProfileDO;
import com.aimeeting.interview.auth.dao.repository.UserProfileRepository;
import com.aimeeting.interview.auth.dao.repository.UserRepository;
import com.aimeeting.interview.auth.domain.PasswordPolicy;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 启动数据初始化：用户表为空时插入管理员与演示账号，保证冷启动即可登录演示。
 *
 * <p>账号：
 * <ul>
 *   <li>{@code admin / admin123}（role=ADMIN）</li>
 *   <li>{@code demo / demo1234}（role=USER）</li>
 * </ul>
 *
 * <p>幂等：仅当 {@code t_user} 无任何记录时执行；密码一律 BCrypt 加密后落库。
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    /** 管理员账号。 */
    public static final String ADMIN_USERNAME = "admin";

    /** 管理员初始密码。 */
    public static final String ADMIN_PASSWORD = "admin123";

    /** 演示账号。 */
    public static final String DEMO_USERNAME = "demo";

    /** 演示账号初始密码。 */
    public static final String DEMO_PASSWORD = "demo1234";

    private final UserRepository userRepository;

    private final UserProfileRepository userProfileRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.countAll() > 0L) {
            log.info("[DataInit] 用户表非空，跳过初始化");
            return;
        }
        createUser(ADMIN_USERNAME, ADMIN_PASSWORD, "ADMIN", "平台管理员", "admin@aimeeting.local");
        createUser(DEMO_USERNAME, DEMO_PASSWORD, "USER", "演示用户", "demo@aimeeting.local");
        log.info("[DataInit] 初始化账号完成: {}/{} (ADMIN), {}/{} (USER)",
                ADMIN_USERNAME, ADMIN_PASSWORD, DEMO_USERNAME, DEMO_PASSWORD);
    }

    /**
     * 创建账号并初始化资料。
     *
     * @param username 用户名
     * @param rawPassword 明文密码（仅初始化时使用，落库为 BCrypt）
     * @param role     角色
     * @param nickname 昵称
     * @param email    邮箱
     */
    private void createUser(String username, String rawPassword, String role,
                            String nickname, String email) {
        UserDO user = new UserDO();
        user.setUsername(username);
        user.setPasswordHash(PasswordPolicy.encode(rawPassword));
        user.setNickname(nickname);
        user.setEmail(email);
        user.setRole(role);
        user.setStatus(1);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.insert(user);
        userProfileRepository.insert(buildProfile(user.getId(), role));
    }

    private UserProfileDO buildProfile(Long userId, String role) {
        UserProfileDO profile = new UserProfileDO();
        profile.setUserId(userId);
        profile.setWorkYears("ADMIN".equals(role) ? 10 : 1);
        profile.setTargetPosition("ADMIN".equals(role) ? "技术负责人" : "Java 后端工程师");
        profile.setIntro("ADMIN".equals(role) ? "平台内置管理员账号" : "平台内置演示账号，可用于快速体验完整流程");
        return profile;
    }
}
