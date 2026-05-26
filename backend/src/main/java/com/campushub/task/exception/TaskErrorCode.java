package com.campushub.task.exception;

public final class TaskErrorCode {
    private TaskErrorCode() {}

    public static final int CREDIT_TOO_LOW   = 4001;
    public static final int NOT_VERIFIED     = 4002;
    public static final int SELF_ACCEPT      = 4003;
    public static final int VERSION_CONFLICT = 4004;
    public static final int ACCEPT_LIMIT     = 4005;
    public static final int STATE_VIOLATION  = 4006;
    public static final int NOT_PUBLISHER    = 4007;
    public static final int NOT_INVOLVED     = 4008;
    public static final int PROOF_IMAGE_TOO_MANY = 4009;
    public static final int PROOF_TEXT_TOO_LONG   = 4010;
    public static final int NOT_ASSIGNEE     = 4011;
    public static final int EXTEND_LIMIT     = 4012;
    public static final int EXTEND_RANGE     = 4013;
    public static final int ACCEPT_LIMIT_RANGE = 4015;
}
