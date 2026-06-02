package com.campushub.im.listener;

import com.campushub.im.service.ImService;
import com.campushub.task.event.TaskAcceptedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * F-IM-01：任务被接单后自动建任务关联会话 + 系统消息。
 *
 * <p>用 {@code AFTER_COMMIT}：task 业务事务先提交，会话创建失败也不回滚接单
 * （与 notify TaskEventListener 同款解耦策略）。{@code ImService.onTaskAccepted}
 * 自带 {@code @Transactional}，在提交后开新事务写入。零侵入 task 代码（只监听其事件）。
 */
@Component
public class TaskAcceptedImListener {

    private final ImService imService;

    public TaskAcceptedImListener(ImService imService) {
        this.imService = imService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskAccepted(TaskAcceptedEvent event) {
        imService.onTaskAccepted(event.publisherId(), event.accepterId(), event.taskId());
    }
}
