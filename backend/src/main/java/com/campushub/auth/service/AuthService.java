package com.campushub.auth.service;

import com.campushub.auth.entity.AuthUser;
import com.campushub.auth.entity.SmsScene;
import com.campushub.auth.exception.AuthErrorCode;
import com.campushub.auth.repository.AuthUserRepository;
import com.campushub.auth.vo.TokenPairVO;
import com.campushub.common.exception.BizException;
import com.campushub.common.response.ResponseCode;
import com.campushub.common.util.AesUtil;
import com.campushub.common.util.JwtUtil;
import com.campushub.common.util.PhoneMaskUtil;
import com.campushub.config.JwtProperties;
import com.campushub.user.entity.UserProfile;
import com.campushub.user.repository.UserProfileRepository;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * 鉴权核心编排（AUTH-02 三个接口共用）：
 *   - loginOrRegisterByCode：验证码登录 / 首次自动建账
 *   - registerWithPassword：已通过验证码后设置密码
 *   - loginByPassword：日常密码登录
 *
 * 共性规则：
 *   - 手机号永不落库明文，全程使用 phone_hmac + phone_cipher
 *   - banned=true 一律抛 ACCOUNT_LOCKED 403
 *   - 返回 TokenPairVO（access + refresh + expiresAt + verifyStatus）
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /** 新用户启动积分（@JsonValue P2.7 修复） */
    private static final int SIGNUP_BONUS_POINTS = 100;

    private final AuthUserRepository userRepo;
    private final UserProfileRepository profileRepo;
    private final SmsCodeVerifier smsVerifier;
    private final PasswordEncoder passwordEncoder;
    private final AesUtil aes;
    private final JwtUtil jwt;
    private final JwtProperties jwtProps;
    private final TokenBlacklist blacklist;
    private final com.campushub.credit.api.CreditApi creditApi;

    public AuthService(AuthUserRepository userRepo,
                       UserProfileRepository profileRepo,
                       SmsCodeVerifier smsVerifier,
                       PasswordEncoder passwordEncoder,
                       AesUtil aes,
                       JwtUtil jwt,
                       JwtProperties jwtProps,
                       TokenBlacklist blacklist,
                       com.campushub.credit.api.CreditApi creditApi) {
        this.userRepo = userRepo;
        this.profileRepo = profileRepo;
        this.smsVerifier = smsVerifier;
        this.passwordEncoder = passwordEncoder;
        this.aes = aes;
        this.jwt = jwt;
        this.jwtProps = jwtProps;
        this.blacklist = blacklist;
        this.creditApi = creditApi;
    }

    /** 给新用户发启动积分（幂等，重复调用会被 bizKey 短路）。 */
    private void grantSignupBonus(long userId) {
        creditApi.award(userId, SIGNUP_BONUS_POINTS, "SIGNUP_BONUS", "user:" + userId + ":signup");
    }

    /**
     * POST /api/auth/token  验证码登录/注册。
     * 验证码 OK → 已存在则登录；不存在则自动建账（verifyStatus=GUEST，password_hash=null）。
     */
    @Transactional
    public TokenPairVO loginOrRegisterByCode(String phone, String smsCode) {
        smsVerifier.verifyAndConsume(phone, smsCode, SmsScene.LOGIN_REGISTER);

        String hmac = aes.hmacIndex(phone);
        AuthUser user = userRepo.findByPhoneHmac(hmac).orElseGet(() -> {
            AuthUser fresh = new AuthUser(hmac, aes.encrypt(phone));
            AuthUser saved = userRepo.save(fresh);
            profileRepo.save(new UserProfile(saved.getId(), defaultNickname(phone)));
            grantSignupBonus(saved.getId());
            log.info("新用户自动注册 userId={} phone={}", saved.getId(), PhoneMaskUtil.mask(phone));
            return saved;
        });

        ensureNotBanned(user);
        return issueTokens(user);
    }

    /**
     * POST /api/auth/register  验证码通过后设置密码。
     * 已设密的手机号再次注册 → 409 PHONE_ALREADY_REGISTERED。
     * 未注册 → 自动建账并设密。
     * 已注册但未设密（仅 LoginByCode 过的）→ 补设密码。
     */
    @Transactional
    public TokenPairVO registerWithPassword(String phone, String smsCode, String password) {
        PasswordPolicy.validate(password);
        smsVerifier.verifyAndConsume(phone, smsCode, SmsScene.LOGIN_REGISTER);

        String hmac = aes.hmacIndex(phone);
        AuthUser user = userRepo.findByPhoneHmac(hmac).orElseGet(() -> {
            AuthUser fresh = new AuthUser(hmac, aes.encrypt(phone));
            AuthUser saved = userRepo.save(fresh);
            profileRepo.save(new UserProfile(saved.getId(), defaultNickname(phone)));
            grantSignupBonus(saved.getId());
            return saved;
        });

        if (user.hasPassword()) {
            throw new BizException(AuthErrorCode.PHONE_ALREADY_REGISTERED,
                    "该手机号已注册并设置过密码", 409);
        }

        ensureNotBanned(user);
        user.setPasswordHash(passwordEncoder.encode(password));
        userRepo.save(user);
        log.info("用户设置密码 userId={}", user.getId());
        return issueTokens(user);
    }

    /**
     * POST /api/auth/token/password  密码登录。
     * 用户不存在 / 密码不匹配 → 统一 401 CREDENTIALS_INVALID（避免账号枚举）。
     */
    @Transactional(readOnly = true)
    public TokenPairVO loginByPassword(String phone, String password) {
        String hmac = aes.hmacIndex(phone);
        AuthUser user = userRepo.findByPhoneHmac(hmac).orElseThrow(() ->
                new BizException(AuthErrorCode.CREDENTIALS_INVALID, "手机号或密码错误", 401));

        ensureNotBanned(user);

        if (!user.hasPassword()) {
            throw new BizException(AuthErrorCode.PASSWORD_NOT_SET,
                    "账号未设置密码，请使用验证码登录后到设置中补设", 401);
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BizException(AuthErrorCode.CREDENTIALS_INVALID, "手机号或密码错误", 401);
        }
        return issueTokens(user);
    }

    /**
     * POST /api/auth/token/refresh  用 refresh token 换新 access+refresh。
     * 旧 refresh 的 jti 立即入黑（refresh-rotation 防重放）。
     */
    @Transactional(readOnly = true)
    public TokenPairVO refresh(String refreshToken) {
        Claims claims = jwt.parse(refreshToken);
        if (!"REFRESH".equals(jwt.getType(claims))) {
            throw new BizException(ResponseCode.UNAUTHORIZED, "需要 Refresh Token");
        }
        String oldJti = jwt.getJti(claims);
        if (blacklist.isRevoked(oldJti)) {
            throw new BizException(ResponseCode.UNAUTHORIZED, "Refresh Token 已失效");
        }

        long userId = jwt.getUserId(claims);
        AuthUser user = userRepo.findById(userId).orElseThrow(() ->
                new BizException(AuthErrorCode.CREDENTIALS_INVALID, "用户不存在", 401));
        ensureNotBanned(user);

        // 旧 refresh 入黑 → 防重放
        blacklist.revoke(oldJti, jwt.getExpiry(claims));

        return issueTokens(user);
    }

    /**
     * POST /api/auth/logout  把当前 access + 携带 refresh 双入黑。
     * refreshToken 可空（前端有时只传 access）。
     */
    public void logout(String accessToken, String refreshToken) {
        revokeIfValid(accessToken);
        if (refreshToken != null && !refreshToken.isBlank()) {
            revokeIfValid(refreshToken);
        }
    }

    private void revokeIfValid(String token) {
        try {
            Claims claims = jwt.parse(token);
            blacklist.revoke(jwt.getJti(claims), jwt.getExpiry(claims));
        } catch (BizException ignore) {
            // token 已失效就无需再入黑（幂等）
        }
    }

    private void ensureNotBanned(AuthUser user) {
        if (user.isBanned()) {
            throw new BizException(AuthErrorCode.ACCOUNT_LOCKED, "账号已被封禁", 403);
        }
    }

    private TokenPairVO issueTokens(AuthUser user) {
        String access = jwt.generateAccessToken(user.getId(), user.getVerifyStatus().name());
        String refresh = jwt.generateRefreshToken(user.getId(), user.getVerifyStatus().name());
        Instant now = Instant.now();
        return new TokenPairVO(
                user.getId(),
                access,
                refresh,
                now.plus(Duration.ofMinutes(jwtProps.getAccessTtlMinutes())),
                now.plus(Duration.ofDays(jwtProps.getRefreshTtlDays())),
                user.getVerifyStatus()
        );
    }

    private String defaultNickname(String phone) {
        return "用户" + PhoneMaskUtil.mask(phone);
    }
}
