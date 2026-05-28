package com.campushub.notify.vo;

import java.time.Instant;

/**
 * 站内信出参（与前端 types/credit.ts NotifyMessageVO 字段对齐）。
 *
 * <p>{@code readAt} 为 NULL 时前端判定未读。{@code bizKey} 透传供前端调试 / 跳转目标。
 */
public record NotifyMessageVO(
        Long id,
        String type,
        String title,
        String body,
        Instant readAt,
        Instant createdAt,
        String bizId) {
}
