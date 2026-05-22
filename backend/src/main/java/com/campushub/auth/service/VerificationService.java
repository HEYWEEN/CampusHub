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
import com.campushub.common.response.ResponseCode;
import com.campushub.common.storage.ObjectStorage;
import com.campushub.common.util.AesUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/**
 * 学生证认证业务：
 *   - submit：上传 base64 图 → 对象存储拿 SHA-256 → 三段密文 AES-GCM → 写 auth_verification
 *     同步把 auth_user.verify_status 从 GUEST/REJECTED 改成 PENDING
 *   - PENDING 中重复提交直接 409；REJECTED 后允许重新提交（覆盖上一条）
 *   - queryMine：返回 VerificationStatusVO（不下发任何明文/密文，仅 status+sha+时间）
 */
@Service
public class VerificationService {

    private final AuthVerificationRepository verRepo;
    private final AuthUserRepository userRepo;
    private final ObjectStorage storage;
    private final AesUtil aes;
    private final ObjectMapper json = new ObjectMapper();

    public VerificationService(AuthVerificationRepository verRepo,
                               AuthUserRepository userRepo,
                               ObjectStorage storage,
                               AesUtil aes) {
        this.verRepo = verRepo;
        this.userRepo = userRepo;
        this.storage = storage;
        this.aes = aes;
    }

    @Transactional
    public VerificationStatusVO submit(long userId, VerificationSubmitDTO dto) {
        // 1. PENDING 中拒绝重复提交（避免审核队列被刷）
        if (verRepo.existsByUserIdAndStatus(userId, VerificationStatus.PENDING)) {
            throw new BizException(AuthErrorCode.PHONE_ALREADY_REGISTERED,
                    "已有待审认证，请等待管理员处理", 409);
        }

        // 2. 上传图 + 收集 sha256
        List<String> shaList = new ArrayList<>(dto.getAttachmentsBase64().size());
        for (String b64 : dto.getAttachmentsBase64()) {
            byte[] bytes;
            try {
                bytes = Base64.getDecoder().decode(b64);
            } catch (IllegalArgumentException ex) {
                throw new BizException(ResponseCode.BAD_REQUEST, "证件图 base64 格式错误");
            }
            ObjectStorage.PutResult put = storage.put(bytes, "image/jpeg");
            shaList.add(put.sha256());
        }

        // 3. 加密敏感字段（idCard 可空）
        String idCardCipher = (dto.getIdCard() == null || dto.getIdCard().isBlank())
                ? null : aes.encrypt(dto.getIdCard());

        AuthVerification ver = new AuthVerification(
                userId,
                aes.encrypt(dto.getRealName()),
                aes.encrypt(dto.getStudentNo()),
                idCardCipher,
                toJsonArray(shaList)
        );
        verRepo.save(ver);

        // 4. 同步 auth_user.verify_status → PENDING
        AuthUser user = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在: " + userId));
        user.setVerifyStatus(VerifyStatus.PENDING);
        userRepo.save(user);

        return toVO(ver, shaList);
    }

    @Transactional(readOnly = true)
    public VerificationStatusVO queryMine(long userId) {
        AuthVerification ver = verRepo.findTopByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new NotFoundException("尚未提交学生证认证"));
        return toVO(ver, fromJsonArray(ver.getAttachmentSha256Json()));
    }

    private VerificationStatusVO toVO(AuthVerification ver, List<String> shaList) {
        return new VerificationStatusVO(
                ver.getId(), ver.getStatus(), ver.getRejectReason(),
                shaList, ver.getCreatedAt(), ver.getUpdatedAt()
        );
    }

    private String toJsonArray(List<String> shaList) {
        try {
            return json.writeValueAsString(shaList);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化 sha 列表失败", e);
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
