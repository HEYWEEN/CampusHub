package com.campushub.task.repository;

import com.campushub.task.entity.TaskAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, Long> {

    List<TaskAttachment> findByTaskIdOrderByCreatedAtAsc(Long taskId);
}
