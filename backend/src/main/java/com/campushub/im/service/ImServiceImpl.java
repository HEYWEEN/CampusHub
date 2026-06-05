package com.campushub.im.service;

import com.campushub.common.exception.BizException;
import com.campushub.common.exception.NotFoundException;
import com.campushub.common.response.PageResponse;
import com.campushub.im.dto.ImSendMessageDTO;
import com.campushub.im.entity.ImConversation;
import com.campushub.im.entity.ImMessage;
import com.campushub.im.entity.ImMessageType;
import com.campushub.im.exception.ImErrorCode;
import com.campushub.im.repository.ImConversationRepository;
import com.campushub.im.repository.ImMessageRepository;
import com.campushub.im.vo.ImConversationVO;
import com.campushub.im.vo.ImMessageVO;
import com.campushub.user.api.UserApi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ImServiceImpl implements ImService {

    private final ImConversationRepository convRepo;
    private final ImMessageRepository msgRepo;
    private final UserApi userApi;

    public ImServiceImpl(ImConversationRepository convRepo, ImMessageRepository msgRepo, UserApi userApi) {
        this.convRepo = convRepo;
        this.msgRepo = msgRepo;
        this.userApi = userApi;
    }

    @Override
    @Transactional
    public ImConversationVO getOrCreateConversation(long userId, long peerId) {
        if (userId == peerId) {
            throw new BizException(ImErrorCode.CANNOT_CHAT_SELF, "不能和自己私信");
        }
        if (!userApi.exists(peerId)) {
            throw new BizException(ImErrorCode.PEER_NOT_FOUND, "对方用户不存在", 404);
        }
        ImConversation conv = getOrCreateEntity(userId, peerId, null, null);
        return toVO(conv, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImConversationVO> listConversations(long userId) {
        return convRepo.findByUserAIdOrUserBIdOrderByLastMsgAtDesc(userId, userId).stream()
                .map(c -> toVO(c, userId))
                .toList();
    }

    @Override
    @Transactional
    public PageResponse<ImMessageVO> getMessages(long userId, long conversationId, int page, int size) {
        ImConversation conv = requireParticipant(conversationId, userId);
        // 拉取即清未读：已读推进到该会话最新消息 id
        msgRepo.findFirstByConversationIdOrderByCreatedAtDesc(conversationId)
                .ifPresent(latest -> conv.markReadUpTo(userId, latest.getId()));
        convRepo.save(conv);
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 100);
        Page<ImMessage> result = msgRepo.findByConversationIdOrderByCreatedAtDesc(
                conversationId, PageRequest.of(p - 1, s));
        return PageResponse.of(result, m -> ImMessageVO.from(m, userId));
    }

    @Override
    @Transactional
    public ImMessageVO sendMessage(long userId, long conversationId, ImSendMessageDTO dto) {
        ImConversation conv = requireParticipant(conversationId, userId);
        ImMessageType type = dto.getContentType() == ImMessageType.SYSTEM ? ImMessageType.TEXT : dto.getContentType();
        ImMessage msg = msgRepo.save(new ImMessage(conversationId, userId, type, dto.getContent().trim()));
        conv.touchLastMsg(msg.getCreatedAt());
        conv.markReadUpTo(userId, msg.getId());   // 自己发的对自己即已读
        convRepo.save(conv);
        return ImMessageVO.from(msg, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadTotal(long userId) {
        return convRepo.findByUserAIdOrUserBIdOrderByLastMsgAtDesc(userId, userId).stream()
                .mapToLong(c -> unreadOf(c, userId))
                .sum();
    }

    @Override
    @Transactional
    public void onTaskAccepted(long publisherId, long accepterId, long taskId) {
        if (publisherId == accepterId) return;
        ImConversation conv = getOrCreateEntity(publisherId, accepterId, "TASK", taskId);
        ImMessage sys = msgRepo.save(new ImMessage(
                conv.getId(), ImMessage.SYSTEM_SENDER, ImMessageType.SYSTEM, "任务已被接单，双方可在此沟通 🤝"));
        conv.touchLastMsg(sys.getCreatedAt());
        convRepo.save(conv);
    }

    // ==================== 内部 ====================

    private ImConversation getOrCreateEntity(long u1, long u2, String bizType, Long bizId) {
        long a = Math.min(u1, u2);
        long b = Math.max(u1, u2);
        return convRepo.findByUserAIdAndUserBId(a, b)
                .orElseGet(() -> convRepo.save(new ImConversation(a, b, bizType, bizId)));
    }

    private ImConversation requireParticipant(long conversationId, long userId) {
        ImConversation conv = convRepo.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("会话不存在: " + conversationId));
        if (!conv.hasParticipant(userId)) {
            throw new BizException(ImErrorCode.NOT_PARTICIPANT, "无权访问该会话", 403);
        }
        return conv;
    }

    private long unreadOf(ImConversation conv, long viewerId) {
        return msgRepo.countByConversationIdAndSenderIdNotAndIdGreaterThan(
                conv.getId(), viewerId, conv.lastReadMsgIdOf(viewerId));
    }

    private ImConversationVO toVO(ImConversation conv, long viewerId) {
        long peerId = conv.peerOf(viewerId);
        ImMessage last = msgRepo.findFirstByConversationIdOrderByCreatedAtDesc(conv.getId()).orElse(null);
        String preview = last == null ? null : switch (last.getContentType()) {
            case IMAGE -> "[图片]";
            case ORDER -> "[订单卡片]";
            default -> last.getContent();
        };
        return new ImConversationVO(
                conv.getId(),
                userApi.getPublicUser(peerId),
                preview,
                last == null ? null : last.getContentType(),
                conv.getLastMsgAt(),
                unreadOf(conv, viewerId));
    }
}
