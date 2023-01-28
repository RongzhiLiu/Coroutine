package com.lrz.coroutine.handler;

import android.os.SystemClock;

import com.lrz.coroutine.Dispatcher;
import com.lrz.coroutine.LLog;

import java.util.Arrays;
import java.util.concurrent.LinkedBlockingDeque;


/**
 * Author And Date: liurongzhi on 2020/2/25.
 * Description: com.yilan.sdk.common.executor
 */
class LJob implements Runnable {
    private final String TAG = "COROUTINE_JOB";
    private volatile Dispatcher dispatcher;
    private volatile IHandlerThread iHandlerThread;
    volatile Runnable runnable;
    private volatile boolean isRunning = false;
    private volatile boolean cancel = false;
    volatile long sysTime = 0;
    volatile long delay = 0;
    private volatile boolean isLoop = false;
    /**
     * 额外的方法栈信息，记录了异步在执行之前的发起位置
     */
    private volatile StackTraceElement[] stackTraceExtra;

    private LJob(Runnable runnable) {
        dispatcher = Dispatcher.MAIN;
        this.iHandlerThread = CoroutineLRZScope.mainHandler;
        this.runnable = runnable;
    }

    public synchronized LJob handlerThread(IHandlerThread iHandlerThread) {
        this.iHandlerThread = iHandlerThread;
        return this;
    }

    public synchronized LJob dispatch(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
        return this;
    }

    public synchronized LJob delayTime(long spaceTime) {
        this.sysTime = SystemClock.uptimeMillis() + spaceTime;
        delay = spaceTime;
        return this;
    }

    public synchronized LJob loop(boolean isLoop) {
        this.isLoop = isLoop;
        return this;
    }

    public synchronized void setStackTraceExtra(StackTraceElement[] stackTraceExtra) {
        this.stackTraceExtra = stackTraceExtra;
    }

    private synchronized void recycle() {
        isLoop = false;
        sysTime = 0;
        delay = 0;
        isRunning = false;
        dispatcher = null;
        runnable = null;
        cancel = false;
        iHandlerThread = null;
        stackTraceExtra = null;
    }

    private synchronized void setRunnable(Runnable runnable) {
        this.runnable = runnable;
    }


    public void cancel() {
        synchronized (this) {
            if (dispatcher == null || iHandlerThread == null || cancel) {
                return;
            }
            ((CoroutineLRZScope) CoroutineLRZContext.INSTANCE).removeToFromQueue(this);
            cancel = true;
            isRunning = false;
            if (iHandlerThread != null) {
                iHandlerThread.removeJob(this);
            }

            jobs.remove(this);
            jobs.offerLast(this);
            runnable = null;
        }
    }

    public int getRunnableHash() {
        synchronized (this) {
            if (runnable == null) {
                return 0x000000;
            } else {
                return runnable.hashCode();
            }
        }

    }


    @Override
    public final void run() {
        if (isRunning) {
            LLog.e(TAG, "job can not be execute again!");
            return;
        }
        Runnable work = runnable;
        if (!isCancel() && work != null) {
            isRunning = true;
            try {
                work.run();
            } catch (Throwable throwable) {
                if (stackTraceExtra != null) {
                    StackTraceElement[] stackTraceElements = throwable.getStackTrace();
                    int oldLength = stackTraceElements.length;
                    stackTraceElements = Arrays.copyOf(stackTraceElements, stackTraceElements.length + stackTraceExtra.length);
                    System.arraycopy(stackTraceExtra, 0, stackTraceElements, oldLength, stackTraceExtra.length);
                    throwable.setStackTrace(stackTraceElements);
                }
                throw throwable;
            }

            //判断如果是周期任务，则继续执行
            if (isLoop && dispatcher != null) {
                isRunning = false;
                cancel = false;
                synchronized (this) {
                    iHandlerThread = null;
                }
                ((CoroutineLRZScope) CoroutineLRZScope.INSTANCE).executeTimeInner(dispatcher, this, delay);
            } else {
                isRunning = false;
                synchronized (this) {
                    if (iHandlerThread != null) {
                        iHandlerThread.onJobComplete();
                    }
                    iHandlerThread = null;
                    runnable = null;
                    jobs.remove(this);
                    jobs.offerLast(this);
                }
            }
        }
    }


    public synchronized boolean isRunning() {
        return isRunning;
    }


    synchronized Dispatcher getDispatcher() {
        return dispatcher;
    }

    public synchronized boolean isCancel() {
        return cancel;
    }


    //对象复用池
    private static volatile LinkedBlockingDeque<LJob> jobs = new LinkedBlockingDeque<>();

    public static LJob obtain(Runnable runnable) {
        LJob job = jobs.pollFirst();
        if (job == null) {
            job = new LJob(runnable);
        } else {
            job.recycle();
            job.setRunnable(runnable);
        }
        return job;
    }

    @Override
    public String toString() {
        return "{" +
                "\"hash\":" + this.hashCode() +
                ", \"dispatcher\":" + "\"" + dispatcher + "\"" +
                ", \"thread\":" + iHandlerThread +
                ", \"sysTime\":" + sysTime +
                ", \"delay\":" + delay +
                ", \"isLoop\":" + isLoop +
                '}';
    }
}
