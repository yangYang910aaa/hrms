package com.yang.hr.common;

/**
 * 业务异常：Service 层主动抛出，由 GlobalExceptionHandler 统一转成 R
 */
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BizException(String msg) {
        this(ResultCode.ERROR, msg);
    }

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
