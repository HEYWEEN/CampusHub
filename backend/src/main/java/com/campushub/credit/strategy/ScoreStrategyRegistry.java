package com.campushub.credit.strategy;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 计分策略注册表（CRD-03 注册表模式）。按 reasonCode 解析对应 {@link ScoreStrategy}。
 *
 * <p>当前规则来自 {@link ScoreRule} 枚举；未来新增规则只需加枚举常量，注册表自动收录。
 */
@Component
public class ScoreStrategyRegistry {

    private final Map<String, ScoreStrategy> rules = buildRules();

    private static Map<String, ScoreStrategy> buildRules() {
        Map<String, ScoreStrategy> map = new HashMap<>();
        for (ScoreRule rule : ScoreRule.values()) {
            map.put(rule.reasonCode(), rule);
        }
        return map;
    }

    /** 是否为已登记的计分规则（区分系统规则 vs 管理员手动调整）。 */
    public boolean contains(String reasonCode) {
        return rules.containsKey(reasonCode);
    }

    /**
     * 解析规则；reasonCode 未登记时抛 IllegalArgumentException（属编码错误，不应发生）。
     */
    public ScoreStrategy resolve(String reasonCode) {
        ScoreStrategy s = rules.get(reasonCode);
        if (s == null) {
            throw new IllegalArgumentException("未登记的计分 reasonCode: " + reasonCode);
        }
        return s;
    }
}
