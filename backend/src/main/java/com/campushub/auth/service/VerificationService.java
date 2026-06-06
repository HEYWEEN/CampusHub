package com.campushub.auth.service;

import com.campushub.auth.dto.VerificationSubmitDTO;
import com.campushub.auth.entity.AuthUser;
import com.campushub.auth.entity.AuthVerification;
import com.campushub.auth.entity.VerificationStatus;
import com.campushub.auth.exception.AuthErrorCode;
import com.campushub.auth.repository.AuthUserRepository;
import com.campushub.auth.repository.AuthVerificationRepository;
import com.campushub.auth.vo.AdminVerificationVO;
import com.campushub.auth.vo.VerificationStatusVO;
import com.campushub.common.enums.VerifyStatus;
import com.campushub.common.exception.BizException;
import com.campushub.common.exception.NotFoundException;
import com.campushub.common.util.AesUtil;
import com.campushub.notify.api.NotifyApi;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 学生证认证业务：
 *   - submit：接前端 attachmentUrls（由 POST /api/uploads 预上传得到）
 *     三段密文 AES-GCM → 写 auth_verification（attachment_sha256_json 列改存 URL 列表 JSON）
 *     同步把 auth_user.verify_status 从 GUEST/REJECTED 改成 PENDING
 *   - PENDING 中重复提交直接 409；REJECTED 后允许重新提交（覆盖上一条）
 *   - queryMine：返回 VerificationStatusVO（不下发任何明文/密文，仅 status+url+时间）
 *
 * schema_audit A-12 修复：原本接 Base64，已改为 URL 列表（与 trade/profile 统一）。
 */
@Service
public class VerificationService {

    private final AuthVerificationRepository verRepo;
    private final AuthUserRepository userRepo;
    private final AesUtil aes;
    private final NotifyApi notifyApi;
    private final ObjectMapper json = new ObjectMapper();

    public VerificationService(AuthVerificationRepository verRepo,
                               AuthUserRepository userRepo,
                               AesUtil aes,
                               NotifyApi notifyApi) {
        this.verRepo = verRepo;
        this.userRepo = userRepo;
        this.aes = aes;
        this.notifyApi = notifyApi;
    }

    // ==================== F-ADMIN-01 管理员审核 ====================

    /** admin：待审核认证队列（含解密实名/学号，仅 admin 端）。 */
    @Transactional(readOnly = true)
    public List<AdminVerificationVO> adminListPending() {
        return verRepo.findByStatusOrderByCreatedAtAsc(VerificationStatus.PENDING).stream()
                .map(v -> new AdminVerificationVO(
                        v.getId(), v.getUserId(),
                        safeDecrypt(v.getRealNameCipher()),
                        safeDecrypt(v.getStudentNoCipher()),
                        safeDecrypt(v.getIdCardCipher()),
                        fromJsonArray(v.getAttachmentSha256Json()),
                        v.getStatus(), v.getCreatedAt()))
                .toList();
    }

    /** admin：通过认证 → 用户 verify_status=APPROVED + 站内信。 */
    @Transactional
    public void adminApprove(long verificationId) {
        AuthVerification ver = requirePending(verificationId);
        ver.approve();
        verRepo.save(ver);
        AuthUser user = userRepo.findById(ver.getUserId())
                .orElseThrow(() -> new NotFoundException("用户不存在: " + ver.getUserId()));
        user.setVerifyStatus(VerifyStatus.APPROVED);
        userRepo.save(user);
        notifyApi.appendLetter(ver.getUserId(), "VERIFY_RESULT",
                "学生证认证已通过", "恭喜，你已通过校园认证 🎓", verifyBizKey(ver));
    }

    /** admin：驳回认证 + 理由 → 用户 verify_status=REJECTED + 站内信。 */
    @Transactional
    public void adminReject(long verificationId, String reason) {
        AuthVerification ver = requirePending(verificationId);
        ver.reject(reason);
        verRepo.save(ver);
        AuthUser user = userRepo.findById(ver.getUserId())
                .orElseThrow(() -> new NotFoundException("用户不存在: " + ver.getUserId()));
        user.setVerifyStatus(VerifyStatus.REJECTED);
        userRepo.save(user);
        notifyApi.appendLetter(ver.getUserId(), "VERIFY_RESULT",
                "学生证认证未通过", "认证被驳回：" + reason + "，可修改后重新提交", verifyBizKey(ver));
    }

    private AuthVerification requirePending(long verificationId) {
        AuthVerification ver = verRepo.findById(verificationId)
                .orElseThrow(() -> new NotFoundException("认证申请不存在: " + verificationId));
        if (ver.getStatus() != VerificationStatus.PENDING) {
            throw new BizException(AuthErrorCode.VERIFICATION_NOT_PENDING, "该认证申请已处理", 409);
        }
        return ver;
    }

    private String verifyBizKey(AuthVerification ver) {
        return "VERIFY_RESULT:" + ver.getId() + ":" + ver.getUserId();
    }

    private String safeDecrypt(String cipher) {
        if (cipher == null || cipher.isBlank()) return null;
        try {
            return aes.decrypt(cipher);
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public VerificationStatusVO submit(long userId, VerificationSubmitDTO dto) {
        // 1. PENDING 中拒绝重复提交（避免审核队列被刷）
        if (verRepo.existsByUserIdAndStatus(userId, VerificationStatus.PENDING)) {
            throw new BizException(AuthErrorCode.PHONE_ALREADY_REGISTERED,
                    "已有待审认证，请等待管理员处理", 409);
        }

        // 2. 直接保留 URL 列表（前端已通过 /api/uploads 上传到 ImageStorage）
        List<String> urls = List.copyOf(dto.getAttachmentUrls());

        // 3. 加密敏感字段（idCard 可空）
        String idCardCipher = (dto.getIdCard() == null || dto.getIdCard().isBlank())
                ? null : aes.encrypt(dto.getIdCard());

        AuthVerification ver = new AuthVerification(
                userId,
                aes.encrypt(dto.getRealName()),
                aes.encrypt(dto.getStudentNo()),
                idCardCipher,
                toJsonArray(urls)
        );
        verRepo.save(ver);

        // 4. 同步 auth_user.verify_status → PENDING
        AuthUser user = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在: " + userId));
        user.setVerifyStatus(VerifyStatus.PENDING);
        userRepo.save(user);

        return toVO(ver, urls);
    }

    @Transactional(readOnly = true)
    public VerificationStatusVO queryMine(long userId) {
        AuthVerification ver = verRepo.findTopByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new NotFoundException("尚未提交学生证认证"));
        return toVO(ver, fromJsonArray(ver.getAttachmentSha256Json()));
    }

    private VerificationStatusVO toVO(AuthVerification ver, List<String> urls) {
        return new VerificationStatusVO(
                ver.getId(), ver.getStatus(), ver.getRejectReason(),
                urls, ver.getCreatedAt(), ver.getUpdatedAt()
        );
    }

    private String toJsonArray(List<String> urls) {
        try {
            return json.writeValueAsString(urls);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化 URL 列表失败", e);
        }
    }

    private List<String> fromJsonArray(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptyList();
        try {
            return json.readValue(raw, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
