package com.campushub.admin;

import com.campushub.auth.entity.AuthUser;
import com.campushub.auth.entity.Role;
import com.campushub.auth.repository.AuthUserRepository;
import com.campushub.common.enums.VerifyStatus;
import com.campushub.common.util.AesUtil;
import com.campushub.user.entity.UserProfile;
import com.campushub.user.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 启动时幂等创建内置「超级管理员」。
 *
 * <p>为什么用 Java runner 而不是 SQL seed：手机号必须 HMAC + AES 加密落库
 * （phone_hmac / phone_cipher），密码必须 BCrypt，这些都依赖运行期密钥，
 * 无法在迁移 SQL 里预先算好。
 *
 * <p>默认账号：phone=「admin」、密码=「admin123」、role=SUPER_ADMIN、认证态=APPROVED。
 * 已存在（按 phone_hmac 命中）则跳过，不覆盖密码/角色。
 * 可通过 campushub.admin.bootstrap.* 配置或关闭。
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AuthUserRepository userRepo;
    private final UserProfileRepository profileRepo;
    private final PasswordEncoder passwordEncoder;
    private final AesUtil aes;

    private final boolean enabled;
    private final String phone;
    private final String password;
    private final String nickname;

    public AdminBootstrap(AuthUserRepository userRepo,
                          UserProfileRepository profileRepo,
                          PasswordEncoder passwordEncoder,
                          AesUtil aes,
                          @Value("${campushub.admin.bootstrap.enabled:true}") boolean enabled,
                          @Value("${campushub.admin.bootstrap.phone:admin}") String phone,
                          @Value("${campushub.admin.bootstrap.password:admin123}") String password,
                          @Value("${campushub.admin.bootstrap.nickname:超级管理员}") String nickname) {
        this.userRepo = userRepo;
        this.profileRepo = profileRepo;
        this.passwordEncoder = passwordEncoder;
        this.aes = aes;
        this.enabled = enabled;
        this.phone = phone;
        this.password = password;
        this.nickname = nickname;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) return;

        String hmac = aes.hmacIndex(phone);
        if (userRepo.findByPhoneHmac(hmac).isPresent()) {
            return; // 已存在，幂等跳过（不覆盖既有密码/角色）
        }

        AuthUser admin = new AuthUser(hmac, aes.encrypt(phone));
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setVerifyStatus(VerifyStatus.APPROVED);
        admin.setRole(Role.SUPER_ADMIN);
        AuthUser saved = userRepo.save(admin);
        profileRepo.save(new UserProfile(saved.getId(), nickname));
        log.info("已创建内置超级管理员 userId={}（phone 标识={}）", saved.getId(), phone);
    }
}
