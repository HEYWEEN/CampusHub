# 实验记录：AI 代码信任度实验 — 订单状态更新

> 实验者：陈旭枫(B)
> 日期：2026-06-05
> AI 实验对象：gemini


## 功能点选择

**订单状态更新**（task_order 表），覆盖：
- 状态机：PENDING_ACCEPT → IN_PROGRESS → WAIT_CONFIRM → COMPLETED / CANCELED / EXPIRED
- 核心操作：接单、上传凭证、确认完成、取消、编辑、延期、超时扫描
- 涉及信用分冻结/解冻、乐观锁防并发、事件发布


## 步骤一：AI 直接生成

### 1.1 Prompt


请你为我实现一个校园任务平台的订单状态更新功能的全部后端代码。

业务背景:这是一个校园跑腿/互助任务平台。用户可以发布任务（如代取快递、帮忙占座），其他用户接单完成后获得积分奖励。一个任务从发布到完成有多个状态，不同状态下允许的操作不同。
任务状态流转（状态机）:
```
PENDING_ACCEPT (0) 待接单  →  可接单 / 可取消(发布者) / 可超时 / 可编辑(发布者)
        ↓ 有人接单
IN_PROGRESS (1)    进行中  →  可上传凭证(接单者) / 可取消(双方) / 可超时 / 可延期(发布者)
        ↓ 接单者上传完成凭证
WAIT_CONFIRM (2)   待确认  →  可确认完成(发布者) / 可超时
        ↓ 发布者确认
COMPLETED (3)      已完成  →  终态，不可操作
CANCELED  (4)      已取消  →  终态，不可操作
EXPIRED   (5)      已超时  →  终态，不可操作
```

规则：
- 终态（COMPLETED / CANCELED / EXPIRED）不可做任何操作
- 不在上述任一状态下执行不允许的操作时，应抛出业务异常（409 Conflict）
- CANCELED 和 EXPIRED 可从多个非终态到达（如 PENDING_ACCEPT→CANCELED，IN_PROGRESS→EXPIRED 等）
各操作的核心业务逻辑:
1. 接单（accept）
- 接单者不能是发布者本人
- 需要乐观锁（version 字段）防并发，version 不匹配时拒绝
- 接单者信用分需 ≥ 60
- 接单者有每日接单上限（默认 2），可在用户资料中配置
- 接单后冻结接单者押金 = max(任务奖励积分 / 5, 5)
- 状态变为 IN_PROGRESS
2. 上传凭证（submitProof）
- 仅接单者可操作
- 凭证图片最多 3 张，说明文字最多 300 字
- 状态变为 WAIT_CONFIRM
3. 确认完成（confirm）
- 仅发布者可操作
- 解冻接单者押金
- 发布事件通知积分系统发放奖励
- 状态变为 COMPLETED
4. 取消（cancel）
- 发布者或接单者均可取消（PENDING_ACCEPT 状态仅发布者可取消）
- 解冻发布者的任务奖励积分
- 如果已有接单者，也解冻接单者押金
- 状态变为 CANCELED
5. 编辑（edit）
- 仅发布者可操作，仅 PENDING_ACCEPT 状态可编辑
- 可编辑字段：deliveryBuilding、deadlineAt、remark
6. 延期（extend）
- 仅发布者可操作，仅 IN_PROGRESS 状态可延期
- 每个任务最多延期 2 次
- 单次延期 1~120 分钟
- 记录每次延期的日志（旧截止时间、新截止时间、第几次延期）
7. 超时（expire）
- 由定时任务（每 5 分钟）扫描截止时间已过且仍处于活跃状态的任务
- 自动将状态置为 EXPIRED
数据模型:
task_order 表字段：
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增 |
| publisher_id | BIGINT NOT NULL | 发布者 |
| assignee_id | BIGINT | 接单者 |
| title | VARCHAR(120) | 标题 |
| task_type | INT | 类型(0跑腿/1互助/2辅导) |
| status | INT | 状态码 |
| reward_point | INT | 奖励积分 |
| deadline_at | DATETIME | 截止时间 |
| pickup_hint | VARCHAR(200) | 取件提示 |
| delivery_building | VARCHAR(120) | 送达楼栋 |
| remark | VARCHAR(500) | 备注 |
| version | INT | 乐观锁版本号 |
| created_at / updated_at | DATETIME | 时间戳 |
| deleted_at | DATETIME | 软删除标记 |
技术要求:
- 语言/框架：Java 17+, Spring Boot 3.5, Spring Data JPA (Hibernate 6.6)
- 数据库：MySQL / MariaDB（已配置好 DataSource，直接注入 Repository 即可）
- 项目结构：按功能分包（task.entity / task.service / task.repository / task.api 等）
- 异常使用 `BizException(int code, String message, int httpStatus)` 抛出
- 需要 `@Transactional` 保证数据一致性
- 接单时使用乐观锁（`@Version` + version 比对）
- 积分操作通过 `CreditApi` 接口（已提供 freeze / unfreeze 方法），你不需要实现
需要交付的文件:
请在以下包路径下生成完整的 Java 文件：

