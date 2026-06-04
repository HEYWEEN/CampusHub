package com.campushub.admin.service;

import com.campushub.admin.vo.AdminUserVO;
import com.campushub.auth.entity.AuthUser;
import com.campushub.auth.repository.AuthUserRepository;
import com.campushub.common.exception.NotFoundException;
import com.campushub.notify.api.NotifyApi;
import com.campushub.user.entity.UserProfile;
import com.campushub.user.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AdminUserService 纯单测（Mockito）—— F-ADMIN-02 用户搜索 + 封禁/解封。
 * 覆盖：空查询 / 数字精确查 / 昵称模糊查 / 封禁(含原因) / 解封 / 用户不存在。
 */
@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock AuthUserRepository userRepo;
    @Mock UserProfileRepository profileRepo;
    @Mock NotifyApi notifyApi;

    @InjectMocks AdminUserService service;

    private AuthUser mockUser(long id) {
        AuthUser u = mock(AuthUser.class);
        when(u.getId()).thenReturn(id);
        return u;
    }

    @Test
    void search_blank_returnsAllUsers() {
        // 空查询 → 列出全部用户（按 id 升序）。mock 须先建好再 thenReturn，避免嵌套 stubbing
        AuthUser u1 = mockUser(1L);
        AuthUser u2 = mockUser(2L);
        when(userRepo.findAll(any(Sort.class))).thenReturn(List.of(u1, u2));
        when(profileRepo.findAll()).thenReturn(List.of());

        assertEquals(2, service.search("   ").size());
        assertEquals(2, service.search(null).size());
    }

    @Test
    void search_byNumericId_exactLookup() {
        AuthUser u = mockUser(42L);
        when(userRepo.findById(42L)).thenReturn(Optional.of(u));
        when(profileRepo.findByUserId(42L)).thenReturn(Optional.empty());

        List<AdminUserVO> out = service.search(" 42 ");

        assertEquals(1, out.size());
        assertEquals(42L, out.get(0).userId());
        verify(profileRepo, never()).findTop20ByNicknameContainingIgnoreCaseOrderByUserIdAsc(anyString());
    }

    @Test
    void search_byNickname_fuzzyMapsAll() {
        UserProfile p = mock(UserProfile.class);
        when(p.getUserId()).thenReturn(7L);
        when(p.getNickname()).thenReturn("阿强");
        AuthUser u = mockUser(7L);
        when(profileRepo.findTop20ByNicknameContainingIgnoreCaseOrderByUserIdAsc("强"))
                .thenReturn(List.of(p));
        when(userRepo.findById(7L)).thenReturn(Optional.of(u));

        List<AdminUserVO> out = service.search("强");

        assertEquals(1, out.size());
        assertEquals("阿强", out.get(0).nickname());
    }

    @Test
    void setBan_ban_savesAndNotifiesWithReason() {
        AuthUser u = mockUser(5L);
        when(userRepo.findById(5L)).thenReturn(Optional.of(u));
        when(profileRepo.findByUserId(5L)).thenReturn(Optional.empty());

        service.setBan(5L, true, "刷单");

        verify(u).setBanned(true);
        verify(userRepo).save(u);
        verify(notifyApi).appendLetter(eq(5L), eq("ACCOUNT_BAN"), eq("账号被封禁"),
                contains("刷单"), anyString());
    }

    @Test
    void setBan_unban_savesAndNotifies() {
        AuthUser u = mockUser(5L);
        when(userRepo.findById(5L)).thenReturn(Optional.of(u));
        when(profileRepo.findByUserId(5L)).thenReturn(Optional.empty());

        service.setBan(5L, false, null);

        verify(u).setBanned(false);
        verify(notifyApi).appendLetter(eq(5L), eq("ACCOUNT_BAN"), eq("账号已解封"),
                anyString(), anyString());
    }

    @Test
    void setBan_userNotFound_throws() {
        when(userRepo.findById(999L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.setBan(999L, true, "x"));
        verify(notifyApi, never()).appendLetter(anyLong(), anyString(), anyString(), anyString(), anyString());
    }
}
