package com.lrz.coroutine.handler;

import android.os.Handler;

import com.lrz.coroutine.Dispatcher;

/**
 * Author And Date: liurongzhi on 2020/3/1.
 * Description: com.yilan.sdk.common.executor.handler
 */
public interface IHandlerThread {

    Handler getThreadHandler();

    boolean quit();

    void tryQuitOutTime();

    boolean isIdle();

    boolean isRunning();

    boolean isCore();

    boolean execute(LJob job);

    void removeJob(Runnable runnable);

    void onJobComplete();

    void setOnHandlerThreadListener(OnHandlerThreadListener listener);

    Dispatcher getDispatcher();

    boolean hasJob();

    interface OnHandlerThreadListener {
        boolean onIdle(IHandlerThread handlerThread);

        void onDeath(IHandlerThread handlerThread);
    }
}
