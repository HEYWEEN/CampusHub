package com.campushub.task.repository;

import com.campushub.task.entity.TaskExtendLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskExtendLogRepository extends JpaRepository<TaskExtendLog, Long> {

    int countByTaskId(Long taskId);
}
