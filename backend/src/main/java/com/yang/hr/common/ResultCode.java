package com.yang.hr.common;

/**
 * 统一业务响应码（与规划书 6.3 一致）
 */
public final class ResultCode {

    /** 成功 */
    public static final int SUCCESS = 200;
    /** 参数错误 */
    public static final int PARAM_ERROR = 400;
    /** 未登录 / token 失效 */
    public static final int UNAUTHORIZED = 401;
    /** 无权限 */
    public static final int FORBIDDEN = 403;
    /** 资源不存在（如静态文件 / 未映射的路径） */
    public static final int NOT_FOUND = 404;
    /** 服务异常 */
    public static final int ERROR = 500;

    private ResultCode() {
    }
}
