package com.lrz.coroutine.handler;

import com.lrz.coroutine.Dispatcher;
import com.lrz.coroutine.flow.Observable;
import com.lrz.coroutine.flow.Task;

import java.util.concurrent.Executor;

/**
 * Author And Date: liurongzhi on 2020/2/26.
 * Description: com.yilan.sdk.common.executor
 */
public interface CoroutineLRZContext extends Executor {
    /**
     * 全局唯一实例
     */
    CoroutineLRZContext INSTANCE = new CoroutineLRZScope();

    static <T> Observable<T> Create(Task<T> task) {
        return INSTANCE.create(task);
    }

    static Job Execute(Dispatcher dispatcher, Runnable runnable) {
        return INSTANCE.execute(dispatcher, runnable);
    }

    static Job ExecuteTime(Dispatcher dispatcher, Runnable runnable, long spaceTime) {
        return INSTANCE.executeTime(dispatcher, runnable, spaceTime);
    }

    static Job ExecuteDelay(Dispatcher dispatcher, Runnable runnable, long delayTime) {
        return INSTANCE.executeDelay(dispatcher, runnable, delayTime);
    }

    static Job ExecuteJobs(Dispatcher dispatcher, Runnable... runnable) {
        return INSTANCE.executeJobs(dispatcher, runnable);
    }

    static void Clear() {
        INSTANCE.clear();
    }

    static void SetKeepTime(long time) {
        INSTANCE.setKeepTime(time);
    }

    static void SetElasticCount(int count) {
        INSTANCE.setElasticCount(count);
    }

    static void SetStackTraceExtraEnable(boolean enable) {
        INSTANCE.setStackTraceExtraEnable(enable);
    }

    static boolean GetStackTraceExtraEnable() {
        return INSTANCE.getStackTraceExtraEnable();
    }

    /**
     * 启动异步任务
     *
     * @param dispatcher 任务调度器类型
     * @param runnable   任务
     * @return job
     */
    Job execute(Dispatcher dispatcher, Runnable runnable);

    /**
     * 启动周期任务
     *
     * @param dispatcher 任务调度器类型
     * @param runnable   任务
     * @param spaceTime  周期时间
     * @return job
     */
    Job executeTime(Dispatcher dispatcher, Runnable runnable, long spaceTime);

    /**
     * 启动延时任务
     *
     * @param dispatcher 任务调度器
     * @param runnable   任务
     * @param delayTime  延时时间
     * @return job
     */

    Job executeDelay(Dispatcher dispatcher, Runnable runnable, long delayTime);

    /**
     * 同时执行多个job，不保证先后顺序
     *
     * @param dispatcher 任务调度器
     * @param runnable   任务
     * @return job 第一个被执行的job
     */
    Job executeJobs(Dispatcher dispatcher, Runnable... runnable);

    /**
     * 清除所有任务
     */
    void clear();


    /**
     * 设置非核心线程最大空闲时间，默认值10000毫秒
     *
     * @param time 毫秒
     */
    void setKeepTime(long time);

    /**
     * 通过task 创建可以被订阅的Observable
     *
     * @param task 任务
     * @param <T>  范型及task任务的返回值类型
     * @return 可订阅的Observable
     */
    <T> Observable<T> create(Task<T> task);


    /**
     * 设置非核心线程最大数量，非核心线程在空闲KeepTime后将结束
     * 一般用于突发性的并发任务，移动平台该数量不应设置过大
     *
     * @param count 非核心线程最大数量
     */
    void setElasticCount(int count);

    /**
     * 设置是否开启额外调用堆栈
     *
     * @param enable
     */
    void setStackTraceExtraEnable(boolean enable);

    /**
     * 是否开启额外调用堆栈
     *
     * @return boolean
     */
    boolean getStackTraceExtraEnable();
}
