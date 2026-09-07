package com.aimeeting.interview.common.convention.errorcode;

/**
 * 错误码契约接口。
 *
 * <p>错误码分层：
 * <ul>
 *   <li>A —— 客户端错误（参数、鉴权、权限、限流等）</li>
 *   <li>B —— 系统错误（DB、状态机、归属校验等）</li>
 *   <li>C —— 远程错误（AI 服务调用相关）</li>
 * </ul>
 */
public interface IErrorCode {

    /**
     * @return 错误码
     */
    String code();

    /**
     * @return 默认错误文案
     */
    String message();
}
