package com.campushub.notify.service;

import com.campushub.common.exception.BizException;
import com.campushub.notify.api.NotifyApi;
import com.campushub.notify.repository.NotifyMessageRepository;
import com.campushub.notify.vo.NotifyMessageVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * NTF-01 service 单测：appendLetter 幂等 + list filter + markRead 越权 + markAllRead 批量。
 * 沿用 D 的 SpringBootTest + ActiveProfiles("test") 风格。
 */
@SpringBootTest
@ActiveProfiles("test")
class NotifyServiceImplTest {

    @Autowired private NotifyApi notifyApi;
    @Autowired private NotifyService notifyService;
    @Autowired private NotifyMessageRepository repo;

    private static final long USER = 8001L;
    private static final long OTHER_USER = 8002L;

    @BeforeEach
    void cleanup() {
        repo.deleteAll();
    }

    @Test
    void appendLetter_writesRow_andCanBeListed() {
        notifyApi.appendLetter(USER, "TASK_ACCEPTED", "你的任务已被接单", "任务 #100 已被接单", "TASK_ACCEPTED:100:" + USER);

        List<NotifyMessageVO> mine = notifyService.listMine(USER, "all");
        assertEquals(1, mine.size());
        assertEquals("TASK_ACCEPTED", mine.get(0).type());
        assertNull(mine.get(0).readAt(), "新消息未读 readAt 应为 null");
    }

    @Test
    void appendLetter_sameBizKey_isIdempotent() {
        String bizKey = "TASK_COMPLETED:200:" + USER;
        notifyApi.appendLetter(USER, "TASK_COMPLETED", "任务已完成", "任务 #200 已完成", bizKey);
        notifyApi.appendLetter(USER, "TASK_COMPLETED", "任务已完成", "任务 #200 已完成", bizKey); // 重投

        assertEquals(1, repo.count(), "同 bizKey 重复 append 应被短路");
    }

    @Test
    void listMine_filterByReadStatus_works() {
        notifyApi.appendLetter(USER, "TASK_ACCEPTED", "t1", "b1", "BK:1:" + USER);
        notifyApi.appendLetter(USER, "TASK_COMPLETED", "t2", "b2", "BK:2:" + USER);
        long firstId = notifyService.listMine(USER, "all").get(1).id(); // 倒序：第二条是最早写入的

        notifyService.markRead(USER, firstId);

        assertEquals(2, notifyService.listMine(USER, "all").size());
        assertEquals(1, notifyService.listMine(USER, "unread").size());
        assertEquals(1, notifyService.listMine(USER, "read").size());
        assertEquals(1, notifyService.countUnread(USER));
    }

    @Test
    void markRead_otherUsersMessage_throwsForbidden() {
        notifyApi.appendLetter(OTHER_USER, "TASK_ACCEPTED", "t", "b", "BK:99:" + OTHER_USER);
        long otherId = notifyService.listMine(OTHER_USER, "all").get(0).id();

        BizException ex = assertThrows(BizException.class,
                () -> notifyService.markRead(USER, otherId));
        assertEquals(9002, ex.getCode());
    }

    @Test
    void markRead_nonexistent_throwsNotFound() {
        BizException ex = assertThrows(BizException.class,
                () -> notifyService.markRead(USER, 999_999_999L));
        assertEquals(9001, ex.getCode());
    }

    @Test
    void markAllRead_marksOnlyMine() {
        notifyApi.appendLetter(USER, "TASK_ACCEPTED", "t", "b", "BK:A:" + USER);
        notifyApi.appendLetter(USER, "TASK_COMPLETED", "t", "b", "BK:B:" + USER);
        notifyApi.appendLetter(OTHER_USER, "TASK_ACCEPTED", "t", "b", "BK:A:" + OTHER_USER);

        notifyService.markAllRead(USER);

        assertEquals(0, notifyService.countUnread(USER));
        assertEquals(1, notifyService.countUnread(OTHER_USER), "他人的未读不应被影响");
    }
}
