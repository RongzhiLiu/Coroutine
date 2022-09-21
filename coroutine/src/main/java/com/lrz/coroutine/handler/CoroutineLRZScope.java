package com.lrz.coroutine.handler;

import android.os.Process;

import com.lrz.coroutine.Dispatcher;
import com.lrz.coroutine.LLog;
import com.lrz.coroutine.Priority;
import com.lrz.coroutine.PriorityRunnable;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Author And Date: liurongzhi on 2020/2/16.
 * Description: com.yilan.sdk.common.executor
 */
class CoroutineLRZScope implements CoroutineLRZContext, IHandlerThread.OnHandlerThreadListener {
    private final String TAG = "COMMON_COROUTINE";
    static final MainHandlerThread mainHandler = new MainHandlerThread();
    private static final ArrayList<IHandlerThread> threadPool = new ArrayList<>();
    private static final ArrayList<IHandlerThread> backPool = new ArrayList<>();
    private long keepTime = 1000 * 10;
    //正在运行的io线程数量
    private static volatile int runningCount = 0;
    //正在运行的background线程数量
    private static volatile int backCount = 0;
    /**
     * 核心线程数量保持为 cpu 个数，保证并发时的性能
     */
    private final int MAX_COUNT = Math.max(Runtime.getRuntime().availableProcessors(), 2);
    /**
     * 后台线程数的一半，最小是1
     */
    private final int MAX_BACKGROUND = Runtime.getRuntime().availableProcessors() / 4 > 0 ? Runtime.getRuntime().availableProcessors() / 4 : 1;
    /**
     * 工作队列已满，放到此容器中暂存
     */
    private static final PriorityBlockingQueue<LJob> jobQueue = new PriorityBlockingQueue<>(6, (o1, o2) -> {
        int x1 = Priority.MEDIUM.ordinal(), x2 = Priority.MEDIUM.ordinal();
        if (o1.runnable instanceof PriorityRunnable) {
            x1 = ((PriorityRunnable) o1.runnable).getPriority().ordinal();
        }
        if (o2.runnable instanceof PriorityRunnable) {
            x2 = ((PriorityRunnable) o2.runnable).getPriority().ordinal();
        }
        return x2 - x1;
    });
    /**
     * 后台任务，用来执行不紧急的任务,线程安全的链表
     */
    private static final LinkedBlockingDeque<LJob> backgroundJobs = new LinkedBlockingDeque<>();

    private HandlerLRZThread createThread(Dispatcher dispatcher) {
        HandlerLRZThread thread;
        if (dispatcher == Dispatcher.BACKGROUND) {
            thread = new HandlerLRZThread("YLCoroutineScope_" + dispatcher.name() + "_" + backCount, Process.THREAD_PRIORITY_BACKGROUND, dispatcher, true, keepTime);
        } else {
            thread = new HandlerLRZThread("YLCoroutineScope_" + dispatcher.name() + "_" + runningCount, Process.THREAD_PRIORITY_DEFAULT, dispatcher, runningCount < MAX_COUNT, keepTime);

        }
        thread.setOnHandlerThreadListener(this);
        return thread;
    }

    @Override
    public Job execute(Dispatcher dispatcher, Runnable runnable) {
        LJob job = LJob.obtain(runnable)
                .dispatch(dispatcher);
        doExecute(job);
        return new Job(job);
    }

    @Override
    public Job executeTime(Dispatcher dispatcher, Runnable runnable, final long spaceTime) {
        LJob job = LJob.obtain(runnable)
                .dispatch(dispatcher)
                .loop(true)
                .delayTime(spaceTime);
        doExecute(job);
        return new Job(job);
    }

    @Override
    public Job executeDelay(Dispatcher dispatcher, Runnable runnable, long delayTime) {
        LJob job = LJob.obtain(runnable)
                .dispatch(dispatcher)
                .loop(false)
                .delayTime(delayTime);
        doExecute(job);
        return new Job(job);
    }

    @Override
    public Job executeJobs(Dispatcher dispatcher, Runnable... runnables) {
        if (runnables == null || runnables.length < 1) return new Job(null);
        LJob headJob = LJob.obtain(runnables[0])
                .dispatch(dispatcher)
                .loop(false);
        Job head = new Job(headJob);
        doExecute(headJob);
        for (int i = 1; i < runnables.length; i++) {
            LJob job = LJob.obtain(runnables[i])
                    .dispatch(dispatcher)
                    .loop(false);
            head.next = new Job(job);
            doExecute(job);
        }
        return head;
    }


