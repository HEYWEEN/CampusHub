package com.campushub.auth.service;

import com.campushub.auth.dto.VerificationSubmitDTO;
import com.campushub.auth.entity.AuthUser;
import com.campushub.auth.entity.AuthVerification;
import com.campushub.auth.entity.VerificationStatus;
import com.campushub.auth.exception.AuthErrorCode;
import com.campushub.auth.repository.AuthUserRepository;
import com.campushub.auth.repository.AuthVerificationRepository;
import com.campushub.auth.vo.VerificationStatusVO;
import com.campushub.common.enums.VerifyStatus;
import com.campushub.common.exception.BizException;
import com.campushub.common.exception.NotFoundException;
import com.campushub.common.util.AesUtil;
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
    private final ObjectMapper json = new ObjectMapper();

    public VerificationService(AuthVerificationRepository verRepo,
                               AuthUserRepository userRepo,
                               AesUtil aes) {
        this.verRepo = verRepo;
        this.userRepo = userRepo;
        this.aes = aes;
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

    /**
     * dev-only：把当前用户最新一条 PENDING verification 改成 APPROVED，
     * 同时 auth_user.verify_status 升为 APPROVED。
     * 给 dev/demo 模式下没有管理员审批端的场景兜底。
     * 由 DevAuthController（@Profile("!prod")）调用，不暴露到生产。
     */
    @Transactional
    public VerificationStatusVO devApproveMine(long userId) {
        AuthVerification ver = verRepo.findTopByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new NotFoundException("尚未提交学生证认证"));
        ver.approve();
        verRepo.save(ver);

        AuthUser user = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在: " + userId));
        user.setVerifyStatus(VerifyStatus.APPROVED);
        userRepo.save(user);

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
