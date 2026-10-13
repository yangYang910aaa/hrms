package com.yang.hr.common;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

/**
 * 统一响应体：{ "code": 200, "msg": "操作成功", "data": {} }
 *
 * @param <T> 业务数据类型
 */
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;
    private String msg;
    private T data;

    public R() {
    }

    public R(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> R<T> ok() {
        return new R<>(ResultCode.SUCCESS, "操作成功", null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(ResultCode.SUCCESS, "操作成功", data);
    }

    public static <T> R<T> ok(String msg, T data) {
        return new R<>(ResultCode.SUCCESS, msg, data);
    }

    public static <T> R<T> fail(int code, String msg) {
        return new R<>(code, msg, null);
    }

    public static <T> R<T> fail(String msg) {
        return new R<>(ResultCode.ERROR, msg, null);
    }

    /**
     * 便捷判断，仅供 Java 代码内部使用。
     * 加 @JsonIgnore 是为了让响应体严格保持 {code,msg,data} 三个字段，
     * 不因这个 getter 被 Jackson 当成属性多序列化出一个 success。
     */
    @JsonIgnore
    public boolean isSuccess() {
        return code != null && code == ResultCode.SUCCESS;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
