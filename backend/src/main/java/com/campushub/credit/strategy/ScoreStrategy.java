package com.campushub.credit.strategy;

/**
 * 信用分计分策略（CRD-03 / P1 SRS FR-CRED-01）。
 *
 * <p>每条计分规则是一个策略：给定 reasonCode，产出对信用分的有符号 delta。
 * 把规则从 service 抽出来，是为了让算法<b>可解释、可单测、可扩展</b>——
 * 评审关注信用分算法的公平性，规则集中在 {@link ScoreRule} 一处便于核对与举证。
 */
public interface ScoreStrategy {

    /** 规则标识，与 credit_record / credit_score_log 的 reason_code 一致。 */
    String reasonCode();

    /** 该规则对信用分的有符号变化（加分为正、扣分为负）。 */
    int delta();

    /** 人类可读的规则说明（用于日志/审计/评审举证）。 */
    String description();
}
