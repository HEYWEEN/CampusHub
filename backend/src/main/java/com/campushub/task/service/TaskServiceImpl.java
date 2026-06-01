package com.campushub.task.service;

import com.campushub.common.PublicUserVO;
import com.campushub.common.enums.VerifyStatus;
import com.campushub.common.exception.BizException;
import com.campushub.common.exception.NotFoundException;
import com.campushub.common.util.CurrentUserHolder;
import com.campushub.credit.api.CreditApi;
import com.campushub.task.api.TaskApi;
import com.campushub.task.dto.*;
import com.campushub.task.entity.*;
import com.campushub.task.event.*;
import com.campushub.task.exception.TaskErrorCode;
import com.campushub.task.repository.*;
import com.campushub.task.service.state.TaskStateContext;
import com.campushub.task.vo.*;
import com.campushub.user.api.UserApi;
import com.campushub.user.entity.UserProfile;
import com.campushub.user.repository.UserProfileRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class TaskServiceImpl implements TaskService, TaskApi {

    private static final int MAX_EXTEND_COUNT = 2;
    private static final int MAX_EXTEND_MINUTES = 120;
    private static final int MAX_PROOF_IMAGES = 3;
    private static final int MAX_PROOF_TEXT_LEN = 300;
    /** 发布/接单的信用分门槛。 */
    private static final int MIN_CREDIT_SCORE = 60;

    /** 接单押金：奖励积分的 1/5，最低 5 分。 */
    private static int depositOf(int rewardPoint) {
        return Math.max(rewardPoint / 5, 5);
    }

    private final TaskRepository taskRepo;
    private final TaskAttachmentRepository attachRepo;
    private final TaskExtendLogRepository extendLogRepo;
    private final CreditApi creditApi;
    private final UserApi userApi;
    private final UserProfileRepository profileRepo;
    private final ApplicationEventPublisher events;

    public TaskServiceImpl(TaskRepository taskRepo,
                           TaskAttachmentRepository attachRepo,
                           TaskExtendLogRepository extendLogRepo,
                           CreditApi creditApi,
                           UserApi userApi,
                           UserProfileRepository profileRepo,
                           ApplicationEventPublisher events) {
        this.taskRepo = taskRepo;
        this.attachRepo = attachRepo;
        this.extendLogRepo = extendLogRepo;
        this.creditApi = creditApi;
        this.userApi = userApi;
        this.profileRepo = profileRepo;
        this.events = events;
    }

    // ==================== TaskApi 跨模块接口 ====================

    @Override
    @Transactional(readOnly = true)
    public TaskStatus getTaskStatus(long taskId) {
        return findTask(taskId).getStatus();
    }

    @Override
    @Transactional(readOnly = true)
    public Long getPublisherId(long taskId) {
        return findTask(taskId).getPublisherId();
    }

    @Override
    @Transactional(readOnly = true)
    public int countInProgressBy(long userId) {
        return taskRepo.countInProgressBy(userId);
    }

    // ==================== TASK-02 发布任务 ====================

    @Override
    @Transactional
    public Task create(long userId, TaskCreateDTO dto) {
        VerifyStatus vs = userApi.getVerifyStatus(userId);
        if (vs != VerifyStatus.APPROVED) {
            throw new BizException(TaskErrorCode.NOT_VERIFIED, "需完成学生证认证", 403);
        }

        int score = creditApi.getScoreOf(userId);
        if (score < MIN_CREDIT_SCORE) {
            throw new BizException(TaskErrorCode.CREDIT_TOO_LOW, "信用分不足 " + MIN_CREDIT_SCORE + "，无法发布任务", 403);
        }

        Task task = taskRepo.save(new Task(
                userId,
                dto.getTitle(),
                dto.getTaskType(),
                TaskStatus.PENDING_ACCEPT,
                dto.getRewardPoint(),
                dto.getDeadlineAt(),
                dto.getPickupHint(),
                dto.getDeliveryBuilding(),
                dto.getRemark()
        ));

        creditApi.freeze(userId, dto.getRewardPoint(), "task:" + task.getId() + ":freeze");

        return task;
    }

    // ==================== TASK-06 任务大厅 + 详情 ====================

    @Override
    @Transactional(readOnly = true)
    public Page<Task> search(TaskQueryDTO query) {
        Specification<Task> spec = (root, q, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.isNull(root.get("deletedAt")));

            if (query.getTaskType() != null)
                preds.add(cb.equal(root.get("taskType"), query.getTaskType()));
            if (query.getStatus() != null) {
                preds.add(cb.equal(root.get("status"), query.getStatus()));
            }
            if (query.getDeadlineFrom() != null)
                preds.add(cb.greaterThanOrEqualTo(root.get("deadlineAt"), query.getDeadlineFrom()));
            if (query.getDeadlineTo() != null)
                preds.add(cb.lessThanOrEqualTo(root.get("deadlineAt"), query.getDeadlineTo()));
            if (query.getQ() != null && !query.getQ().isBlank()) {
                String like = "%" + query.getQ() + "%";
                preds.add(cb.or(
                    cb.like(root.get("title"), like),
                    cb.like(root.get("remark"), like)
                ));
            }

            return cb.and(preds.toArray(new Predicate[0]));
        };

        Sort.Direction dir = "asc".equalsIgnoreCase(
                query.getSort().contains(",asc") ? "asc" : "desc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortField = query.getSort().split(",")[0];
        Pageable pageable = PageRequest.of(query.getPage() - 1, query.getSize(), Sort.by(dir, sortField));

        return taskRepo.findAll(spec, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDetailVO getDetail(long taskId, Long currentUserId) {
        Task task = findTask(taskId);

        PublicUserVO publisher = userApi.getPublicUser(task.getPublisherId());
        PublicUserVO assignee = null;
        if (task.getAssigneeId() != null) {
            assignee = userApi.getPublicUser(task.getAssigneeId());
        }

        List<String> urls = attachRepo.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(TaskAttachment::getUrl)
                .toList();

        boolean canAccept = false;
        boolean isPublisher = false;
        if (currentUserId != null) {
            isPublisher = currentUserId.equals(task.getPublisherId());
            if (!isPublisher && task.getStatus() == TaskStatus.PENDING_ACCEPT) {
                canAccept = creditApi.getScoreOf(currentUserId) >= MIN_CREDIT_SCORE;
            }
        }

        return TaskDetailVO.from(task, publisher, assignee, urls, canAccept, isPublisher);
    }

    // ==================== TASK-03 编辑 ====================

    @Override
    @Transactional
    public Task update(long userId, long taskId, TaskUpdateDTO dto) {
        Task task = findTask(taskId);

        if (task.getPublisherId() != userId) {
            throw new BizException(TaskErrorCode.NOT_PUBLISHER, "仅发布者可编辑", 403);
        }

        new TaskStateContext(task).edit(dto);
        return taskRepo.save(task);
    }

    // ==================== TASK-03 取消 ====================

    @Override
    @Transactional
    public void cancel(long userId, long taskId, String reason) {
        Task task = findTask(taskId);

        boolean isPublisher = task.getPublisherId() == userId;
        boolean isAssignee = task.getAssigneeId() != null && task.getAssigneeId() == userId;

        if (!isPublisher && !isAssignee) {
            throw new BizException(TaskErrorCode.NOT_INVOLVED, "仅发布者或接单者可取消", 403);
        }

        if (task.getStatus() == TaskStatus.PENDING_ACCEPT && !isPublisher) {
            throw new BizException(TaskErrorCode.NOT_PUBLISHER, "仅发布者可取消待接单任务", 403);
        }

        Long assigneeId = task.getAssigneeId();
        int reward = task.getRewardPoint();
        int deposit = depositOf(reward);

        new TaskStateContext(task).cancel();
        taskRepo.save(task);

        creditApi.unfreeze(task.getPublisherId(), reward, "task:" + taskId + ":unfreeze_reward");

        if (assigneeId != null) {
            creditApi.unfreeze(assigneeId, deposit, "task:" + taskId + ":unfreeze_deposit");
        }

        events.publishEvent(new TaskCanceledEvent(
                task.getId(), task.getPublisherId(), assigneeId,
                reward, assigneeId != null ? deposit : null, task.getVersion()));
    }

    // ==================== TASK-04 接单 ====================

    @Override
    @Transactional
    public void accept(long userId, long taskId, int expectedVersion) {
        int score = creditApi.getScoreOf(userId);
        if (score < MIN_CREDIT_SCORE) {
            throw new BizException(TaskErrorCode.CREDIT_TOO_LOW, "信用分不足 " + MIN_CREDIT_SCORE + "，无法接单", 403);
        }

        Task task = findTask(taskId);

        if (task.getPublisherId() == userId) {
            throw new BizException(TaskErrorCode.SELF_ACCEPT, "不能接自己发布的任务");
        }

        if (task.getVersion() != expectedVersion) {
            throw new BizException(TaskErrorCode.VERSION_CONFLICT, "任务已被其他人抢先接单");
        }

        int inProgress = taskRepo.countInProgressBy(userId);
        int limit = profileRepo.findByUserId(userId)
                .map(UserProfile::getDailyAcceptLimit)
                .orElse(2);
        if (inProgress >= limit) {
            throw new BizException(TaskErrorCode.ACCEPT_LIMIT, "接单已达上限(" + limit + ")", 409);
        }

        TaskStateContext ctx = new TaskStateContext(task);
        ctx.accept(userId);

        int deposit = depositOf(task.getRewardPoint());
        creditApi.freeze(userId, deposit, "task:" + taskId + ":deposit");

        taskRepo.save(task);

        events.publishEvent(new TaskAcceptedEvent(
                task.getId(), task.getPublisherId(), userId, deposit, task.getVersion()));
    }

    // ==================== TASK-05 上传凭证 ====================

    @Override
    @Transactional
    public TaskProofVO submitProof(long userId, long taskId,
                                    List<String> imageUrls, String text) {
        Task task = findTask(taskId);

        if (task.getAssigneeId() == null || task.getAssigneeId() != userId) {
            throw new BizException(TaskErrorCode.NOT_ASSIGNEE, "仅接单者可上传凭证", 403);
        }

        int imgCount = imageUrls == null ? 0 : imageUrls.size();
        if (imgCount > MAX_PROOF_IMAGES) {
            throw new BizException(TaskErrorCode.PROOF_IMAGE_TOO_MANY,
                    "凭证图片最多 " + MAX_PROOF_IMAGES + " 张", 400);
        }
        if (text != null && text.length() > MAX_PROOF_TEXT_LEN) {
            throw new BizException(TaskErrorCode.PROOF_TEXT_TOO_LONG,
                    "凭证说明不超过 " + MAX_PROOF_TEXT_LEN + " 字", 400);
        }

        new TaskStateContext(task).submitProof();

        // schema_audit A-8 修复：URL 由前端预先上传到 /api/uploads，service 直接落库
        List<String> urls = new ArrayList<>();
        if (imageUrls != null) {
            for (String url : imageUrls) {
                if (url == null || url.isBlank()) continue;
                attachRepo.save(new TaskAttachment(taskId, url, 0));
                urls.add(url);
            }
        }

        taskRepo.save(task);
        return new TaskProofVO(taskId, task.getStatus(), text, urls);
    }

    // ==================== TASK-05 确认完成 ====================

    @Override
    @Transactional
    public Task confirmComplete(long userId, long taskId) {
        Task task = findTask(taskId);

        if (task.getPublisherId() != userId) {
            throw new BizException(TaskErrorCode.NOT_PUBLISHER, "仅发布者可确认完成", 403);
        }

        new TaskStateContext(task).confirm();

        int deposit = depositOf(task.getRewardPoint());
        creditApi.unfreeze(task.getAssigneeId(), deposit,
                "task:" + taskId + ":unfreeze_deposit");

        taskRepo.save(task);

        events.publishEvent(new TaskCompletedEvent(
                task.getId(), task.getPublisherId(), task.getAssigneeId(),
                task.getRewardPoint(), task.getVersion()));

        return task;
    }

    // ==================== TASK-08 延长截止 ====================

    @Override
    @Transactional
    public TaskExtendVO extend(long userId, long taskId, int additionalMinutes) {
        Task task = findTask(taskId);

        if (task.getPublisherId() != userId) {
            throw new BizException(TaskErrorCode.NOT_PUBLISHER, "仅发布者可操作", 403);
        }

        int ec = extendLogRepo.countByTaskId(taskId);
        if (ec >= MAX_EXTEND_COUNT) {
            throw new BizException(TaskErrorCode.EXTEND_LIMIT,
                    "延长次数已达上限(" + MAX_EXTEND_COUNT + ")", 409);
        }
        if (additionalMinutes < 1 || additionalMinutes > MAX_EXTEND_MINUTES) {
            throw new BizException(TaskErrorCode.EXTEND_RANGE,
                    "单次延长 1~" + MAX_EXTEND_MINUTES + " 分钟", 409);
        }

        Instant old = task.getDeadlineAt();
        Instant next = old.plusSeconds(additionalMinutes * 60L);

        new TaskStateContext(task).extend(next);

        extendLogRepo.save(new TaskExtendLog(taskId, old, next, ec + 1));
        taskRepo.save(task);

        return new TaskExtendVO(taskId, next, ec + 1);
    }

    // ==================== TASK-08 接单上限调整 ====================

    @Override
    @Transactional
    public AcceptLimitVO updateAcceptLimit(long userId, int limit) {
        UserProfile profile = profileRepo.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("用户资料缺失: " + userId));
        profile.setDailyAcceptLimit(limit);
        profileRepo.save(profile);
        return new AcceptLimitVO(limit);
    }

    // ==================== 内部工具 ====================

    private Task findTask(long taskId) {
        return taskRepo.findByIdAndDeletedAtIsNull(taskId)
                .orElseThrow(() -> new NotFoundException("任务不存在: " + taskId));
    }
}
