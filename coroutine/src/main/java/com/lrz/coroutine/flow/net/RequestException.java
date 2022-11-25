package com.lrz.coroutine.flow.net;

/**
 * Author:  liurongzhi
 * CreateTime:  2022/11/15
 * Description:
 */
public class RequestException extends RuntimeException {
    int code = -1;

    public RequestException(String message, int code) {
        super(message);
        this.code = code;
    }

    public RequestException(String message, Throwable cause, int code) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
