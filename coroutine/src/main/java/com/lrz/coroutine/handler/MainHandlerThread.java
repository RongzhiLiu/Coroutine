package com.lrz.coroutine.handler;

import android.os.Handler;
import android.os.Looper;

import com.lrz.coroutine.Dispatcher;
import com.lrz.coroutine.LLog;

/**
 * Author And Date: liurongzhi on 2020/3/1.
 * Description: com.yilan.sdk.common.executor
 */
class MainHandlerThread implements IHandlerThread {
    private volatile Handler mHandler;

    public MainHandlerThread() {
        synchronized (this) {
            mHandler = new Handler(Looper.getMainLooper());
        }
    }

    @Override
    public synchronized Handler getThreadHandler() {
        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }
        return mHandler;
    }

    @Override
    public boolean isCore() {
        return true;
    }

    @Override
    public boolean quit() {
        LLog.e("YY_MainHandler", "main looper can not be quit!");
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
            getThreadHandler().postDelayed(job, job.delayTime);
            return true;
        }
        return false;
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
