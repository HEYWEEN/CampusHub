package com.campushub.credit.service;

import com.campushub.credit.api.CreditApi;
import org.springframework.stereotype.Service;

/**
 * Credit 模块核心服务 —— 同时实现 {@link CreditApi} 给跨模块调用。
 *
 * <p><b>当前状态：INF-06 stub。</b> 仅提供接口骨架让 task / trade（B / C）能 @Autowired
 * 并通过编译，方法体尚未实现，真实逻辑在 CRD-01 完成。getScoreOf 暂返回初始信用分 100，
 * 让依赖信用闸门的调用方在 stub 期不会被误拦截。其余写操作显式抛出，避免被误当成功。
 */
@Service
public class CreditServiceImpl implements CreditApi {

    /** 新用户认证通过后的初始信用分（P3 §3.7 / schema credit_account.credit_score 默认 100）。 */
    private static final int INITIAL_CREDIT_SCORE = 100;

    @Override
    public int getScoreOf(long userId) {
        // CRD-01 待实现：查 credit_account.credit_score（账户不存在懒创建）。
        // stub 期返回初始分，避免 task 信用闸门（<60 禁发）误拦截联调。
        return INITIAL_CREDIT_SCORE;
    }

    @Override
    public void freeze(long userId, int points, String bizKey) {
        throw new UnsupportedOperationException("CRD-01 待实现：CreditApi.freeze");
    }

    @Override
    public void unfreeze(long userId, int points, String bizKey) {
        throw new UnsupportedOperationException("CRD-01 待实现：CreditApi.unfreeze");
    }

    @Override
    public void settle(long userId, int points, String bizKey) {
        throw new UnsupportedOperationException("CRD-01 待实现：CreditApi.settle");
    }

    @Override
    public void deduct(long userId, int delta, String reasonCode, String bizKey) {
        throw new UnsupportedOperationException("CRD-03 待实现：CreditApi.deduct");
    }
}