    private void doExecute(LJob job) {
        if (job.runnable == null) return;
        /*
         * 处理调用栈，增加自定义调用栈，方便问题定位
         */
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        int i = 0;
        StackTraceElement[] stackTrace = new StackTraceElement[3];
        for (StackTraceElement element : stackTraceElements) {
            if (i > 0 || element.getClassName().equals(CoroutineLRZScope.class.getName())) {
                i += 1;
            }
            if (i < 4 && i > 0) {
                stackTrace[i - 1] = element;
            }
        }
        job.setStackTraceExtra(stackTrace);

        if (job.getDispatcher() == Dispatcher.IO) {
            //先将任务丢到队列中排序
            addToJobQueue(job);
            job = jobQueue.poll();
        }
        if (job == null) return;

        /*
            1 先获取空闲的线程，如果有，就执行
            2 如果没有空闲线程，先看任务队列压力是否过载（io队列长度大于核心线程数量的2倍），如果过载，则创建非核心线程执行任务
            3 如果任务队列没有过载，则加入队列等待执行
         */

        IHandlerThread handlerThread = getThreadHandler(job.getDispatcher());
        if (handlerThread != null && !handlerThread.execute(job.handlerThread(handlerThread))) {
            LLog.d(TAG, job.getDispatcher().name() + "there has no thread to execute your job ,please hold on");
            addToJobQueue(job.handlerThread(null));
        } else {
            //如果该任务是BACKGROUND，且此时BACKGROUND已经满载，且backgroundJobs任务堆积(线程数量的2倍)，则IO中的空闲非核心线程将尝试窃取任务执行
            //如果是io满载，且任务队列达到 MAX_COUNT,则BACKGROUND中空闲的非核心线程尝试窃取任务执行
            IHandlerThread thief;
            Dispatcher dispatcher = job.getDispatcher() == Dispatcher.IO ? Dispatcher.BACKGROUND : Dispatcher.IO;
            //判断线程池是否繁忙，1.当前核心线程数量是否已经达到最大 2.当前任务队列是否已经超载
            boolean isBusy;
            if (job.getDispatcher() == Dispatcher.IO) {
                // 如果正在运行的io核心线程已达到最大，或者任务积压达到核心线程的数量
                isBusy = runningCount >= MAX_COUNT && jobQueue.size() >= MAX_COUNT;
            } else {
                isBusy = backCount >= MAX_BACKGROUND && backgroundJobs.size() >= MAX_BACKGROUND * 2;
            }
            if (isBusy && (thief = getThreadHandler(dispatcher, false)) != null) {
                if (!thief.execute(job.handlerThread(thief))) {
                    LLog.w(TAG, job.getDispatcher().name() + "：thread steal job failed，name =  " + ((Thread) thief).getName());
                    addToJobQueue(job.handlerThread(null));
                } else {
                    LLog.d(TAG, ((Thread) thief).getName() + " steal a job and do it");
                }
            } else {
                LLog.w(TAG, job.getDispatcher().name() + "：thread pool is fully,add to queue and waiting");
                addToJobQueue(job);
            }
        }

    }


    private void addToJobQueue(LJob job) {
        if (job.getDispatcher() == Dispatcher.BACKGROUND) {
            backgroundJobs.offerLast(job);
        } else {
            jobQueue.offer(job);
        }
    }

    void removeToFromQueue(LJob job) {
        if (job.getDispatcher() == Dispatcher.BACKGROUND) {
            backgroundJobs.remove(job);
        } else {
            jobQueue.remove(job);
        }
    }

    private IHandlerThread getThreadHandler(Dispatcher dispatcher) {
        return getThreadHandler(dispatcher, true);
    }

