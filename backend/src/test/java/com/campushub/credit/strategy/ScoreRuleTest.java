package com.campushub.credit.strategy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CRD-03：计分规则分值固化（对照 P1 SRS FR-CRED-01），防止规则被误改。
 * 纯逻辑，无需 Spring 上下文。
 */
class ScoreRuleTest {

    private final ScoreStrategyRegistry registry = new ScoreStrategyRegistry();

    @Test
    void ruleDeltas_matchSRS() {
        assertEquals(+1, ScoreRule.TASK_COMPLETE_BONUS.delta());
        assertEquals(-5, ScoreRule.TASK_NO_SHOW.delta());
        assertEquals(-30, ScoreRule.SEVERE_VIOLATION.delta());
        assertEquals(-10, ScoreRule.TREEHOLE_VIOLATION.delta());
        assertEquals(-10, ScoreRule.MALICIOUS_REVIEW.delta());
    }

    @Test
    void registry_resolvesByReasonCode() {
        assertTrue(registry.contains("TASK_NO_SHOW"));
        assertEquals(-5, registry.resolve("TASK_NO_SHOW").delta());
    }

    @Test
    void registry_unknownReason_notContained_andResolveThrows() {
        assertFalse(registry.contains("MANUAL_ADJUST"));
        assertThrows(IllegalArgumentException.class, () -> registry.resolve("MANUAL_ADJUST"));
    }
}
