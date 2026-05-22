package com.campushub.common.response;

/**
 * 全局通用错误码（1xxx 段），模块自定义码请在各模块 exception 子包扩展。
 * 数值分段约定（P3 统一规约 §2.4）：
 *   1xxx 通用 / 2xxx auth / 3xxx user / 4xxx task / 5xxx trade /
 *   6xxx edu / 7xxx team / 8xxx im / 9xxx notify / 10xxx credit /
 *   11xxx report / 12xxx admin / 13xxx search
 */
public enum ResponseCode {

    SUCCESS(0, "ok"),

    // 1xxx 通用
    BAD_REQUEST(1001, "请求参数错误"),
    UNAUTHORIZED(1002, "未登录或凭据无效"),
    FORBIDDEN(1003, "无权限访问"),
    NOT_FOUND(1004, "资源不存在"),
    CONFLICT(1005, "操作冲突"),
    RULE_VIOLATION(1006, "业务规则不满足"),
    RATE_LIMITED(1007, "请求过于频繁"),
    INTERNAL_ERROR(1500, "服务器内部错误");

    private final int code;
    private final String message;

    ResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