    private IHandlerThread getThreadHandler(Dispatcher dispatcher, boolean needCreate) {
        if (dispatcher == Dispatcher.MAIN) {
            return mainHandler;
        } else if (dispatcher == Dispatcher.IO) {
            IHandlerThread handler = null;
            synchronized (threadPool) {
                for (int i = 0; i < threadPool.size(); i++) {
                    IHandlerThread handlerThread = threadPool.get(i);
                    if (handlerThread.isIdle() && handlerThread.isRunning()) {
                        handler = handlerThread;
                        LLog.d(TAG, "thread reuse，num=" + runningCount + ",name=" + ((Thread) handlerThread).getName() + ",isCore=" + handlerThread.isCore());
                        break;
                    }
                }
                /*
                    如果是io线程，在创建新的非核心线程也需要谨慎处理
                    我们在测试中发现，线程数量多，未必能提升并发量，且，在创建新的线程去处理新任务时，大多数情况下需要的时间比等待已存在的线程执行还要长
                    甚至存在新的线程创建后，发现，任务已经被别的线程执行完了，新的线程没有做任何事情，白白等在死亡
                    如果核心线程被使用完，但是并没有达到过载的状态，其实可以不用创建，而是等待被执行即可
                    如何界定io线程达到过载呢，这里暂时定为：任务队列积压的任务数量达到核心线程的2-3倍

                    此处创建新线程的条件：
                        1 核心线程数量没有达到最大，则创建
                        2 核心线程数达到最大，非核心线程数量没有达到最大，且任务队列积压，
                 */
                if (handler == null && needCreate
                        //核心线程数量没有达到最大           核心线程数达到最大，非核心线程数量没有达到最大，且任务队列积压
                        && (runningCount < MAX_COUNT || (runningCount < MAX_COUNT + 2 && jobQueue.size() > MAX_COUNT * 2))) {
                    IHandlerThread handlerThread = createThread(dispatcher);
                    LLog.d(TAG, "thread create,num=" + runningCount + ",name=" + ((Thread) handlerThread).getName() + ",isCore=" + handlerThread.isCore());
                    threadPool.add(handlerThread);
                    runningCount += 1;
                }
            }
            return handler;
        } else {
            IHandlerThread handler = null;
            synchronized (backPool) {
                for (int i = 0; i < backPool.size(); i++) {
                    IHandlerThread h = backPool.get(i);
                    if (h.isIdle() && h.isRunning()) {
                        handler = h;
                        LLog.d(TAG, "thread finding,background->reuse...");
                        break;
                    }
                }
                /*
                    BACKGROUND线程由于线程数量有限，且不存在非核心线程，则不需要考虑线程池扩容问题
                    BACKGROUND中的任务一定是不紧急的，如上报，日志记录等
                    且由于窃取机制的存在，正常情况下，BACKGROUND的任务执行会比我们预想的更快
                 */
                if (handler == null && backCount < MAX_BACKGROUND && needCreate) {
                    IHandlerThread handlerThread = createThread(dispatcher);
                    LLog.d(TAG, "thread finding,background->create...");
                    backPool.add(handlerThread);
                    backCount += 1;
                }
            }
            return handler;
        }
    }


    @Override
    public void clear() {
        jobQueue.clear();
        backgroundJobs.clear();
        synchronized (threadPool) {
            for (IHandlerThread handlerThread : threadPool) {
                handlerThread.getThreadHandler().removeCallbacksAndMessages(null);
            }
        }

        synchronized (backPool) {
            for (IHandlerThread handlerThread : backPool) {
                handlerThread.getThreadHandler().removeCallbacksAndMessages(null);
            }
        }
    }

    @Override
    public boolean onIdle(IHandlerThread handlerThread) {
        //尝试获取任务
        LLog.d(TAG, "thread onIdle" + ",name=" + ((Thread) handlerThread).getName() + ",isCore=" + handlerThread.isCore());
        if (handlerThread.getDispatcher() == Dispatcher.BACKGROUND) {
            LJob job;
            if ((job = backgroundJobs.pollFirst()) != null) {
                handlerThread.execute(job.handlerThread(handlerThread));
                return true;
            } else {
                // 尝试窃取
                boolean isBusy = runningCount >= MAX_COUNT && jobQueue.size() >= MAX_COUNT;
                if (isBusy && (job = jobQueue.poll()) != null) {
                    LLog.d(TAG, ((Thread) handlerThread).getName() + " steal a job and do it when it is onIdle");
                    handlerThread.execute(job.handlerThread(handlerThread));
                    return true;
                }
                return false;
            }
        } else {
            LJob job = jobQueue.poll();
            if (job != null) {
                handlerThread.execute(job.handlerThread(handlerThread));
                return true;
            } else {
                // 尝试窃取
                boolean isBusy = backCount >= MAX_BACKGROUND && backgroundJobs.size() >= MAX_BACKGROUND * 2;
                if (isBusy && (job = backgroundJobs.pollFirst()) != null) {
                    LLog.d(TAG, ((Thread) handlerThread).getName() + " steal a job and do it when it is onIdle");
                    handlerThread.execute(job.handlerThread(handlerThread));
                    return true;
                }
                if (!handlerThread.isCore() && !handlerThread.hasJob()) {
                    handlerThread.tryQuitOutTime();
                }
                return false;
            }
        }
    }

    @Override
    public void onDeath(IHandlerThread handlerThread) {
        if (handlerThread.getDispatcher() == Dispatcher.BACKGROUND) {
            synchronized (backPool) {
                backPool.remove(handlerThread);
                backCount -= 1;
                LLog.d(TAG, "thread dead,now num=" + backCount + ",name=" + ((Thread) handlerThread).getName());
            }
        } else {
            synchronized (threadPool) {
                threadPool.remove(handlerThread);
                runningCount -= 1;
                LLog.d(TAG, "thread dead,now num=" + runningCount + ",name=" + ((Thread) handlerThread).getName());
            }
        }
    }

    @Override
    public void setKeepTime(long keepTime) {
        this.keepTime = keepTime;
    }
}
