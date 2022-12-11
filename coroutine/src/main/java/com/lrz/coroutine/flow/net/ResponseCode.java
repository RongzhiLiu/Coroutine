package com.lrz.coroutine.flow.net;

/**
 * Author:  liurongzhi
 * CreateTime:  2022/11/15
 * Description:
 */
public final class ResponseCode {
    // 无网络
    public static final int CODE_ERROR_NO_NET = -1;
    // 响应内容是null
    public static final int CODE_ERROR_RESPONSE_NULL = 0;
    // io异常
    public static final int CODE_ERROR_IO = 1;
    // json 序列化异常
    public static final int CODE_ERROR_JSON_FORMAT = 2;
    // url 错误
    public static final int CODE_ERROR_URL_ILLEGAL = 3;
    // 本地代码逻辑错误，
    public static final int CODE_ERROR_LOCAL = 4;
    // 请求被取消
    public static final int CODE_ERROR_REQUEST_CANCEL = 5;

    public static final int CODE_ERROR_THREAD = 6;
}
