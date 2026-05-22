package com.campushub.user.repository;

import com.campushub.user.entity.UserAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAuditLogRepository extends JpaRepository<UserAuditLog, Long> {

    /** 按用户取最新 N 条审计 — 供 admin 后续查询 */
    List<UserAuditLog> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);
}
