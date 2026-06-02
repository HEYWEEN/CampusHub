package com.campushub.im.dto;

import jakarta.validation.constraints.NotNull;

/** POST /api/im/conversations — 与某人开聊（get-or-create）。 */
public class ImStartConversationDTO {

    @NotNull
    private Long peerId;

    public Long getPeerId() { return peerId; }
    public void setPeerId(Long peerId) { this.peerId = peerId; }
}
