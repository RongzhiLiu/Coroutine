package com.lrz.coroutine.flow;

/**
 * Author:  liurongzhi
 * CreateTime:  2022/10/14
 * Description:
 */
public interface Function<F,T> {
    T apply(F f);
}
