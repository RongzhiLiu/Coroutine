package com.lrz.coroutine.handler;

import android.os.Handler;
import android.os.Looper;



/**
 * Author:  liurongzhi
 * CreateTime:  2022/12/10
 * Description:
 */
public class CoroutineHandler extends Handler {
    private final String threadName;

    /**
     * @return 当前Handler所负责的线程
     */
    @Override
    public String toString() {
        return threadName;
    }

    public CoroutineHandler(Looper looper, String threadName) {
        super(looper);
        this.threadName = threadName;
    }
}
