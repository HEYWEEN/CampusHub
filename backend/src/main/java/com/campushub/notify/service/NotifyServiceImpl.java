package com.campushub.notify.service;

import com.campushub.common.exception.BizException;
import com.campushub.notify.api.NotifyApi;
import com.campushub.notify.entity.NotifyMessage;
import com.campushub.notify.exception.NotifyErrorCode;
import com.campushub.notify.repository.NotifyMessageRepository;
import com.campushub.notify.vo.NotifyMessageVO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * notify 模块核心服务 —— 同时实现 {@link NotifyApi} 给跨模块调用（NTF-01）。
 *
 * <p><b>幂等</b>：appendLetter 先用 existsByBizKey 短路；并发场景下两个线程都查不到时，
 * DB 的 {@code uk_notify_biz_key} 唯一约束兜底，捕获 {@link DataIntegrityViolationException} 后静默 return。
 *
 * <p><b>越权</b>：markRead 只允许标记自己的消息，越权抛 {@link NotifyErrorCode#MESSAGE_FORBIDDEN}。
 */
@Service
public class NotifyServiceImpl implements NotifyApi, NotifyService {

    private final NotifyMessageRepository repo;

    public NotifyServiceImpl(NotifyMessageRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public void appendLetter(long userId, String type, String title, String body, String bizKey) {
        if (repo.existsByBizKey(bizKey)) return;
        try {
            repo.save(new NotifyMessage(userId, type, title, body, bizKey));
        } catch (DataIntegrityViolationException dup) {
            // 并发 race：另一线程已抢先插入同 bizKey，按幂等语义静默
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotifyMessageVO> listMine(long userId, String filter) {
        List<NotifyMessage> rows;
        if ("unread".equalsIgnoreCase(filter)) {
            rows = repo.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId);
        } else if ("read".equalsIgnoreCase(filter)) {
            rows = repo.findByUserIdAndReadAtIsNotNullOrderByCreatedAtDesc(userId);
        } else {
            rows = repo.findByUserIdOrderByReadAtAscCreatedAtDesc(userId);
        }
        return rows.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(long userId) {
        return repo.countByUserIdAndReadAtIsNull(userId);
    }

    @Override
    @Transactional
    public void markRead(long userId, long messageId) {
        NotifyMessage msg = repo.findById(messageId)
                .orElseThrow(() -> new BizException(NotifyErrorCode.MESSAGE_NOT_FOUND, "消息不存在", 404));
        if (!msg.getUserId().equals(userId)) {
            throw new BizException(NotifyErrorCode.MESSAGE_FORBIDDEN, "无权操作他人的消息", 403);
        }
        msg.markRead();
        repo.save(msg);
    }

    @Override
    @Transactional
    public void markAllRead(long userId) {
        List<NotifyMessage> unread = repo.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId);
        for (NotifyMessage m : unread) {
            m.markRead();
        }
        repo.saveAll(unread);
    }

    private NotifyMessageVO toVO(NotifyMessage m) {
        return new NotifyMessageVO(
                m.getId(),
                m.getType(),
                m.getTitle(),
                m.getBody(),
                m.getReadAt(),
                m.getCreatedAt(),
                m.getBizKey());
    }
}