1. `task.entity.Task` — JPA Entity（对应 task_order 表）
2. `task.entity.TaskStatus` — 状态枚举 + JPA Converter（存 INT，代码中 Enum）
3. `task.entity.TaskType` — 任务类型枚举 + JPA Converter
4. `task.service.TaskService` — Service 接口
5. `task.service.TaskServiceImpl` — Service 实现（包含所有上述业务逻辑）
6. `task.repository.TaskRepository` — Spring Data JPA Repository（含必要的自定义查询）
7. `task.exception.TaskErrorCode` — 错误码常量
8. `task.scheduler.TaskTimeoutScanner` — 定时扫描超时任务
接口签名参考:
```java
// TaskService
Task create(long userId, TaskCreateDTO dto);
void accept(long userId, long taskId, int expectedVersion);
void submitProof(long userId, long taskId, List<String> imageUrls, String text);
void confirmComplete(long userId, long taskId);
void cancel(long userId, long taskId, String reason);
Task update(long userId, long taskId, TaskUpdateDTO dto);
void extend(long userId, long taskId, int additionalMinutes);
```

请直接输出所有文件的完整代码，每个文件用 `// filename: path/to/File.java` 标注。



### 1.2 AI 生成结果

#### 原始结果

研究已完成。 欢迎针对此研究进一步提问，或提出变动。

完整后端源代码

