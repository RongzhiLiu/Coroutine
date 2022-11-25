package com.lrz.coroutine.flow;

/**
 * Author:  liurongzhi
 * CreateTime:  2022/11/1
 * Description:
 */
public interface IError<T extends Throwable> {
    void onError(T error);
}
