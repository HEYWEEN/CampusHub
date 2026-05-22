package com.campushub.common.exception;

import com.campushub.common.response.ResponseCode;

/**
 * 资源不存在（HTTP 404）。
 * 示例：findById(taskId).orElseThrow(() -&gt; new NotFoundException("task not found: " + taskId));
 */
public class NotFoundException extends BizException {

    public NotFoundException(String message) {
        super(ResponseCode.NOT_FOUND.getCode(), message, 404);
    }
}
