package com.yang.hr.common;

import cn.dev33.satoken.exception.DisableServiceException;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理：所有异常统一转成 {code,msg,data}
 *
 * 注意：HTTP 状态码一律 200，业务结果看响应体里的 code（规划书 6.3 的约定）。
 * 因此前端不能靠 HTTP 状态码判断成败，必须判 code。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常：可预期，按原码返回 */
    @ExceptionHandler(BizException.class)
    public R<Void> handleBiz(BizException e) {
        log.warn("[业务异常] code={} msg={}", e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /** 参数校验异常：取第一条字段错误 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValid(MethodArgumentNotValidException e) {
        FieldError first = e.getBindingResult().getFieldError();
        String msg = first == null ? "参数校验不通过" : first.getField() + " " + first.getDefaultMessage();
        log.warn("[参数校验异常] {}", msg);
        return R.fail(ResultCode.PARAM_ERROR, msg);
    }

    /**
     * 请求格式类异常：都是客户端传错了，统一按 400 回，不能掉进 500 兜底。
     * 覆盖：JSON 体无法解析 / 缺必填参数 / 参数类型不匹配 / 请求方法不对 /
     * Content-Type 不对（前端联调忘带 application/json 是高频错误，落 500 会被误判成"后端挂了"）。
     */
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpRequestMethodNotSupportedException.class,
            HttpMediaTypeNotSupportedException.class
    })
    public R<Void> handleBadRequest(Exception e) {
        String msg = describe(e);
        log.warn("[请求异常] {} -> {}", e.getClass().getSimpleName(), msg);
        return R.fail(ResultCode.PARAM_ERROR, msg);
    }

    private String describe(Exception e) {
        if (e instanceof HttpMessageNotReadableException) {
            return "请求体格式错误，无法解析 JSON";
        }
        if (e instanceof HttpMediaTypeNotSupportedException) {
            return "请使用 Content-Type: application/json 发送请求";
        }
        if (e instanceof MissingServletRequestParameterException ex) {
            return "缺少必填参数：" + ex.getParameterName();
        }
        if (e instanceof MethodArgumentTypeMismatchException ex) {
            return "参数类型不正确：" + ex.getName();
        }
        if (e instanceof HttpRequestMethodNotSupportedException ex) {
            return "请求方法不支持：" + ex.getMethod();
        }
        return "请求参数不合法";
    }

    /** 未登录 / token 无效 / 过期 / 被顶下线 → 401（由 Sa-Token 拦截器抛出） */
    @ExceptionHandler(NotLoginException.class)
    public R<Void> handleNotLogin(NotLoginException e) {
        String msg = switch (e.getType()) {
            case NotLoginException.NOT_TOKEN -> "未提供登录凭证，请先登录";
            case NotLoginException.INVALID_TOKEN -> "登录凭证无效，请重新登录";
            case NotLoginException.TOKEN_TIMEOUT -> "登录已过期，请重新登录";
            case NotLoginException.BE_REPLACED -> "账号已在其他设备登录，你已被顶下线";
            case NotLoginException.KICK_OUT -> "账号已被管理员强制下线";
            default -> "登录状态异常，请重新登录";
        };
        log.warn("[未登录] type={} 原始信息={}", e.getType(), e.getMessage());
        return R.fail(ResultCode.UNAUTHORIZED, msg);
    }

    /** 已登录但权限/角色不足 → 403 */
    @ExceptionHandler({NotPermissionException.class, NotRoleException.class})
    public R<Void> handleNoPermission(Exception e) {
        log.warn("[无权限] {}", e.getMessage());
        return R.fail(ResultCode.FORBIDDEN, "没有该操作的权限");
    }

    /** 账号被封禁 → 403 */
    @ExceptionHandler(DisableServiceException.class)
    public R<Void> handleDisabled(DisableServiceException e) {
        log.warn("[账号封禁] {}", e.getMessage());
        return R.fail(ResultCode.FORBIDDEN, "账号已被封禁，剩余 " + e.getDisableTime() + " 秒");
    }

    /**
     * 上传文件超过容器限制 → 400。
     * 这个异常在 multipart 解析阶段就抛出，若不管会掉进下面的 500 兜底，前端只看到"服务异常"，
     * 完全联想不到是文件太大。上限在 config/UploadConfig 里声明。
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public R<Void> handleUploadTooLarge(MaxUploadSizeExceededException e) {
        log.warn("[上传超限] {}", e.getMessage());
        return R.fail(ResultCode.PARAM_ERROR, "上传文件过大，请压缩后再试");
    }

    /**
     * 资源不存在（静态文件缺失 / 路径未映射）→ 404。
     *
     * 不接管的话会掉进 500 兜底，前端只看到"服务异常：NoResourceFoundException" ——
     * 完全联想不到其实只是"图片路径不对/文件不在"。这个坑在 M4 批次3 排查上传时实际踩到过。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public R<Void> handleNoResource(NoResourceFoundException e) {
        log.warn("[资源不存在] {}", e.getMessage());
        return R.fail(ResultCode.NOT_FOUND, "请求的资源不存在");
    }

    /** 兜底：未预期异常只对外暴露概要，细节写日志 */
    @ExceptionHandler(Exception.class)
    public R<Void> handleOther(Exception e) {
        log.error("[系统异常] ", e);
        return R.fail(ResultCode.ERROR, "服务异常：" + e.getClass().getSimpleName());
    }
}
