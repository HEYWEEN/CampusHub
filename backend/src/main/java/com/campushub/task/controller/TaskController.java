package com.campushub.task.controller;

import com.campushub.common.PublicUserVO;
import com.campushub.common.response.ApiResponse;
import com.campushub.common.response.PageResponse;
import com.campushub.common.util.CurrentUserHolder;
import com.campushub.task.dto.*;
import com.campushub.task.entity.Task;
import com.campushub.task.service.TaskService;
import com.campushub.task.vo.*;
import com.campushub.user.api.UserApi;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TaskController {

    private final TaskService taskService;
    private final UserApi userApi;

    public TaskController(TaskService taskService, UserApi userApi) {
        this.taskService = taskService;
        this.userApi = userApi;
    }

    // -- TASK-02 发布 -------------------------------------------------

    @PostMapping("/api/tasks")
    public ApiResponse<TaskIdVO> create(@Valid @RequestBody TaskCreateDTO dto) {
        long userId = CurrentUserHolder.getUserId();
        Task task = taskService.create(userId, dto);
        return ApiResponse.success(new TaskIdVO(task.getId()));
    }

    // -- TASK-06 大厅 -------------------------------------------------

    @GetMapping("/api/search/tasks")
    public ApiResponse<PageResponse<TaskListItemVO>> search(@Valid TaskQueryDTO query) {
        Page<Task> page = taskService.search(query);
        List<TaskListItemVO> items = page.getContent().stream().map(task -> {
            PublicUserVO pub = userApi.getPublicUser(task.getPublisherId());
            return TaskListItemVO.from(task, pub);
        }).toList();
        return ApiResponse.success(PageResponse.of(items, page.getTotalElements(),
                page.getNumber() + 1, page.getSize()));
    }

    // -- TASK-06 详情 -------------------------------------------------

    @GetMapping("/api/tasks/{taskId}")
    public ApiResponse<TaskDetailVO> getDetail(@PathVariable long taskId) {
        Long currentUserId = CurrentUserHolder.getUserIdOrNull();
        return ApiResponse.success(taskService.getDetail(taskId, currentUserId));
    }

    // -- TASK-03 编辑 -------------------------------------------------

    @PatchMapping("/api/tasks/{taskId}")
    public ApiResponse<TaskDetailVO> update(
            @PathVariable long taskId,
            @Valid @RequestBody TaskUpdateDTO dto) {
        long userId = CurrentUserHolder.getUserId();
        Task task = taskService.update(userId, taskId, dto);
        Long self = userId;
        return ApiResponse.success(taskService.getDetail(task.getId(), self));
    }

    // -- TASK-03 取消 -------------------------------------------------

    @PostMapping("/api/tasks/{taskId}/cancel")
    public ApiResponse<Void> cancel(
            @PathVariable long taskId,
            @Valid @RequestBody TaskCancelDTO dto) {
        long userId = CurrentUserHolder.getUserId();
        taskService.cancel(userId, taskId, dto.getReason());
        return ApiResponse.success();
    }

    // -- TASK-04 接单 -------------------------------------------------

    @PostMapping("/api/tasks/{taskId}/accept")
    public ApiResponse<Void> accept(
            @PathVariable long taskId,
            @Valid @RequestBody TaskAcceptDTO dto) {
        long userId = CurrentUserHolder.getUserId();
        taskService.accept(userId, taskId, dto.getVersion());
        return ApiResponse.success();
    }

    // -- TASK-05 上传凭证 ----------------------------------------------

    /**
     * 提交任务凭证。schema_audit A-8 修复：原本是 multipart，改成 JSON + 预上传 URL。
     */
    @PostMapping("/api/tasks/{taskId}/proof")
    public ApiResponse<TaskProofVO> submitProof(
            @PathVariable long taskId,
            @Valid @RequestBody TaskProofSubmitDTO dto) {
        long userId = CurrentUserHolder.getUserId();
        return ApiResponse.success(taskService.submitProof(userId, taskId, dto.getImages(), dto.getText()));
    }

    // -- TASK-05 确认完成 ----------------------------------------------

    @PostMapping("/api/tasks/{taskId}/confirm")
    public ApiResponse<TaskDetailVO> confirmComplete(@PathVariable long taskId) {
        long userId = CurrentUserHolder.getUserId();
        Task task = taskService.confirmComplete(userId, taskId);
        Long self = userId;
        return ApiResponse.success(taskService.getDetail(task.getId(), self));
    }

    // -- TASK-08 延长截止 ----------------------------------------------

    @PostMapping("/api/tasks/{taskId}/extend")
    public ApiResponse<TaskExtendVO> extend(
            @PathVariable long taskId,
            @Valid @RequestBody TaskExtendDTO dto) {
        long userId = CurrentUserHolder.getUserId();
        return ApiResponse.success(
                taskService.extend(userId, taskId, dto.getAdditionalMinutes()));
    }

    // -- TASK-08 接单上限调整 ------------------------------------------

    @PatchMapping("/api/users/me/accept-limit")
    public ApiResponse<AcceptLimitVO> updateAcceptLimit(
            @Valid @RequestBody AcceptLimitPatchDTO dto) {
        long userId = CurrentUserHolder.getUserId();
        return ApiResponse.success(
                taskService.updateAcceptLimit(userId, dto.getDailyAcceptLimit()));
    }
}
