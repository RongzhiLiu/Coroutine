# Coroutine
Android 高性能线程池框架（伪协程）

Java 线程池的缺点：

​	1.在提交新任务时，如果核心线程数没有达到最大，则会继续创建新的线程，而不是查看是否有空闲的线程，

​		在移动平台上，应尽可能利用已有资源，而不是新建线程，华为手机已经开始限制对线程的创建，导致应用崩溃

​	2.异步任务不可控，无法取消

​	3.异步任务出错，无法定位，无法看到发起位置的栈结构

​	4.线程的阻塞依赖起内部任务队列，性能较低

解决以上痛点，于是有了这么一款框架,

1.线程池内部的线程采用HandlerThread来实现，epoll机制

2.采用一个线程池多种线程，可根据业务来定制异步任务的使用

3.一个线程池，统一管理所有线程，搞定所有不同场景的所有任务类型，如定时任务，队列任务，优先级任务，延迟任务等

使用:

```java
// 发起异步任务
Job job = CoroutineLRZContext.INSTANCE.execute(Dispatcher.IO, new Runnable() {
            @Override
            public void run() {
                
            }
        });
// 发起优先级任务
Job job2 = CoroutineLRZContext.INSTANCE.execute(Dispatcher.IO, new PriorityRunnable(Priority.HIGH) {
            @Override
            public void run() {

            }
        });


//取消任务（如果任务还没有开始执行）
job.cancel();

//更多方法见下文
```

#### Dispatcher 有三种类型

​	1.Dispatcher.MAIN：在主线程中执行任务

​	2.Dispatcher.IO：在子线程中执行

​	3.Dispatcher.BACKGROUND：在后台线程中执行

Dispatcher.IO线程最大数量是cpu number，并可弹性2个，非核心线程超过10秒则释放，也可以自定义超时时间

Dispatcher.BACKGROUND：后来线程最大为cpu number/4，最小是1，无非核心线程

#### 任务窃取机制

IO类型的线程和BACKGROUND线程会相互窃取对方的任务执行，以保证在有效资源下的最大并发

1.当IO核心线程满载时，且任务队列中已经积压了一定数量的任务等待执行，则BACKGROUND线程在空闲的情况下会窃取任务，协助IO线程执行任务，反之亦然

#### 所有函数：

```java
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
     * 同时执行多个job，不保证先后顺序,
     * @param dispatcher 任务调度器
     * @param runnable   任务
     * @return job 返回第一个job，调用job.cancel(),会取消所有袭相关任务任务
     */
    Job executeJobs(Dispatcher dispatcher, Runnable... runnable);

    /**
     * 清除所有任务
     */
    void clear();


    /**
     * 设置非核心线程最大空闲时间，默认值10000毫秒
     * @param time 毫秒
     */
    void setKeepTime(long time);
```

