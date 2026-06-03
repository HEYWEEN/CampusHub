package com.campushub.task.repository;

import com.campushub.task.entity.Task;
import com.campushub.task.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    @Query("SELECT COUNT(t) FROM Task t WHERE t.assigneeId = :userId " +
           "AND t.status NOT IN (3, 4, 5) AND t.deletedAt IS NULL")
    int countInProgressBy(@Param("userId") long userId);

    @Query("SELECT t FROM Task t WHERE t.status IN (0, 1, 2) " +
           "AND t.deadlineAt < :now AND t.deletedAt IS NULL")
    List<Task> findActiveWithDeadlineBefore(@Param("now") Instant now);

    Optional<Task> findByIdAndDeletedAtIsNull(Long id);

    // ── 推荐（P2，只读）：候选集 + 用户历史 ──
    List<Task> findTop100ByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(TaskStatus status);

    List<Task> findTop50ByPublisherIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long publisherId);

    List<Task> findTop50ByAssigneeIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long assigneeId);
}
