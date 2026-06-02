package com.campushub.im.dto;

import com.campushub.im.entity.ImMessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** POST /api/im/conversations/{id}/messages — 发送消息（F-IM-02）。 */
public class ImSendMessageDTO {

    /** TEXT / IMAGE（SYSTEM 仅后端内部产生）。默认 TEXT。 */
    private ImMessageType contentType = ImMessageType.TEXT;

    @NotBlank
    @Size(max = 2000, message = "内容最多 2000 字")
    private String content;

    public ImMessageType getContentType() { return contentType == null ? ImMessageType.TEXT : contentType; }
    public void setContentType(ImMessageType contentType) { this.contentType = contentType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
