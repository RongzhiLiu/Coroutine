package com.lrz.coroutine.flow;

/**
 * Author:  liurongzhi
 * CreateTime:  2022/11/15
 * Description:
 */
public class CoroutineFlowException extends RuntimeException {

    public CoroutineFlowException(String message) {
        super(message);
    }

    public CoroutineFlowException(String message, Throwable cause) {
        super(message, cause);
    }
}
