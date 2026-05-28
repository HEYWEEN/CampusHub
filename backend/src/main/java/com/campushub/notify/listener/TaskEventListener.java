package com.campushub.notify.listener;

import com.campushub.notify.api.NotifyApi;
import com.campushub.notify.service.NotifyTemplate;
import com.campushub.task.event.TaskAcceptedEvent;
import com.campushub.task.event.TaskCanceledEvent;
import com.campushub.task.event.TaskCompletedEvent;
import com.campushub.task.event.TaskExpiredEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * NTF-02：订阅 task 模块的 4 类业务事件，转化为站内信。
 *
 * <p><b>事务时机</b>：用 {@code @TransactionalEventListener(phase = AFTER_COMMIT)} ——
 * task 业务事务先提交，notify 写入失败也不回滚业务（避免"通知失败导致业务回滚"的耦合）。
 * 同步执行（项目当前未开启 @EnableAsync），单条 INSERT 延迟可忽略。
 *
 * <p><b>通知谁</b>：
 * <ul>
 *   <li>TaskAcceptedEvent  → publisher（"你的任务被接单了"）</li>
 *   <li>TaskCompletedEvent → 双方（"任务完成，可以评价了"）</li>
 *   <li>TaskCanceledEvent  → 双方（accepter 为 null 时只通知 publisher）</li>
 *   <li>TaskExpiredEvent   → 双方（accepter 为 null 时只通知 publisher）</li>
 * </ul>
 *
 * <p><b>幂等</b>：bizKey = {@code TYPE:taskId:userId}，同事件重投不会重复发信
 * （uk_notify_biz_key + service 内 existsByBizKey 双层兜底）。
 */
@Component
public class TaskEventListener {

    private final NotifyApi notifyApi;

    public TaskEventListener(NotifyApi notifyApi) {
        this.notifyApi = notifyApi;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskAccepted(TaskAcceptedEvent event) {
        notifyOne(NotifyTemplate.TASK_ACCEPTED, event.taskId(), event.publisherId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskCompleted(TaskCompletedEvent event) {
        notifyBoth(NotifyTemplate.TASK_COMPLETED, event.taskId(), event.publisherId(), event.accepterId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskCanceled(TaskCanceledEvent event) {
        notifyBoth(NotifyTemplate.TASK_CANCELED, event.taskId(), event.publisherId(), event.accepterId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskExpired(TaskExpiredEvent event) {
        notifyBoth(NotifyTemplate.TASK_EXPIRED, event.taskId(), event.publisherId(), event.accepterId());
    }

    private void notifyOne(NotifyTemplate tpl, long taskId, Long userId) {
        if (userId == null) return;
        notifyApi.appendLetter(
                userId,
                tpl.name(),
                tpl.renderTitle(),
                tpl.renderBody(taskId),
                tpl.bizKey(taskId, userId));
    }

    private void notifyBoth(NotifyTemplate tpl, long taskId, Long publisherId, Long accepterId) {
        notifyOne(tpl, taskId, publisherId);
        notifyOne(tpl, taskId, accepterId);
    }
}
