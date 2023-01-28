package com.lrz.coroutine.flow;

/**
 * Author:  liurongzhi
 * CreateTime:  2023/1/28
 * Description: 事件流状态变化
 */
public interface OnStateChanged {
    void onPost(Observable<?> observable);
    void onStart(Observable<?> observable);
    void onError(Observable<?> observable);
    void onComplete(Observable<?> observable);
}
