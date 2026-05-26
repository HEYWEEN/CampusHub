package com.campushub.task.service;

import com.campushub.task.dto.*;
import com.campushub.task.entity.Task;
import com.campushub.task.vo.*;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TaskService {

    /** TASK-02 发布任务 */
    Task create(long userId, TaskCreateDTO dto);

    /** TASK-06 任务大厅 */
    Page<Task> search(TaskQueryDTO query);

    /** TASK-06 任务详情 */
    TaskDetailVO getDetail(long taskId, Long currentUserId);

    /** TASK-03 编辑任务 */
    Task update(long userId, long taskId, TaskUpdateDTO dto);

    /** TASK-03 取消任务 */
    void cancel(long userId, long taskId, String reason);

    /** TASK-04 接单 */
    void accept(long userId, long taskId, int expectedVersion);

    /** TASK-05 上传凭证 */
    TaskProofVO submitProof(long userId, long taskId, List<MultipartFile> images, String text);

    /** TASK-05 确认完成 */
    Task confirmComplete(long userId, long taskId);

    /** TASK-08 延长截止 */
    TaskExtendVO extend(long userId, long taskId, int additionalMinutes);

    /** TASK-08 接单上限调整 */
    AcceptLimitVO updateAcceptLimit(long userId, int limit);
}
