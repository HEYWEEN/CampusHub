package com.campushub.task.scheduler;

import com.campushub.task.entity.Task;
import com.campushub.task.entity.TaskStatus;
import com.campushub.task.event.TaskExpiredEvent;
import com.campushub.task.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class TaskTimeoutScanner {

    private static final Logger log = LoggerFactory.getLogger(TaskTimeoutScanner.class);

    private final TaskRepository taskRepo;
    private final ApplicationEventPublisher events;

    public TaskTimeoutScanner(TaskRepository taskRepo, ApplicationEventPublisher events) {
        this.taskRepo = taskRepo;
        this.events = events;
    }

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void scan() {
        Instant now = Instant.now();

        List<Task> expired = taskRepo.findActiveWithDeadlineBefore(now);

        for (Task task : expired) {
            task.setStatus(TaskStatus.EXPIRED);
            taskRepo.save(task);
            events.publishEvent(new TaskExpiredEvent(task.getId(), task.getVersion()));
        }

        if (!expired.isEmpty()) {
            log.info("timeout-scan: expired {} tasks", expired.size());
        }
    }
}
