package com.campushub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启用 @Scheduled。
 *
 * 当前用途：
 *   - TokenBlacklist#purgeExpired 清理黑名单
 *   - 后续：TaskTimeoutScanner（B 的 task 模块）扫超时任务
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
}
