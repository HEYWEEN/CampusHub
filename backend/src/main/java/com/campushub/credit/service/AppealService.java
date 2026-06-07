package com.campushub.credit.service;

import com.campushub.credit.dto.CreditAppealCreateDTO;
import com.campushub.credit.vo.CreditAppealVO;
import com.campushub.credit.vo.ReceivedReviewVO;

import java.util.List;

/** 信用差评申诉（F-CREDIT-05~07）+ 管理员裁决。 */
public interface AppealService {

    CreditAppealVO submitAppeal(long userId, CreditAppealCreateDTO dto);

    List<CreditAppealVO> listMyAppeals(long userId);

    /** 我收到的评价（带可申诉标记），作为申诉入口。 */
    List<ReceivedReviewVO> listReceivedReviews(long userId);

    /** 用户从「我收到的评价」隐藏一条已撤销的差评（软删，仅本人视图）。 */
    void hideReceivedReview(long userId, long reviewId);

    /** 用户从「我的申诉」隐藏一条已处理的申诉记录（软删，仅本人视图）。 */
    void hideAppeal(long userId, long appealId);

    /** admin：待处理申诉队列。 */
    List<CreditAppealVO> listPending();

    /** admin：裁决（通过→撤销该评价；驳回）。 */
    void resolve(long adminId, long appealId, boolean approve, String note);

    /** 某评价是否有进行中申诉（公开渲染打码用，F-CREDIT-07）。 */
    boolean isUnderAppeal(long reviewId);
}
