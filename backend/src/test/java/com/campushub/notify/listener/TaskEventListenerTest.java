package com.campushub.notify.listener;

import com.campushub.notify.repository.NotifyMessageRepository;
import com.campushub.notify.service.NotifyService;
import com.campushub.task.event.TaskAcceptedEvent;
import com.campushub.task.event.TaskCanceledEvent;
import com.campushub.task.event.TaskCompletedEvent;
import com.campushub.task.event.TaskExpiredEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * NTF-02 单测：4 类 task 事件 → notify 站内信触达。
 *
 * <p>直接调 listener 方法（参考 D 的 TradeEventListenerTest），绕过 @TransactionalEventListener
 * 在无事务上下文中不触发的限制。要测的是「事件 payload → notify 写入」的纯映射逻辑。
 */
@SpringBootTest
@ActiveProfiles("test")
class TaskEventListenerTest {

    @Autowired private TaskEventListener listener;
    @Autowired private NotifyService notifyService;
    @Autowired private NotifyMessageRepository repo;

    private static final long TASK_ID = 5001L;
    private static final long PUBLISHER = 1001L;
    private static final long ACCEPTER = 1002L;

    @BeforeEach
    void cleanup() {
        repo.deleteAll();
    }

    @Test
    void onTaskAccepted_notifiesPublisherOnly() {
        listener.onTaskAccepted(new TaskAcceptedEvent(TASK_ID, PUBLISHER, ACCEPTER, 10, 1L));

        assertEquals(1, notifyService.listMine(PUBLISHER, "all").size());
        assertEquals(0, notifyService.listMine(ACCEPTER, "all").size(), "接单者不收 ACCEPTED");
        assertEquals("TASK_ACCEPTED", notifyService.listMine(PUBLISHER, "all").get(0).type());
    }

    @Test
    void onTaskCompleted_notifiesBoth() {
        listener.onTaskCompleted(new TaskCompletedEvent(TASK_ID, PUBLISHER, ACCEPTER, 20, 1L));

        assertEquals(1, notifyService.listMine(PUBLISHER, "all").size());
        assertEquals(1, notifyService.listMine(ACCEPTER, "all").size());
    }

    @Test
    void onTaskCanceled_withNullAccepter_notifiesPublisherOnly() {
        listener.onTaskCanceled(new TaskCanceledEvent(TASK_ID, PUBLISHER, null, 10, 5, 1L));

        assertEquals(1, notifyService.listMine(PUBLISHER, "all").size());
    }

    @Test
    void onTaskExpired_notifiesBoth() {
        listener.onTaskExpired(new TaskExpiredEvent(TASK_ID, PUBLISHER, ACCEPTER, 1L));

        assertEquals(1, notifyService.listMine(PUBLISHER, "all").size());
        assertEquals(1, notifyService.listMine(ACCEPTER, "all").size());
        assertEquals("TASK_EXPIRED", notifyService.listMine(PUBLISHER, "all").get(0).type());
    }

    @Test
    void replayedEvent_isIdempotent() {
        TaskCompletedEvent evt = new TaskCompletedEvent(TASK_ID, PUBLISHER, ACCEPTER, 20, 1L);
        listener.onTaskCompleted(evt);
        listener.onTaskCompleted(evt); // 重投

        assertEquals(1, notifyService.listMine(PUBLISHER, "all").size(), "同事件重投不应产生重复站内信");
        assertEquals(1, notifyService.listMine(ACCEPTER, "all").size());
        assertEquals(2, repo.count());
    }
}
