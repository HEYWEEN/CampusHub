package com.campushub.user.service;

/**
 * 敏感词检测策略（Strategy 模式）。
 *
 * 当前阶段：DefaultSensitiveWordChecker 用内存词库（看板 USER-01 工时只有 1.5h，不接管理端词库）。
 * 后续 admin 模块 ADM-XX 完工后：替换为 ForbiddenWordRepositoryChecker（读 admin_forbidden_word 表）。
 *
 * edu 模块 EDU-05 辅导发布也复用本接口（C 改造时把 default 替换为 admin 表实现即可）。
 */
public interface SensitiveWordChecker {

    /**
     * @return 命中的敏感词；未命中返回 null
     */
    String firstHit(String text);

    default boolean isClean(String text) {
        return firstHit(text) == null;
    }
}
