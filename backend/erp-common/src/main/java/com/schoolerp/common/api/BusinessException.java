package com.schoolerp.common.api;

public class BusinessException extends RuntimeException {
    private final ResultCode resultCode;
    private final int status;

    public BusinessException(ResultCode resultCode, int status, String message) {
        super(message);
        this.resultCode = resultCode;
        this.status = status;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }

    public int getStatus() {
        return status;
    }
}
