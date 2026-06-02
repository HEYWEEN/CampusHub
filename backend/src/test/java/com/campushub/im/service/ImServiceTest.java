package com.campushub.im.service;

import com.campushub.common.PublicUserVO;
import com.campushub.common.exception.BizException;
import com.campushub.im.dto.ImSendMessageDTO;
import com.campushub.im.entity.ImConversation;
import com.campushub.im.entity.ImMessage;
import com.campushub.im.entity.ImMessageType;
import com.campushub.im.repository.ImConversationRepository;
import com.campushub.im.repository.ImMessageRepository;
import com.campushub.im.vo.ImConversationVO;
import com.campushub.im.vo.ImMessageVO;
import com.campushub.user.api.UserApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImServiceTest {

    @Mock ImConversationRepository convRepo;
    @Mock ImMessageRepository msgRepo;
    @Mock UserApi userApi;

    @InjectMocks ImServiceImpl service;

    private ImConversation conv(long id, long a, long b) {
        ImConversation c = new ImConversation(a, b, null, null);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    @Test
    void getOrCreate_self_throws() {
        BizException ex = assertThrows(BizException.class, () -> service.getOrCreateConversation(1L, 1L));
        assertEquals(8002, ex.getCode());
    }

    @Test
    void getOrCreate_peerNotFound_throws() {
        when(userApi.exists(2L)).thenReturn(false);
        BizException ex = assertThrows(BizException.class, () -> service.getOrCreateConversation(1L, 2L));
        assertEquals(8003, ex.getCode());
    }

    @Test
    void getOrCreate_dedupesCanonicalPair() {
        when(userApi.exists(1L)).thenReturn(true);
        when(convRepo.findByUserAIdAndUserBId(1L, 2L)).thenReturn(Optional.of(conv(10L, 1L, 2L)));
        when(userApi.getPublicUser(1L)).thenReturn(new PublicUserVO(1L, "对方", null, null));
        when(msgRepo.findFirstByConversationIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.empty());
        when(msgRepo.countByConversationIdAndSenderIdNotAndIdGreaterThan(eq(10L), eq(2L), any()))
                .thenReturn(0L);

        // 用 (2,1) 调用，应规范化到 (1,2) 命中已存在会话，不再 save
        ImConversationVO vo = service.getOrCreateConversation(2L, 1L);

        assertEquals(10L, vo.conversationId());
        assertEquals(1L, vo.peer().getUserId());
        verify(convRepo, never()).save(any());
    }

    @Test
    void sendMessage_notParticipant_throws() {
        when(convRepo.findById(10L)).thenReturn(Optional.of(conv(10L, 1L, 2L)));
        ImSendMessageDTO dto = new ImSendMessageDTO();
        dto.setContent("hi");
        BizException ex = assertThrows(BizException.class, () -> service.sendMessage(99L, 10L, dto));
        assertEquals(8001, ex.getCode());
        verify(msgRepo, never()).save(any());
    }

    @Test
    void sendMessage_happy_insertsAndTouches() {
        when(convRepo.findById(10L)).thenReturn(Optional.of(conv(10L, 1L, 2L)));
        when(msgRepo.save(any(ImMessage.class))).thenAnswer(i -> {
            ImMessage m = i.getArgument(0);
            ReflectionTestUtils.setField(m, "id", 100L);   // 模拟 DB 自增 id
            return m;
        });

        ImSendMessageDTO dto = new ImSendMessageDTO();
        dto.setContent("  在吗  ");
        ImMessageVO vo = service.sendMessage(1L, 10L, dto);

        assertEquals(1L, vo.senderId());
        assertEquals(ImMessageType.TEXT, vo.contentType());
        assertEquals("在吗", vo.content());           // 已 trim
        verify(msgRepo).save(any(ImMessage.class));
        verify(convRepo).save(any(ImConversation.class));
    }

    @Test
    void onTaskAccepted_createsConversationAndSystemMessage() {
        when(convRepo.findByUserAIdAndUserBId(1L, 2L)).thenReturn(Optional.empty());
        when(convRepo.save(any(ImConversation.class))).thenAnswer(i -> {
            ImConversation c = i.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 55L);
            return c;
        });
        when(msgRepo.save(any(ImMessage.class))).thenAnswer(i -> i.getArgument(0));

        service.onTaskAccepted(2L, 1L, 999L);   // publisher=2, accepter=1 → 规范化 (1,2)

        verify(convRepo).findByUserAIdAndUserBId(1L, 2L);
        verify(msgRepo).save(any(ImMessage.class));   // 系统消息
    }

    @Test
    void onTaskAccepted_selfNoop() {
        service.onTaskAccepted(5L, 5L, 1L);
        verify(convRepo, never()).save(any());
        verify(msgRepo, never()).save(any());
    }
}
