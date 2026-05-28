package com.campushub.notify.controller;

import com.campushub.integration.BaseIT;
import com.campushub.notify.api.NotifyApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * NTF-01 controller 测试：4 个端点 + 鉴权边界。继承 BaseIT 复用清表 + mintToken。
 */
class NotifyControllerTest extends BaseIT {

    @Autowired private NotifyApi notifyApi;

    @Test
    void listMessages_returnsMyMessages() throws Exception {
        long uid = 9001L;
        notifyApi.appendLetter(uid, "TASK_ACCEPTED", "你的任务已被接单", "任务 #1 已被接单", "TASK_ACCEPTED:1:" + uid);
        notifyApi.appendLetter(uid, "TASK_COMPLETED", "任务已完成", "任务 #1 已完成", "TASK_COMPLETED:1:" + uid);

        mockMvc.perform(get("/api/notify/messages").header("Authorization", bearer(mintToken(uid))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].type").exists())
                .andExpect(jsonPath("$.data[0].readAt").doesNotExist());
    }

    @Test
    void listMessages_filterUnread_returnsOnlyUnread() throws Exception {
        long uid = 9002L;
        notifyApi.appendLetter(uid, "TASK_ACCEPTED", "t1", "b1", "BK:1:" + uid);
        notifyApi.appendLetter(uid, "TASK_COMPLETED", "t2", "b2", "BK:2:" + uid);

        mockMvc.perform(post("/api/notify/messages/read-all").header("Authorization", bearer(mintToken(uid))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notify/messages?filter=unread").header("Authorization", bearer(mintToken(uid))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(get("/api/notify/messages?filter=read").header("Authorization", bearer(mintToken(uid))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void unreadCount_returnsCount() throws Exception {
        long uid = 9003L;
        notifyApi.appendLetter(uid, "TASK_ACCEPTED", "t", "b", "BK:1:" + uid);
        notifyApi.appendLetter(uid, "TASK_COMPLETED", "t", "b", "BK:2:" + uid);

        mockMvc.perform(get("/api/notify/messages/unread-count").header("Authorization", bearer(mintToken(uid))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(2));
    }

    @Test
    void markRead_singleMessage_updatesReadAt() throws Exception {
        long uid = 9004L;
        notifyApi.appendLetter(uid, "TASK_ACCEPTED", "t", "b", "BK:1:" + uid);

        String body = mockMvc.perform(get("/api/notify/messages").header("Authorization", bearer(mintToken(uid))))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(body).path("data").get(0).path("id").asLong();

        mockMvc.perform(patch("/api/notify/messages/" + id + "/read").header("Authorization", bearer(mintToken(uid))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/notify/messages/unread-count").header("Authorization", bearer(mintToken(uid))))
                .andExpect(jsonPath("$.data.count").value(0));
    }

    @Test
    void markRead_otherUsersMessage_returns403() throws Exception {
        long owner = 9005L;
        long attacker = 9006L;
        notifyApi.appendLetter(owner, "TASK_ACCEPTED", "t", "b", "BK:X:" + owner);

        String body = mockMvc.perform(get("/api/notify/messages").header("Authorization", bearer(mintToken(owner))))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(body).path("data").get(0).path("id").asLong();

        mockMvc.perform(patch("/api/notify/messages/" + id + "/read").header("Authorization", bearer(mintToken(attacker))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(9002));
    }

    @Test
    void listMessages_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/notify/messages"))
                .andExpect(status().isUnauthorized());
    }
}
