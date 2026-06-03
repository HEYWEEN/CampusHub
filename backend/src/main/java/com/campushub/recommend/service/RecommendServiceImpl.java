package com.campushub.recommend.service;

import com.campushub.credit.api.CreditApi;
import com.campushub.recommend.model.UserPreference;
import com.campushub.task.entity.Task;
import com.campushub.task.entity.TaskStatus;
import com.campushub.task.entity.TaskType;
import com.campushub.task.repository.TaskRepository;
import com.campushub.task.vo.TaskListItemVO;
import com.campushub.user.api.UserApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务推荐实现（只读，复用现有表与跨模块 Api，不新建表）。
 *
 * <p>流程：候选集（待接单近期任务）→ 用户偏好画像（历史众数）→ {@link TaskScorer} 打分 →
 * 降序截断 → 装配 {@link TaskListItemVO}。跨模块只走 {@link UserApi} / {@link CreditApi}。
 */
@Service
public class RecommendServiceImpl implements RecommendService {

    private final TaskRepository taskRepo;
    private final TaskScorer scorer;
    private final UserApi userApi;
    private final CreditApi creditApi;

    public RecommendServiceImpl(TaskRepository taskRepo, TaskScorer scorer,
                                UserApi userApi, CreditApi creditApi) {
        this.taskRepo = taskRepo;
        this.scorer = scorer;
        this.userApi = userApi;
        this.creditApi = creditApi;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskListItemVO> recommendTasks(long userId, int limit) {
        // 1) 候选：待接单近期任务，排除自己发布 / 自己已接
        List<Task> candidates = taskRepo
                .findTop100ByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(TaskStatus.PENDING_ACCEPT)
                .stream()
                .filter(t -> !equalsLong(t.getPublisherId(), userId))
                .filter(t -> !equalsLong(t.getAssigneeId(), userId))
                .toList();
        if (candidates.isEmpty()) return List.of();

        // 2) 用户偏好画像（发过 + 接过 的历史众数）
        UserPreference pref = buildPreference(userId);

        // 3) 打分（信用分按发布者去重缓存，避免重复 RPC）
        Instant now = Instant.now();
        double poolMaxReward = candidates.stream().mapToInt(Task::getRewardPoint).max().orElse(1);
        Map<Long, Integer> creditCache = new HashMap<>();

        List<Scored> scored = new ArrayList<>(candidates.size());
        for (Task t : candidates) {
            int credit = creditCache.computeIfAbsent(t.getPublisherId(),
                    pid -> creditApi.getScoreOf(pid));
            scored.add(new Scored(t, scorer.score(t, pref, credit, poolMaxReward, now)));
        }
        scored.sort(Comparator.comparingDouble(Scored::score).reversed());

        // 4) 截断 + 装配 VO（仅对最终结果取脱敏用户信息）
        return scored.stream()
                .limit(limit)
                .map(s -> TaskListItemVO.from(s.task(), userApi.getPublicUser(s.task().getPublisherId())))
                .toList();
    }

    /** 历史聚合：取发过 + 接过任务里出现最多的 taskType / deliveryBuilding。 */
    private UserPreference buildPreference(long userId) {
        List<Task> history = new ArrayList<>();
        history.addAll(taskRepo.findTop50ByPublisherIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId));
        history.addAll(taskRepo.findTop50ByAssigneeIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId));
        if (history.isEmpty()) return UserPreference.EMPTY;

        TaskType preferredType = mode(history.stream().map(Task::getTaskType).toList());
        String preferredBuilding = mode(history.stream()
                .map(Task::getDeliveryBuilding)
                .filter(b -> b != null && !b.isBlank())
                .toList());
        return new UserPreference(preferredType, preferredBuilding);
    }

    /** 众数：出现次数最多的元素；空集返回 null。 */
    private static <T> T mode(List<T> values) {
        Map<T, Integer> freq = new HashMap<>();
        T best = null;
        int bestCount = 0;
        for (T v : values) {
            int c = freq.merge(v, 1, Integer::sum);
            if (c > bestCount) {
                bestCount = c;
                best = v;
            }
        }
        return best;
    }

    private static boolean equalsLong(Long a, long b) {
        return a != null && a == b;
    }

    private record Scored(Task task, double score) {}
}
