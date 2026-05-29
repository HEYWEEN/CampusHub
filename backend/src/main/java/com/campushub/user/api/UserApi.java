package com.campushub.user.api;

import com.campushub.common.PublicUserVO;
import com.campushub.common.enums.VerifyStatus;

import java.util.Optional;

/**
 * 跨模块用户查询契约（P4 §02 跨模块接口表 + P3 §1）。
 *
 * **任何业务模块（task / trade / edu / credit / notify ...）想拿到用户信息**
 * 都必须 @Autowired 此 interface，禁止直接 @Autowired UserServiceImpl。
 *
 * 用户不存在时方法实现抛 NotFoundException（不要返回 null / Optional —— 让 GlobalExceptionHandler 兜底 404）。
 */
public interface UserApi {

    /**
     * 拿公开用户对象（脱敏 + 隐私字段过滤后的安全形态）。
     * 用于任何"展示别人的用户信息"的场景。
     */
    PublicUserVO getPublicUser(long userId);

    /**
     * 取实名认证状态。
     * 用于"该操作需要 APPROVED 才能做"的闸门（例如 task TASK-02 发布、trade 二手发布）。
     */
    VerifyStatus getVerifyStatus(long userId);

    /**
     * 用户是否存在（task accept 自接禁止之类的轻量 check 可用）。
     */
    boolean exists(long userId);

    /**
     * 拿公开用户对象的 Optional 版本：用户不存在时返回 Optional.empty()，
     * 不抛异常，不污染外层事务。
     * 用于"卖家/发布者可能被注销，但商品仍需要展示"等场景。
     */
    Optional<PublicUserVO> findPublicUser(long userId);
}
