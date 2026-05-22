package com.campushub.credit.strategy;

/**
 * 信用分计分规则全集（P1 SRS FR-CRED-01「计分规则」逐条对应，2026 版）。
 *
 * <pre>
 * | reasonCode           | delta | 触发条件（FR-CRED-01）                          |
 * |----------------------|-------|------------------------------------------------|
 * | TASK_COMPLETE_BONUS  |  +1   | 成功完成一次任务，双方互评后各 +1               |
 * | TASK_NO_SHOW         |  -5   | 接单后无故取消任务                              |
 * | SEVERE_VIOLATION     | -30   | 经管理员核实的严重违规（恶意骚扰、欺诈）        |
 * | TREEHOLE_VIOLATION   | -10   | 树洞帖被判定人身攻击 / 泄露他人隐私             |
 * | MALICIOUS_REVIEW     | -10   | 议价报复经核实属恶意差评（同时删除该评价）      |
 * </pre>
 *
 * <p>注：SRS 实为「1 加分 + 4 扣分」，与看板 CRD-03 概述「5 加分 + 4 扣分」不一致，
 * 以 SRS FR-CRED-01 为准（已在 bug/反思日志记录该口径差异）。
 * 管理员手动调整（FR-ADM-01）走 service 的 manual 通道，不在此规则集内。
 */
public enum ScoreRule implements ScoreStrategy {

    TASK_COMPLETE_BONUS(+1, "成功完成任务并双方互评，+1"),
    TASK_NO_SHOW(-5, "接单后无故取消任务，-5"),
    SEVERE_VIOLATION(-30, "管理员核实的严重违规（骚扰/欺诈），-30"),
    TREEHOLE_VIOLATION(-10, "树洞帖人身攻击/泄露隐私，-10"),
    MALICIOUS_REVIEW(-10, "议价报复经核实属恶意差评，-10 并删除评价");

    private final int delta;
    private final String description;

    ScoreRule(int delta, String description) {
        this.delta = delta;
        this.description = description;
    }

    @Override
    public String reasonCode() {
        return name();
    }

    @Override
    public int delta() {
        return delta;
    }

    @Override
    public String description() {
        return description;
    }
}
