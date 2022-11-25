package com.lrz.coroutine.handler;

import com.lrz.coroutine.Dispatcher;
import com.lrz.coroutine.flow.Observable;
import com.lrz.coroutine.flow.Task;

/**
 * Author And Date: liurongzhi on 2020/2/26.
 * Description: com.yilan.sdk.common.executor
 */
public interface CoroutineLRZContext {
    /**
     * 全局唯一实例
     */
    CoroutineLRZContext INSTANCE = new CoroutineLRZScope();

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
     * @param task 任务
     * @param <T> 范型及task任务的返回值类型
     * @return
     */
    <T> Observable<T> create(Task<T> task);

}
