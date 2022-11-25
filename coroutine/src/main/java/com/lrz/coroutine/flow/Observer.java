package com.lrz.coroutine.flow;

/**
 * Author And Date: liurongzhi on 2021/5/10.
 * Description: com.yilan.sdk.common
 */
public interface Observer<T> {
    void onSubscribe(T t);
}
