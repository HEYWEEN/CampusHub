package com.campushub.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM 列加密工具：
 *   - encrypt: 用于 auth_user.phone / auth_verification.real_name / student_no 等隐私字段
 *   - hmacIndex: 同字段的查询索引列（确定性 SHA-256，使加密字段可按精确值查找）
 *
 * 密钥配置：campushub.crypto.aes-key（≥ 32 字节）
 */
@Component
public class AesUtil {

    private static final String TRANSFORM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LEN = 12;

    private final SecretKey aesKey;
    private final byte[] hmacKey;

    public AesUtil(@Value("${campushub.crypto.aes-key:dev-default-aes-key-please-override-32b!!}") String key) {
        byte[] raw = key.getBytes(StandardCharsets.UTF_8);
        if (raw.length < 32) {
            throw new IllegalStateException("campushub.crypto.aes-key 长度必须 ≥ 32 字节");
        }
        byte[] aes256 = new byte[32];
        System.arraycopy(raw, 0, aes256, 0, 32);
        this.aesKey = new SecretKeySpec(aes256, "AES");
        this.hmacKey = aes256;
    }

    public String encrypt(String plain) {
        if (plain == null) return null;
        try {
            byte[] iv = new byte[IV_LEN];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[IV_LEN + cipherText.length];
            System.arraycopy(iv, 0, out, 0, IV_LEN);
            System.arraycopy(cipherText, 0, out, IV_LEN, cipherText.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception ex) {
            throw new IllegalStateException("AES 加密失败", ex);
        }
    }

    public String decrypt(String cipherTextBase64) {
        if (cipherTextBase64 == null) return null;
        try {
            byte[] all = Base64.getDecoder().decode(cipherTextBase64);
            byte[] iv = new byte[IV_LEN];
            byte[] cipherText = new byte[all.length - IV_LEN];
            System.arraycopy(all, 0, iv, 0, IV_LEN);
            System.arraycopy(all, IV_LEN, cipherText, 0, cipherText.length);
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("AES 解密失败", ex);
        }
    }

    /**
     * 用于"加密字段精确查询"的确定性索引：HMAC-SHA256(key, plain)。
     * 例：phone 加密存 phone_enc，再存 phone_index = hmacIndex(phone)，按手机号查找时查 phone_index = ?。
     */
    public String hmacIndex(String plain) {
        if (plain == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(hmacKey);
            md.update(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(md.digest());
        } catch (Exception ex) {
            throw new IllegalStateException("HMAC 索引计算失败", ex);
        }
    }
}
