package com.lrz.coroutine.handler;

import android.os.Handler;
import android.os.Looper;

import com.lrz.coroutine.Dispatcher;

/**
 * Author And Date: liurongzhi on 2020/3/1.
 * Description: com.yilan.sdk.common.executor
 */
class MainHandlerThread implements IHandlerThread {
    private volatile Handler mHandler;

    public MainHandlerThread() {

    }

    @Override
    public Handler getThreadHandler() {
        if (mHandler == null) {
            synchronized (this) {
                if (mHandler == null)
                    mHandler = new CoroutineHandler(Looper.getMainLooper(), "MAIN");
            }
        }
        return mHandler;
    }

    @Override
    public boolean isCore() {
        return true;
    }

    @Override
    public boolean quit() {
        return false;
    }

    @Override
    public void tryQuitOutTime() {

    }

    @Override
    public Dispatcher getDispatcher() {
        return Dispatcher.MAIN;
    }

    public Looper getLooper() {
        return Looper.getMainLooper();
    }

    @Override
    public boolean isIdle() {
        return false;
    }

    @Override
    public boolean execute(LJob job) {
        if (getLooper() != null) {
            getThreadHandler().postAtTime(job, job.sysTime);
        }
        return true;
    }

    @Override
    public void removeJob(Runnable runnable) {
        if (getLooper() != null) {
            getThreadHandler().removeCallbacks(runnable);
        }
    }

    @Override
    public void onJobComplete() {
    }


    @Override
    public void setOnHandlerThreadListener(OnHandlerThreadListener listener) {

    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public boolean hasJob() {
        return true;
    }
}
