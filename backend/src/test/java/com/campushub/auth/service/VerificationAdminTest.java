package com.campushub.auth.service;

import com.campushub.auth.entity.AuthUser;
import com.campushub.auth.entity.AuthVerification;
import com.campushub.auth.entity.VerificationStatus;
import com.campushub.auth.repository.AuthUserRepository;
import com.campushub.auth.repository.AuthVerificationRepository;
import com.campushub.common.enums.VerifyStatus;
import com.campushub.common.exception.BizException;
import com.campushub.common.util.AesUtil;
import com.campushub.notify.api.NotifyApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationAdminTest {

    @Mock AuthVerificationRepository verRepo;
    @Mock AuthUserRepository userRepo;
    @Mock AesUtil aes;
    @Mock NotifyApi notifyApi;

    @InjectMocks VerificationService service;

    private AuthVerification pendingVer(long id, long userId) {
        AuthVerification v = new AuthVerification(userId, "rc", "sc", null, "[]");
        ReflectionTestUtils.setField(v, "id", id);
        return v;
    }

    @Test
    void adminApprove_setsUserApprovedAndNotifies() {
        AuthVerification ver = pendingVer(5L, 100L);
        AuthUser user = new AuthUser("hmac", "cipher");
        when(verRepo.findById(5L)).thenReturn(Optional.of(ver));
        when(userRepo.findById(100L)).thenReturn(Optional.of(user));

        service.adminApprove(5L);

        assertEquals(VerificationStatus.APPROVED, ver.getStatus());
        assertEquals(VerifyStatus.APPROVED, user.getVerifyStatus());
        verify(notifyApi).appendLetter(eq(100L), eq("VERIFY_RESULT"), anyString(), anyString(), anyString());
    }

    @Test
    void adminReject_setsUserRejectedWithReason() {
        AuthVerification ver = pendingVer(5L, 100L);
        AuthUser user = new AuthUser("hmac", "cipher");
        when(verRepo.findById(5L)).thenReturn(Optional.of(ver));
        when(userRepo.findById(100L)).thenReturn(Optional.of(user));

        service.adminReject(5L, "证件照模糊");

        assertEquals(VerificationStatus.REJECTED, ver.getStatus());
        assertEquals(VerifyStatus.REJECTED, user.getVerifyStatus());
        assertEquals("证件照模糊", ver.getRejectReason());
        verify(notifyApi).appendLetter(eq(100L), eq("VERIFY_RESULT"), anyString(), anyString(), anyString());
    }

    @Test
    void adminApprove_notPending_throws() {
        AuthVerification ver = pendingVer(5L, 100L);
        ver.approve();   // 已通过
        when(verRepo.findById(5L)).thenReturn(Optional.of(ver));

        BizException ex = assertThrows(BizException.class, () -> service.adminApprove(5L));
        assertEquals(2006, ex.getCode());
        verify(notifyApi, never()).appendLetter(anyLong(), anyString(), anyString(), anyString(), anyString());
    }
}
