package com.nexusadmin.api.result;

/**
 * 状态码契约，定义响应状态码的统一标准。
 * <p>
 * 所有状态码（包括通用枚举和自定义实现）均须实现此接口，
 * 以保证 Result 体系中状态信息的统一契约。
 */
public interface StatusCode {

    /**
     * 获取状态码数值。
     *
     * @return 状态码
     */
    int code();

    /**
     * 获取状态描述消息。
     *
     * @return 消息文本
     */
    String message();
}
