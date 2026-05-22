package com.campushub.credit.controller;

import com.campushub.common.util.JwtUtil;
import com.campushub.credit.api.CreditApi;
import com.campushub.credit.repository.CreditAccountRepository;
import com.campushub.credit.repository.CreditRecordRepository;
import com.campushub.credit.repository.CreditScoreLogRepository;
import com.campushub.credit.repository.TaskReviewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CRD-02：双向评分接口测试。@SpringBootTest + MockMvc + mock JWT，与 auth 测试一致。
 */
@SpringBootTest
@ActiveProfiles("test")
class CreditReviewControllerTest {

    @Autowired private WebApplicationContext ctx;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private TaskReviewRepository reviewRepo;
    @Autowired private CreditAccountRepository accountRepo;
    @Autowired private CreditRecordRepository recordRepo;
    @Autowired private CreditScoreLogRepository scoreLogRepo;
    @Autowired private CreditApi creditApi;

    private final ObjectMapper json = new ObjectMapper();

    private static final long PUBLISHER = 2001L;
    private static final long ACCEPTOR = 2002L;
    private static final long TASK = 7001L;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(ctx).build();
    }

    @BeforeEach
    void cleanup() {
        reviewRepo.deleteAll();
        scoreLogRepo.deleteAll();
        recordRepo.deleteAll();
        accountRepo.deleteAll();
    }

    private String token(long userId) {
        return jwtUtil.generateAccessToken(userId, "APPROVED");
    }

    private String body(long taskId, long revieweeId, int rating, String comment) throws Exception {
        Map<String, Object> m = new HashMap<>();
        m.put("taskId", taskId);
        m.put("revieweeId", revieweeId);
        m.put("rating", rating);
        if (comment != null) m.put("comment", comment);
        return json.writeValueAsString(m);
    }

    @Test
    void submit_firstReview_ok_bothReviewedFalse() throws Exception {
        mockMvc().perform(post("/api/credit/reviews")
                        .header("Authorization", "Bearer " + token(PUBLISHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(TASK, ACCEPTOR, 5, "靠谱")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.reviewId").isNumber())
                .andExpect(jsonPath("$.data.bothReviewed").value(false));
    }

    @Test
    void submit_selfReview_returns400() throws Exception {
        mockMvc().perform(post("/api/credit/reviews")
                        .header("Authorization", "Bearer " + token(PUBLISHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(TASK, PUBLISHER, 5, null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submit_duplicate_returns409() throws Exception {
        String b = body(TASK, ACCEPTOR, 4, null);
        mockMvc().perform(post("/api/credit/reviews")
                        .header("Authorization", "Bearer " + token(PUBLISHER))
                        .contentType(MediaType.APPLICATION_JSON).content(b))
                .andExpect(status().isOk());
        mockMvc().perform(post("/api/credit/reviews")
                        .header("Authorization", "Bearer " + token(PUBLISHER))
                        .contentType(MediaType.APPLICATION_JSON).content(b))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(10004));
    }

    @Test
    void submit_ratingOutOfRange_returns400() throws Exception {
        mockMvc().perform(post("/api/credit/reviews")
                        .header("Authorization", "Bearer " + token(PUBLISHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(TASK, ACCEPTOR, 6, null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submit_noToken_returns401() throws Exception {
        mockMvc().perform(post("/api/credit/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(TASK, ACCEPTOR, 5, null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bothSidesReview_awardsPlusOneToEach() throws Exception {
        // 发布者评接单者
        mockMvc().perform(post("/api/credit/reviews")
                        .header("Authorization", "Bearer " + token(PUBLISHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(TASK, ACCEPTOR, 5, null)))
                .andExpect(jsonPath("$.data.bothReviewed").value(false));
        // 接单者回评发布者 → 触发双方各 +1
        mockMvc().perform(post("/api/credit/reviews")
                        .header("Authorization", "Bearer " + token(ACCEPTOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(TASK, PUBLISHER, 5, null)))
                .andExpect(jsonPath("$.data.bothReviewed").value(true));

        assertEquals(101, creditApi.getScoreOf(PUBLISHER));
        assertEquals(101, creditApi.getScoreOf(ACCEPTOR));
    }
}
