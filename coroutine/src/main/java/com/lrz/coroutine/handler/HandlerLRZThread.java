
package com.lrz.coroutine.handler;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.os.Process;
import android.os.SystemClock;

import com.lrz.coroutine.Dispatcher;


/**
 * 线程类，用于线程池，也可单独使用，作为某一个组件的调度线程，如数据库操作，sp等
 */
public class HandlerLRZThread extends Thread implements IHandlerThread, MessageQueue.IdleHandler {
    private final String TAG = "COMMON_COROUTINE_HANDLER";
    int mPriority;
    int mTid = -1;
    private Looper mLooper;
    protected volatile Handler mHandler;
    private volatile boolean isRunning = false;
    private volatile boolean isIdle = false;
    private final boolean isCore;
    private volatile boolean isDeath = false;
    private final Dispatcher dispatcher;
    private long keepTime = 1000 * 10;//线程保存的时间;

    public HandlerLRZThread(String name, boolean isCore, Dispatcher dispatcher) {
        this(name, Process.THREAD_PRIORITY_DEFAULT, dispatcher, isCore, 1000 * 10);
    }

    public HandlerLRZThread(String name, int priority, boolean isCore, Dispatcher dispatcher) {
        this(name, priority, dispatcher, isCore, 1000 * 10);
    }

    public HandlerLRZThread(String name, boolean isCore, Dispatcher dispatcher, long keepTime) {
        this(name, Process.THREAD_PRIORITY_DEFAULT, dispatcher, isCore, keepTime);
    }

    public HandlerLRZThread(String name, int priority, Dispatcher dispatcher, boolean isCore, long keepTime) {
        super(name);
        this.dispatcher = dispatcher;
        this.isCore = isCore;
        mPriority = priority;
        this.keepTime = keepTime;
        setDaemon(true);
        start();
    }

    @Override
    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    protected void onLooperPrepared() {
    }

    @Override
    public synchronized boolean isIdle() {
        return isIdle;
    }

    @Override
    public boolean queueIdle() {
        boolean tryGet = false;
        if (onHandlerThreadListener != null) {
            tryGet = onHandlerThreadListener.onIdle(this);
        }
        if (!tryGet) {
            isIdle = true;
        }
        return true;
    }

    @Override
    public boolean isCore() {
        return isCore;
    }

    @Override
    public void run() {
        try {
            mTid = Process.myTid();
            Looper.prepare();
            synchronized (this) {
                mLooper = Looper.myLooper();
                notifyAll();
            }

            Process.setThreadPriority(mPriority);
            onLooperPrepared();
            isRunning = true;
            Looper.myQueue().addIdleHandler(this);
            Looper.loop();
        } finally {
            mLooper = null;
            mHandler = null;
            mTid = -1;
            isRunning = false;
            isIdle = false;
            isDeath = true;
            if (onHandlerThreadListener != null) {
                onHandlerThreadListener.onDeath(this);
            }
            onHandlerThreadListener = null;
        }
    }

    @Override
    public synchronized boolean isRunning() {
        return isRunning;
    }


    private Looper getLooper() {
        if (!isAlive()) {
            return null;
        }
        synchronized (this) {
            while (isAlive() && mLooper == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }
        return mLooper;
    }

    @Override
    public synchronized boolean execute(LJob job) {
        if (mLooper == null) {
            return false;
        }
        if (isIdle()) {
            getThreadHandler().removeMessages(Integer.MIN_VALUE);
        }
        isIdle = job.sysTime > SystemClock.uptimeMillis();
        boolean result = getThreadHandler().postAtTime(job, job.sysTime);
        if (isIdle) {
            queueIdle();
        }
        return result;
    }

    @Override
    public synchronized void removeJob(Runnable runnable) {
        getThreadHandler().removeCallbacks(runnable);
        //移除消息后，再次检查是否需要推出线程
        if (!isCore()) {
            tryQuitOutTime();
        }
    }


    @Override
    public synchronized void onJobComplete() {

    }

    private volatile OnHandlerThreadListener onHandlerThreadListener;

    @Override
    public synchronized void setOnHandlerThreadListener(OnHandlerThreadListener listener) {
        this.onHandlerThreadListener = listener;
    }

    @Override
    public synchronized Handler getThreadHandler() {
        if (isDeath) {
            throw new IllegalStateException("this thread is death~,can not use it again");
        }
        if (mHandler == null) {
            mHandler = new Handler(getLooper());
        }
        return mHandler;
    }

    @Override
    public boolean hasJob() {
        return getThreadHandler().hasMessages(0);
    }

    @Override
    public synchronized boolean quit() {
        if (!isRunning()) return false;
        Looper looper = getLooper();
        if (looper != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                looper.quitSafely();
            }else {
                looper.quit();
            }
            return true;
        }
        return false;
    }

    @Override
    public synchronized void tryQuitOutTime() {
        if (!isRunning()) return;
        // 没有消息需要处理时，尝试退出非核心线程
        if (!hasJob()) {
            Message m = Message.obtain(getThreadHandler(), new Runnable() {
                @Override
                public void run() {
                    //加同步锁，防止丢消息
                    synchronized (HandlerLRZThread.this) {
                        if (!hasJob()) {
                            quit();
                        }
                    }
                }
            });
            //hasJob()只会检测what=0的任务
            m.what = Integer.MIN_VALUE;
            getThreadHandler().sendMessageDelayed(m, keepTime);
        }
    }

    public int getThreadId() {
        return mTid;
    }

    @Override
    public synchronized void start() {
        if (!isRunning()) {
            super.start();
        }
    }
}
