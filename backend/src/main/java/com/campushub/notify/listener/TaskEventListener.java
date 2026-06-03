package com.campushub.notify.listener;

import com.campushub.notify.api.NotifyApi;
import com.campushub.notify.service.NotifyTemplate;
import com.campushub.task.event.TaskAcceptedEvent;
import com.campushub.task.event.TaskCanceledEvent;
import com.campushub.task.event.TaskCompletedEvent;
import com.campushub.task.event.TaskExpiredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * NTF-02：订阅 task 模块的 4 类业务事件，转化为站内信。
 *
 * <p><b>事务时机</b>：用 {@code @EventListener} 同步执行 ——
 * notify 写入与 task 业务事务在同一 TX 内，通知写入失败会回滚业务。
 * （原设计用 {@code @TransactionalEventListener(phase = AFTER_COMMIT)}，
 * 但在 MariaDB + JPA 组合下事务同步未触发，改为同步监听。）
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

    private static final Logger log = LoggerFactory.getLogger(TaskEventListener.class);

    private final NotifyApi notifyApi;

    public TaskEventListener(NotifyApi notifyApi) {
        this.notifyApi = notifyApi;
    }

    @EventListener
    public void onTaskAccepted(TaskAcceptedEvent event) {
        log.info("notify: task {} accepted, notifying publisher {}", event.taskId(), event.publisherId());
        notifyOne(NotifyTemplate.TASK_ACCEPTED, event.taskId(), event.publisherId());
    }

    @EventListener
    public void onTaskCompleted(TaskCompletedEvent event) {
        notifyBoth(NotifyTemplate.TASK_COMPLETED, event.taskId(), event.publisherId(), event.accepterId());
    }

    @EventListener
    public void onTaskCanceled(TaskCanceledEvent event) {
        notifyBoth(NotifyTemplate.TASK_CANCELED, event.taskId(), event.publisherId(), event.accepterId());
    }

    @EventListener
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
