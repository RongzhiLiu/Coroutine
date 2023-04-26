package com.lrz.coroutine.flow.net;

/**
 * Author:  liurongzhi
 * CreateTime:  2023/4/26
 * Description:
 */
public interface CodeInterceptor {
    void onInterceptor(String json, String msg);
}
