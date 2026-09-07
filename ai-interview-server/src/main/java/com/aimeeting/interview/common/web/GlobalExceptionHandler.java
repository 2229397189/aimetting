package com.aimeeting.interview.common.web;

import com.aimeeting.interview.common.convention.errorcode.BaseErrorCode;
import com.aimeeting.interview.common.convention.exception.AbstractException;
import com.aimeeting.interview.common.convention.exception.ClientException;
import com.aimeeting.interview.common.convention.result.Result;
import com.aimeeting.interview.common.convention.result.Results;
import com.aimeeting.interview.common.util.MdcUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理器：把所有异常统一转成 {@code Result} 返回体。
 *
 * <p>处理顺序（Spring 按最具体匹配）：
 * <ol>
 *   <li>{@link AbstractException}（业务异常：Client / Service / Remote）-&gt; 按错误码映射 HTTP 状态</li>
 *   <li>{@link MethodArgumentNotValidException} -&gt; A0103 + 字段级错误明细</li>
 *   <li>{@link ConstraintViolationException} -&gt; A0103</li>
 *   <li>{@link MaxUploadSizeExceededException} -&gt; A0403</li>
 *   <li>{@link Exception} -&gt; B0001 兜底，带 requestId 便于排查</li>
 * </ol>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常（A/B/C 三级）。
     *
     * @param e        异常
     * @param request  当前请求
     * @return 统一返回体
     */
    @ExceptionHandler(AbstractException.class)
    public ResponseEntity<Result<Void>> handle(AbstractException e, HttpServletRequest request) {
        HttpStatus status = HttpStatusResolver.resolve(e.getErrorCode());
        log.warn("[GlobalException] 业务异常, code={}, msg={}, uri={}, requestId={}",
                e.getErrorCode(), e.getErrorMessage(), uriOf(request), MdcUtil.getRequestId());
        Result<Void> body = Results.failure(e.getErrorCode(), e.getErrorMessage())
                .setRequestId(MdcUtil.getRequestId());
        return ResponseEntity.status(status).body(body);
    }

    /**
     * 处理 {@code @Valid} 请求体校验失败。
     *
     * @param e       异常
     * @param request 当前请求
     * @return 统一返回体，message 拼接字段级错误
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handle(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::formatFieldError)
                .collect(Collectors.joining("; "));
        if (message.isEmpty()) {
            message = BaseErrorCode.PARAM_VALID_FAIL.message();
        }
        log.warn("[GlobalException] 参数校验失败, uri={}, msg={}", uriOf(request), message);
        Result<Void> body = Results.failure(BaseErrorCode.PARAM_VALID_FAIL.code(), message)
                .setRequestId(MdcUtil.getRequestId());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * 处理 {@code @Validated} 方法参数校验失败。
     *
     * @param e       异常
     * @param request 当前请求
     * @return 统一返回体
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handle(ConstraintViolationException e, HttpServletRequest request) {
        String message = e.getConstraintViolations().stream()
                .map(GlobalExceptionHandler::formatViolation)
                .collect(Collectors.joining("; "));
        if (message.isEmpty()) {
            message = BaseErrorCode.PARAM_VALID_FAIL.message();
        }
        log.warn("[GlobalException] 参数校验失败, uri={}, msg={}", uriOf(request), message);
        Result<Void> body = Results.failure(BaseErrorCode.PARAM_VALID_FAIL.code(), message)
                .setRequestId(MdcUtil.getRequestId());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * 处理上传文件超限。
     *
     * @param e       异常
     * @param request 当前请求
     * @return 统一返回体（A0403）
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handle(MaxUploadSizeExceededException e, HttpServletRequest request) {
        log.warn("[GlobalException] 上传文件超限, uri={}, msg={}", uriOf(request), e.getMessage());
        Result<Void> body = Results.failure(BaseErrorCode.FILE_TOO_LARGE)
                .setRequestId(MdcUtil.getRequestId());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * 处理必填参数缺失。
     *
     * @param e       异常
     * @param request 当前请求
     * @return 统一返回体（A0100）
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handle(MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("[GlobalException] 缺少必填参数, uri={}, param={}", uriOf(request), e.getParameterName());
        Result<Void> body = Results.failure(BaseErrorCode.PARAM_ERROR.code(),
                        "缺少必填参数: " + e.getParameterName())
                .setRequestId(MdcUtil.getRequestId());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * 处理参数类型不匹配。
     *
     * @param e       异常
     * @param request 当前请求
     * @return 统一返回体（A0100）
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handle(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        log.warn("[GlobalException] 参数类型不匹配, uri={}, name={}", uriOf(request), e.getName());
        Result<Void> body = Results.failure(BaseErrorCode.PARAM_ERROR.code(),
                        "参数类型不正确: " + e.getName())
                .setRequestId(MdcUtil.getRequestId());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * 兜底异常处理：记录完整堆栈，返回 B0001 + requestId（不暴露内部细节）。
     *
     * @param e       异常
     * @param request 当前请求
     * @return 统一返回体
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handle(Exception e, HttpServletRequest request) {
        log.error("[GlobalException] 未预期异常, uri={}, requestId={}",
                uriOf(request), MdcUtil.getRequestId(), e);
        Result<Void> body = Results.failure(BaseErrorCode.SERVICE_ERROR.code(),
                        BaseErrorCode.SERVICE_ERROR.message() + "(requestId=" + MdcUtil.getRequestId() + ")")
                .setRequestId(MdcUtil.getRequestId());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private static String formatFieldError(FieldError error) {
        return error.getField() + ":" + error.getDefaultMessage();
    }

    private static String formatViolation(ConstraintViolation<?> violation) {
        return violation.getPropertyPath() + ":" + violation.getMessage();
    }

    private static String uriOf(HttpServletRequest request) {
        return request == null ? "" : request.getRequestURI();
    }
}