实现以下为校园任务平台订单状态更新功能的全部后端代码实现。 代码严格遵循分包规范、Java 17 语法特性以及 Spring Boot 3.5 与 Hibernate 6.6 的技术规范。1. 任务实体类// filename: task/entity/Task.javaJavapackage task.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "task_order", indexes = {
    @Index(name = "idx_status_deadline", columnList = "status, deadline_at"),
    @Index(name = "idx_assignee_updated", columnList = "assignee_id, updated_at")
})
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "publisher_id", nullable = false)
    private Long publisherId;

    @Column(name = "assignee_id")
    private Long assigneeId;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "task_type", nullable = false)
    @Convert(converter = TaskType.TaskTypeConverter.class)
    private TaskType taskType;

    @Column(name = "status", nullable = false)
    @Convert(converter = TaskStatus.TaskStatusConverter.class)
    private TaskStatus status;

    @Column(name = "reward_point", nullable = false)
    private Integer rewardPoint;

    @Column(name = "deadline_at", nullable = false)
    private LocalDateTime deadlineAt;

    @Column(name = "pickup_hint", length = 200)
    private String pickupHint;

    @Column(name = "delivery_building", length = 120)
    private String deliveryBuilding;

    @Column(name = "remark", length = 500)
    private String remark;

    @Version
    @Column(name = "version")
    private Integer version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "task_proof_image", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "image_url", length = 500)
    private List<String> proofImages = new ArrayList<>();

    @Column(name = "proof_text", length = 300)
    private String proofText;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "task_extension_log", joinColumns = @JoinColumn(name = "task_id"))
    private List<ExtensionLog> extensionLogs = new ArrayList<>();

    @Column(name = "extension_count", nullable = false)
    private Integer extensionCount = 0;

    public Task() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.extensionCount == null) {
            this.extensionCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Embeddable
    public static class ExtensionLog {
        @Column(name = "old_deadline", nullable = false)
        private LocalDateTime oldDeadline;

        @Column(name = "new_deadline", nullable = false)
        private LocalDateTime newDeadline;

        @Column(name = "extension_index", nullable = false)
        private Integer extensionIndex;

        public ExtensionLog() {}

        public ExtensionLog(LocalDateTime oldDeadline, LocalDateTime newDeadline, Integer extensionIndex) {
            this.oldDeadline = oldDeadline;
            this.newDeadline = newDeadline;
            this.extensionIndex = extensionIndex;
        }

        public LocalDateTime getOldDeadline() { return oldDeadline; }
        public LocalDateTime getNewDeadline() { return newDeadline; }
        public Integer getExtensionIndex() { return extensionIndex; }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPublisherId() { return publisherId; }
    public void setPublisherId(Long publisherId) { this.publisherId = publisherId; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public TaskType getTaskType() { return taskType; }
    public void setTaskType(TaskType taskType) { this.taskType = taskType; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public Integer getRewardPoint() { return rewardPoint; }
    public void setRewardPoint(Integer rewardPoint) { this.rewardPoint = rewardPoint; }
    public LocalDateTime getDeadlineAt() { return deadlineAt; }
    public void setDeadlineAt(LocalDateTime deadlineAt) { this.deadlineAt = deadlineAt; }
    public String getPickupHint() { return pickupHint; }
    public void setPickupHint(String pickupHint) { this.pickupHint = pickupHint; }
    public String getDeliveryBuilding() { return deliveryBuilding; }
    public void setDeliveryBuilding(String deliveryBuilding) { this.deliveryBuilding = deliveryBuilding; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    public List<String> getProofImages() { return proofImages; }
    public void setProofImages(List<String> proofImages) { this.proofImages = proofImages; }
    public String getProofText() { return proofText; }
    public void setProofText(String proofText) { this.proofText = proofText; }
    public List<ExtensionLog> getExtensionLogs() { return extensionLogs; }
    public void setExtensionLogs(List<ExtensionLog> extensionLogs) { this.extensionLogs = extensionLogs; }
    public Integer getExtensionCount() { return extensionCount; }
    public void setExtensionCount(Integer extensionCount) { this.extensionCount = extensionCount; }
}
2. 任务状态枚举与转换器// filename: task/entity/TaskStatus.javaJavapackage task.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum TaskStatus {
    PENDING_ACCEPT(0),
    IN_PROGRESS(1),
    WAIT_CONFIRM(2),
    COMPLETED(3),
    CANCELED(4),
    EXPIRED(5);

    private final int code;

    TaskStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static TaskStatus fromCode(int code) {
        for (TaskStatus status : TaskStatus.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的任務狀態代碼: " + code);
    }

    @Converter(autoApply = true)
    public static class TaskStatusConverter implements AttributeConverter<TaskStatus, Integer> {
        @Override
        public Integer convertToDatabaseColumn(TaskStatus attribute) {
            return attribute != null ? attribute.getCode() : null;
        }

        @Override
        public TaskStatus convertToEntityAttribute(Integer dbData) {
            return dbData != null ? TaskStatus.fromCode(dbData) : null;
        }
    }
}
3. 任务类型枚举与转换器// filename: task/entity/TaskType.javaJavapackage task.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum TaskType {
    RUN(0),
    MUTUAL_AID(1),
    TUTOR(2);

    private final int code;

    TaskType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static TaskType fromCode(int code) {
        for (TaskType type : TaskType.values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的任務類型代碼: " + code);
    }

    @Converter(autoApply = true)
    public static class TaskTypeConverter implements AttributeConverter<TaskType, Integer> {
        @Override
        public Integer convertToDatabaseColumn(TaskType attribute) {
            return attribute != null ? attribute.getCode() : null;
        }

        @Override
        public TaskType convertToEntityAttribute(Integer dbData) {
            return dbData != null ? TaskType.fromCode(dbData) : null;
        }
    }
}
4. 服务层接口定义// filename: task/service/TaskService.javaJavapackage task.service;

import task.entity.Task;
import java.time.LocalDateTime;
import java.util.List;

public interface TaskService {

    Task create(long userId, TaskCreateDTO dto);

    void accept(long userId, long taskId, int expectedVersion);

    void submitProof(long userId, long taskId, List<String> imageUrls, String text);

    void confirmComplete(long userId, long taskId);

    void cancel(long userId, long taskId, String reason);

    Task update(long userId, long taskId, TaskUpdateDTO dto);

    void extend(long userId, long taskId, int additionalMinutes);

    class TaskCreateDTO {
        private String title;
        private int taskType;
        private int rewardPoint;
        private LocalDateTime deadlineAt;
        private String pickupHint;
        private String deliveryBuilding;
        private String remark;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public int getTaskType() { return taskType; }
        public void setTaskType(int taskType) { this.taskType = taskType; }
        public int getRewardPoint() { return rewardPoint; }
        public void setRewardPoint(int rewardPoint) { this.rewardPoint = rewardPoint; }
        public LocalDateTime getDeadlineAt() { return deadlineAt; }
        public void setDeadlineAt(LocalDateTime deadlineAt) { this.deadlineAt = deadlineAt; }
        public String getPickupHint() { return pickupHint; }
        public void setPickupHint(String pickupHint) { this.pickupHint = pickupHint; }
        public String getDeliveryBuilding() { return deliveryBuilding; }
        public void setDeliveryBuilding(String deliveryBuilding) { this.deliveryBuilding = deliveryBuilding; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }

    class TaskUpdateDTO {
        private String deliveryBuilding;
        private LocalDateTime deadlineAt;
        private String remark;

        public String getDeliveryBuilding() { return deliveryBuilding; }
        public void setDeliveryBuilding(String deliveryBuilding) { this.deliveryBuilding = deliveryBuilding; }
        public LocalDateTime getDeadlineAt() { return deadlineAt; }
        public void setDeadlineAt(LocalDateTime deadlineAt) { this.deadlineAt = deadlineAt; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }
}
5. 服务级部门逻辑实现// filename: task/service/TaskServiceImpl.javaJavapackage task.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import task.api.CreditApi;
import task.api.UserApi;
import task.entity.Task;
import task.entity.TaskStatus;
import task.entity.TaskType;
import task.event.TaskCompletedEvent;
import task.exception.BizException;
import task.exception.TaskErrorCode;
import task.repository.TaskRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final CreditApi creditApi;
    private final UserApi userApi;
    private final ApplicationEventPublisher eventPublisher;

    public TaskServiceImpl(TaskRepository taskRepository, 
                           CreditApi creditApi, 
                           UserApi userApi, 
                           ApplicationEventPublisher eventPublisher) {
        this.taskRepository = taskRepository;
        this.creditApi = creditApi;
        this.userApi = userApi;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Task create(long userId, TaskCreateDTO dto) {
        if (dto.getDeadlineAt() == null || dto.getDeadlineAt().isBefore(LocalDateTime.now())) {
            throw new BizException(TaskErrorCode.INVALID_PARAMETER, "截止時間不能早於當前時間", 400);
        }

        // 發布任務時凍結發布者的任務獎勵積分
        creditApi.freeze(userId, dto.getRewardPoint());

        Task task = new Task();
        task.setPublisherId(userId);
        task.setTitle(dto.getTitle());
        task.setTaskType(TaskType.fromCode(dto.getTaskType()));
        task.setStatus(TaskStatus.PENDING_ACCEPT);
        task.setRewardPoint(dto.getRewardPoint());
        task.setDeadlineAt(dto.getDeadlineAt());
        task.setPickupHint(dto.getPickupHint());
        task.setDeliveryBuilding(dto.getDeliveryBuilding());
        task.setRemark(dto.getRemark());
        task.setExtensionCount(0);

        return taskRepository.save(task);
    }

    @Override
    @Transactional
    public void accept(long userId, long taskId, int expectedVersion) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BizException(TaskErrorCode.TASK_NOT_FOUND, "任務訂單未找到", 404));

        // 樂觀鎖版本校驗，防止高並發下的超賣
        if (task.getVersion() != expectedVersion) {
            throw new BizException(TaskErrorCode.CONCURRENCY_CONFLICT, "任務已被其他用戶接單，請刷新重試", 409);
        }

        // 狀態機校驗：僅待接單狀態可被接單
        if (task.getStatus() != TaskStatus.PENDING_ACCEPT) {
            throw new BizException(TaskErrorCode.INVALID_STATE_TRANSITION, "任務當前狀態不允許接單", 409);
        }

        // 業務規則校驗：接單者不能是發布者本人
        if (task.getPublisherId() == userId) {
            throw new BizException(TaskErrorCode.UNAUTHORIZED_ACTION, "發布者本人不能接單", 400);
        }

        // 業務規則校驗：接單者信用分需 ≥ 60
        int creditScore = userApi.getCreditScore(userId);
        if (creditScore < 60) {
            throw new BizException(TaskErrorCode.INSUFFICIENT_CREDIT, "您的信用分數不足，無法接單", 400);
        }

        // 業務規則校驗：接單者每日接單上限
        int dailyLimit = userApi.getDailyLimit(userId);
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long acceptedTodayCount = taskRepository.countTodayAcceptedTasks(
                userId, 
                startOfDay, 
                List.of(TaskStatus.IN_PROGRESS, TaskStatus.WAIT_CONFIRM, TaskStatus.COMPLETED)
        );
        if (acceptedTodayCount >= dailyLimit) {
            throw new BizException(TaskErrorCode.EXCEEDED_DAILY_LIMIT, "已達到每日接單數量上限", 400);
        }

        // 接單後凍結接單者押金 = max(任務獎勵積分 / 5, 5)
        int deposit = Math.max(task.getRewardPoint() / 5, 5);
        creditApi.freeze(userId, deposit);

        // 變更狀態與設置接單者
        task.setAssigneeId(userId);
        task.setStatus(TaskStatus.IN_PROGRESS);
        taskRepository.save(task);
    }

    @Override
    @Transactional
    public void submitProof(long userId, long taskId, List<String> imageUrls, String text) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BizException(TaskErrorCode.TASK_NOT_FOUND, "任務訂單未找到", 404));

        // 狀態機校驗：僅進行中狀態可上傳憑證
        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new BizException(TaskErrorCode.INVALID_STATE_TRANSITION, "當前狀態不可提交完成憑證", 409);
        }

        // 權限校驗：僅接單者可操作
        if (task.getAssigneeId() == null || task.getAssigneeId() != userId) {
            throw new BizException(TaskErrorCode.UNAUTHORIZED_ACTION, "非該任務接單者，無權操作", 403);
        }

        // 憑證校驗：圖片最多 3 張，說明文字最多 300 字
        if (imageUrls != null && imageUrls.size() > 3) {
            throw new BizException(TaskErrorCode.INVALID_PARAMETER, "憑證圖片最多上傳 3 張", 400);
        }
        if (text != null && text.length() > 300) {
            throw new BizException(TaskErrorCode.INVALID_PARAMETER, "說明文字最多支持 300 字", 400);
        }

        task.getProofImages().clear();
        if (imageUrls != null) {
            task.getProofImages().addAll(imageUrls);
        }
        task.setProofText(text);
        task.setStatus(TaskStatus.WAIT_CONFIRM);

        taskRepository.save(task);
    }

    @Override
    @Transactional
    public void confirmComplete(long userId, long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BizException(TaskErrorCode.TASK_NOT_FOUND, "任務訂單未找到", 404));

        // 狀態機校驗：僅待確認狀態可確認完成
        if (task.getStatus() != TaskStatus.WAIT_CONFIRM) {
            throw new BizException(TaskErrorCode.INVALID_STATE_TRANSITION, "當前狀態不可確認完成", 409);
        }

        // 權限校驗：僅發布者可操作
        if (task.getPublisherId() != userId) {
            throw new BizException(TaskErrorCode.UNAUTHORIZED_ACTION, "非任務發布者，無權確認完成", 403);
        }

        // 解凍接單者押金
        int deposit = Math.max(task.getRewardPoint() / 5, 5);
        creditApi.unfreeze(task.getAssigneeId(), deposit);

        // 解凍發布者在創建時預扣的積分
        creditApi.unfreeze(task.getPublisherId(), task.getRewardPoint());

        // 發布事件通知積分系統發放獎勵，維持領域事件的最終一致性
        eventPublisher.publishEvent(new TaskCompletedEvent(task.getId(), task.getPublisherId(), task.getAssigneeId(), task.getRewardPoint()));

        task.setStatus(TaskStatus.COMPLETED);
        taskRepository.save(task);
    }

    @Override
    @Transactional
    public void cancel(long userId, long taskId, String reason) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BizException(TaskErrorCode.TASK_NOT_FOUND, "任務訂單未找到", 404));

        TaskStatus currentStatus = task.getStatus();

        // 終態判斷：終態不可進行任何操作
        if (currentStatus == TaskStatus.COMPLETED || currentStatus == TaskStatus.CANCELED || currentStatus == TaskStatus.EXPIRED) {
            throw new BizException(TaskErrorCode.INVALID_STATE_TRANSITION, "任務已處於終態，無法取消", 409);
        }

        if (currentStatus == TaskStatus.PENDING_ACCEPT) {
            // PENDING_ACCEPT 狀態：僅發布者可取消
            if (task.getPublisherId() != userId) {
                throw new BizException(TaskErrorCode.UNAUTHORIZED_ACTION, "僅發布者可取消待接單任務", 403);
            }
            // 解凍發布者的任務獎勵積分
            creditApi.unfreeze(task.getPublisherId(), task.getRewardPoint());

        } else if (currentStatus == TaskStatus.IN_PROGRESS) {
            // IN_PROGRESS 狀態：雙方均可取消
            if (task.getPublisherId() != userId && (task.getAssigneeId() == null || task.getAssigneeId() != userId)) {
                throw new BizException(TaskErrorCode.UNAUTHORIZED_ACTION, "您非此任務相關人，無權取消", 403);
            }
            // 解凍發布者積分
            creditApi.unfreeze(task.getPublisherId(), task.getRewardPoint());
            // 解凍接單者押金
            if (task.getAssigneeId() != null) {
                int deposit = Math.max(task.getRewardPoint() / 5, 5);
                creditApi.unfreeze(task.getAssigneeId(), deposit);
            }
        } else {
            // 待確認狀態（WAIT_CONFIRM）不可取消（狀態機未定義 WAIT_CONFIRM -> CANCELED 流轉路徑）
            throw new BizException(TaskErrorCode.INVALID_STATE_TRANSITION, "任務已進入待確認階段，不可取消", 409);
        }

        task.setStatus(TaskStatus.CANCELED);
        taskRepository.save(task);
    }

    @Override
    @Transactional
    public Task update(long userId, long taskId, TaskUpdateDTO dto) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BizException(TaskErrorCode.TASK_NOT_FOUND, "任務訂單未找到", 404));

        // 狀態機校驗：僅 PENDING_ACCEPT 狀態可編輯
        if (task.getStatus() != TaskStatus.PENDING_ACCEPT) {
            throw new BizException(TaskErrorCode.INVALID_STATE_TRANSITION, "僅待接單狀態的任務可編輯", 409);
        }

        // 權限校驗：僅發布者可操作
        if (task.getPublisherId() != userId) {
            throw new BizException(TaskErrorCode.UNAUTHORIZED_ACTION, "非任務發布者，無權編輯", 403);
        }

        if (dto.getDeadlineAt() != null && dto.getDeadlineAt().isBefore(LocalDateTime.now())) {
            throw new BizException(TaskErrorCode.INVALID_PARAMETER, "截止時間不能早於當前時間", 400);
        }

        // 僅允許修改 deliveryBuilding、deadlineAt、remark 字段
        if (dto.getDeliveryBuilding() != null) {
            task.setDeliveryBuilding(dto.getDeliveryBuilding());
        }
        if (dto.getDeadlineAt() != null) {
            task.setDeadlineAt(dto.getDeadlineAt());
        }
        if (dto.getRemark() != null) {
            task.setRemark(dto.getRemark());
        }

        return taskRepository.save(task);
    }

    @Override
    @Transactional
    public void extend(long userId, long taskId, int additionalMinutes) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BizException(TaskErrorCode.TASK_NOT_FOUND, "任務訂單未找到", 404));

        // 狀態機校驗：僅 IN_PROGRESS 狀態可延期
        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new BizException(TaskErrorCode.INVALID_STATE_TRANSITION, "僅進行中的任務允許延期", 409);
        }

        // 權限校驗：僅發布者可操作
        if (task.getPublisherId() != userId) {
            throw new BizException(TaskErrorCode.UNAUTHORIZED_ACTION, "非任務發布者，無權操作延期", 403);
        }

        // 次數限制：每個任務最多延期 2 次
        if (task.getExtensionCount() >= 2) {
            throw new BizException(TaskErrorCode.INVALID_PARAMETER, "已達任務最大延期次數限制（2次）", 400);
        }

        // 時間限制：單次延期 1~120 分鐘
        if (additionalMinutes < 1 || additionalMinutes > 120) {
            throw new BizException(TaskErrorCode.INVALID_PARAMETER, "單次延期時間需在 1 到 120 分鐘之內", 400);
        }

        LocalDateTime oldDeadline = task.getDeadlineAt();
        LocalDateTime newDeadline = oldDeadline.plusMinutes(additionalMinutes);

        int nextCount = task.getExtensionCount() + 1;
        Task.ExtensionLog log = new Task.ExtensionLog(oldDeadline, newDeadline, nextCount);
        task.getExtensionLogs().add(log);
        task.setExtensionCount(nextCount);
        task.setDeadlineAt(newDeadline);

        taskRepository.save(task);
    }
}
6. 数据访问层接口// filename: task/repository/TaskRepository.javaJavapackage task.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import task.entity.Task;
import task.entity.TaskStatus;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("SELECT COUNT(t) FROM Task t WHERE t.assigneeId = :assigneeId AND t.updatedAt >= :startOfDay AND t.status IN :activeStatuses AND t.deletedAt IS NULL")
    long countTodayAcceptedTasks(
            @Param("assigneeId") Long assigneeId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("activeStatuses") List<TaskStatus> activeStatuses;
    );

    @Query("SELECT t FROM Task t WHERE t.status IN :activeStatuses AND t.deadlineAt < :now AND t.deletedAt IS NULL")
    List<Task> findExpiredTasks(
            @Param("activeStatuses") List<TaskStatus> activeStatuses,
            @Param("now") LocalDateTime now
    );
}
7. 自定义业务异常// filename: task/exception/BizException.javaJavapackage task.exception;

public class BizException extends RuntimeException {
    private final int code;
    private final int httpStatus;

    public BizException(int code, String message, int httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
8. 异常错误码常量// filename: task/exception/TaskErrorCode.javaJavapackage task.exception;

public final class TaskErrorCode {
    public static final int INVALID_STATE_TRANSITION = 40901;
    public static final int CONCURRENCY_CONFLICT = 40902;
    public static final int TASK_NOT_FOUND = 40401;
    public static final int UNAUTHORIZED_ACTION = 40301;
    public static final int INSUFFICIENT_CREDIT = 40001;
    public static final int EXCEEDED_DAILY_LIMIT = 40002;
    public static final int INVALID_PARAMETER = 40003;

    private TaskErrorCode() {}
}
9. 任务超时自动扫描仪// filename: task/scheduler/TaskTimeoutScanner.javaJavapackage task.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import task.api.CreditApi;
import task.entity.Task;
import task.entity.TaskStatus;
import task.repository.TaskRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class TaskTimeoutScanner {

    private static final Logger log = LoggerFactory.getLogger(TaskTimeoutScanner.class);

    private final TaskRepository taskRepository;
    private final CreditApi creditApi;

    public TaskTimeoutScanner(TaskRepository taskRepository, CreditApi creditApi) {
        this.taskRepository = taskRepository;
        this.creditApi = creditApi;
    }

    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void scanAndExpireTasks() {
        log.info("開始掃描超時未結算活躍任務...");
        LocalDateTime now = LocalDateTime.now();

        // 獲取所有處於非終態且已過期的任務
        List<Task> expiredTasks = taskRepository.findExpiredTasks(
                List.of(TaskStatus.PENDING_ACCEPT, TaskStatus.IN_PROGRESS, TaskStatus.WAIT_CONFIRM),
                now
        );

        if (expiredTasks.isEmpty()) {
            log.info("未檢測到符合超時條件的活躍任務");
            return;
        }

        for (Task task : expiredTasks) {
            try {
                TaskStatus oldStatus = task.getStatus();
                task.setStatus(TaskStatus.EXPIRED);

                // 解凍發布者預扣的獎勵積分
                creditApi.unfreeze(task.getPublisherId(), task.getRewardPoint());

                // 若已有接單者，解凍接單者的押金，退回信用賬戶
                if (task.getAssigneeId() != null) {
                    int deposit = Math.max(task.getRewardPoint() / 5, 5);
                    creditApi.unfreeze(task.getAssigneeId(), deposit);
                }

                log.info("任務 ID: {} 超時處理成功 (原狀態: {}, 新狀態: EXPIRED)", task.getId(), oldStatus);
            } catch (Exception e) {
                log.error("處理超時任務 ID: {} 時發生異常", task.getId(), e);
            }
        }

        taskRepository.saveAll(expiredTasks);
        log.info("超時任務掃描完畢，本次共變更 {} 個任務", expiredTasks.size());
    }
}
10.外部API与领域事件// filename: task/api/CreditApi.javaJavapackage task.api;

public interface CreditApi {
    void freeze(long userId, int points);
    void unfreeze(long userId, int points);
}
// filename: task/api/UserApi.javaJavapackage task.api;

public interface UserApi {
    int getCreditScore(long userId);
    int getDailyLimit(long userId);
}
// filename: task/event/TaskCompletedEvent.javaJavapackage task.event;

public record TaskCompletedEvent(
    long taskId, 
    long publisherId, 
    long assigneeId, 
    int rewardPoint
) {}


#### 摘要

| 文件 | AI 设计方式 |
|------|------------|
| `Task.java` | 贫血 Entity，`LocalDateTime` 时间类型，`@ElementCollection` 存凭证图+延期日志 |
| `TaskStatus.java` | 纯枚举 + JPA Converter，**无状态转换校验 map** |
| `TaskType.java` | `RUN`/`MUTUAL_AID`/`TUTOR`（原设计 `ERRAND`/`MUTUAL_HELP`/`TUTOR`）|
| `TaskService.java` | 接口，DTO 写成 **inner class** |
| `TaskServiceImpl.java` | **扁平 if-else** 状态判断，散落在 6 个方法中 |
| `TaskRepository.java` | 仅 2 个自定义查询 |
| `TaskTimeoutScanner.java` | cron 表达式 `0 */5 * * * *`，内嵌解冻逻辑 |
| `BizException.java` | 自建在 `task.exception` 包（与 `common.exception.BizException` 重复）|
| `CreditApi.java` / `UserApi.java` | 自建简化版（缺 bizKey，签名与现有接口不同）|

### 1.3 放入项目运行

| 指标 | 结果 |
|------|------|
| 编译是否通过 | 否， 131 个错误 |
| 功能是否可运行 | 否 |
| 测试是否通过 | 无法编译，`TaskServiceTest` 全部不通过 |


## 步骤二：人工检查与修复

### 2.1 三点检查

#### 检查 1：是否符合 P3 详细设计

不符合。 P3 设计要求的状态转换使用 **State 模式**（`TaskState` 接口 + 6 个状态类 + `TaskStateContext` 分发器），状态转换规则集中在 `TaskStatus.TRANSITIONS` map 中，并在 `Task.setStatus()` 内调用 `canTransitionTo()` 做校验。AI 的实现采用扁平 if-else，状态判断散落在 `TaskServiceImpl` 的 6 个方法里，每个方法各自检查 `task.getStatus() == X`。另外 `submitProof`/`extend`/`confirmComplete` 全部返回 `void`，不返回 VO 给前端。

#### 检查 2：是否处理了常见异常或边界情况

部分处理，多个缺口：

| 检查项 | AI代码 |
|--------|---------|
| 乐观锁版本冲突 |  有 |
| 发布者不能接自己单 |  有 |
| 接单者信用分 ≥ 60 |  有 |
| 凭证图片 ≤ 3 张 |  有 |
| 延期次数 ≤ 2 |  有 |
| 软删除过滤 | 无， 用 `findById` |
| 发布任务认证检查 |  无 |
| 发布任务信用分检查 |  无 |
| 凭证空 URL 过滤 |  无 

#### 检查 3：是否存在明显 Bug 或不合理逻辑

发现 3 个主要问题：

1. `confirmComplete()` 双重解冻 — 会导致发布者白拿奖励
   ```java
   // AI 代码：解冻发布者预扣的积分（退回了发布者自己）
   creditApi.unfreeze(task.getPublisherId(), task.getRewardPoint());
   // 然后又发事件让积分系统再转一笔
   eventPublisher.publishEvent(new TaskCompletedEvent(...));
   ```
   发布者在创建时冻结的积分本应支付给接单者（通过事件处理）。AI 在确认完成时把冻结积分退给了发布者，再发事件又转一笔——等于发布者白拿任务报酬，积分系统对不上账。

2. API 不兼容：`CreditApi.freeze()` 缺 bizKey
   ```java
   creditApi.freeze(userId, points);
   ```
   无 bizKey 导致无法做幂等、无法审计追溯。且 AI 自建的 `CreditApi` 接口与原项目 `CreditApi` 签名不同，无法编译。

   修复后变为：
   ```java
   creditApi.freeze(userId, points, "task:" + taskId + ":freeze");
   ```

1. 每日接单限额统计用 `updatedAt` 窗口
   ```java
   long count = taskRepo.countTodayAcceptedTasks(userId, startOfDay, activeStatuses);
   ```
   AI使用 updatedAt >= 当天零点 判断今日接单。但 updatedAt 每次 save 都刷新。昨天接的单今天被延期了，updatedAt 变成今天，就会被重复计入，导致限额误判。
   
   修复后 `countInProgressBy(userId)` 直接统计当前进行中的任务数，不依赖时间窗口。

### 2.2 修复项总结


| 维度 | AI 直出 | 修复后 |
|------|---------|----------------|
| 时间类型 | `LocalDateTime` | `Instant` |
| 状态管理 | if-else 散落 Service | State 模式（接口 + 6 状态类 + Context） |
| 状态校验 | 无集中校验 | `TaskStatus.TRANSITIONS` map + `setStatus()` 内嵌 |
| 冻结积分 | 无 bizKey | `"task:" + id + ":freeze"` |
| 确认完成 | 双重解冻（Bug） | 仅解冻接单者押金，积分转移由事件处理 |
| 发布门槛 | 无认证/信用分检查 | `VerifyStatus + score ≥ 60` |
| Repository | 2 个方法 | 12 个方法（含软删除、统计、推荐查询） |
| DTO 组织 | Service inner class | 独立文件 `task.dto.*` |
| 凭证存储 | `@ElementCollection` 内嵌 | 独立 Entity `TaskAttachment` |
| 延期日志 | `@Embeddable` 内嵌 | 独立 Entity `TaskExtendLog` |
| Event | 仅 `TaskCompletedEvent`（缺字段） | `TaskAcceptedEvent`/`TaskCanceledEvent`/`TaskCompletedEvent`/`TaskExpiredEvent` |
| 超时扫描 | cron 表达式 + 内嵌解冻 | `fixedDelay` + 发布 `TaskExpiredEvent` 由监听器处理 |

---

## 步骤三：实验记录表

| 指标 | AI 直出 | 人工审查修复后 |
|------|--------|-------------|
| 编译是否通过 | 否，131 错误 | 通过 |
| 功能是否可运行 | 无法启动 | 正常启动（端口 8080） |
| 测试是否通过 | 无法编译，0/252 | 全部通过（252/252，0 失败） |
| 主要问题/修复说明 | 见上表"2.2 修复内容"的 12 项差异 | — |

