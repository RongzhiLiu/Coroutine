package com.lrz.coroutine.flow;

import com.lrz.coroutine.Dispatcher;
import com.lrz.coroutine.LLog;
import com.lrz.coroutine.handler.CoroutineLRZContext;
import com.lrz.coroutine.handler.Job;

import java.io.Closeable;
import java.io.IOException;

/**
 * Author:  liurongzhi
 * CreateTime:  2022/9/22
 * Description:
 */
public class Observable<T> implements Closeable {
    // 观察者线程
    protected Dispatcher dispatcher;
    // 执行者线程
    protected Dispatcher taskDispatcher;
    protected Task<T> task;
    protected Observer<T> result;
    protected Function<T, ?> map;
    protected IError<Throwable> error;
    // 错误回调线程
    protected Dispatcher errorDispatcher;
    protected volatile Job job;
    // 延迟时间
    protected long delay = -1;
    // 循环任务，间隔时间
    protected long interval = -1;
    // 任务是否已经关闭
    private volatile boolean isCancel = false;

    /**
     * 双向链表结构，用于管理责任链中的 Observable，当使用map 函数时，会生成链表
     */
    protected volatile Observable preObservable;
    protected volatile Observable nextObservable;

    public Observable(Task<T> task) {
        if (task == null) {
            throw new NullPointerException("task can not be null!");
        }
        this.task = task;
    }

    protected Observable() {
    }

    public Observer<T> getResult() {
        return result;
    }

    public Observable getPreObservable() {
        return preObservable;
    }

    public Observable getNextObservable() {
        return nextObservable;
    }

    /**
     * 设置订阅者
     *
     * @param result 任务结果回调
     * @return 任务描述
     */
    public synchronized Observable<T> subscribe(Observer<T> result) {
        this.result = result;
        return this;
    }

    /**
     * 设置订阅者，并指定订阅者所在线程
     *
     * @param dispatcher 线程
     * @param result     回调
     * @return 任务描述
     */
    public synchronized Observable<T> subscribe(Dispatcher dispatcher, Observer<T> result) {
        this.dispatcher = dispatcher;
        this.result = result;
        return this;
    }

    /**
     * 按照一定规则转换事件类型，并生成一个新的可被订阅者Observable
     *
     * @param function 转换函数
     * @param <F>      转换的类型
     * @return 可被订阅的Observable
     */
    public synchronized <F> Observable<F> map(Function<T, F> function) {
        this.map = function;
        Observable<F> observableF = new Observable<>();
        observableF.preObservable = this;
        nextObservable = observableF;
        return observableF;
    }

    /**
     * 不转换事件类型生成新的订阅者
     *
     * @return 可被订阅的Observable
     */
    public synchronized Observable<T> map() {
        return map(null);
    }

    /**
     * 设置错误回调
     *
     * @param error 回调接口
     * @return 任务描述
     */
    public synchronized Observable<T> error(IError error) {
        return error(getDispatcher(), error);
    }

    /**
     * 设置错误回调 并设置回调线程，如果没有指定线程，则在任务的执行线程中回调
     *
     * @param dispatcher 回调线程
     * @param error      回调接口
     * @return 任务描述
     */
    public synchronized Observable<T> error(Dispatcher dispatcher, IError error) {
        Observable<?> observable = this;
        while (observable != null) {
            if (observable.task != null) {
                observable.error = error;
                observable.errorDispatcher = dispatcher;
                break;
            } else if (observable.preObservable == null) {
                observable.error = error;
                observable.errorDispatcher = dispatcher;
                break;
            }
            observable = observable.preObservable;
        }
        return this;
    }

    /**
     * 指定线程，不执行
     */
    public synchronized Observable<T> thread(Dispatcher dispatcher) {
        Observable<?> observable = this;
        while (observable != null) {
            observable.taskDispatcher = dispatcher;
            observable = observable.preObservable;
        }
        return this;
    }

    /**
     * 设置任务延迟时间
     *
     * @param delay 任务延迟时间
     * @return
     */
    public synchronized Observable<T> delay(long delay) {
        Observable<?> observable = this;
        while (observable != null) {
            observable.delay = delay;
            observable = observable.preObservable;
        }
        return this;
    }

    /**
     * 设置任务循环时间
     *
     * @param interval 任务循环时间
     * @return
     */
    public synchronized Observable<T> interval(long interval) {
        Observable<?> observable = this;
        while (observable != null) {
            observable.interval = interval;
            observable = observable.preObservable;
        }
        return this;
    }

    /**
     * 获取任务处理线程
     *
     * @return dispatcher
     */
    public synchronized Dispatcher getTaskDispatch() {
        Observable<?> observable = this;
        while (observable != null) {
            if (observable.taskDispatcher != null) return observable.taskDispatcher;
            observable = observable.preObservable;
        }
        return null;
    }

    public synchronized Dispatcher getDispatcher() {
        Observable<?> observable = this;
        while (observable != null) {
            if (observable.dispatcher != null) return observable.dispatcher;
            observable = observable.preObservable;
        }
        return null;
    }

    public Dispatcher getErrorDispatcher() {
        Observable<?> observable = this;
        while (observable != null) {
            if (observable.errorDispatcher != null) return observable.errorDispatcher;
            observable = observable.preObservable;
        }
        return null;
    }

    /**
     * 获取任务延迟时间
     *
     * @return long
     */
    public synchronized long getDelay() {
        Observable<?> observable = this;
        while (observable != null) {
            if (observable.delay > 0) return observable.delay;
            observable = observable.preObservable;
        }
        return delay;
    }

    /**
     * 获取任务循环间隔时间
     *
     * @return
     */
    public synchronized long getInterval() {
        Observable<?> observable = this;
        while (observable != null) {
            if (observable.interval > 0) return observable.interval;
            observable = observable.preObservable;
        }
        return interval;
    }


    /**
     * 在当前线程执行，该线程可能是thread()设置的，如果是null，则不执行
     */
    public synchronized Observable<T> execute() {
        Task<?> task = getTask();
        if (task == null) return this;
        Dispatcher dispatcher = getTaskDispatch();
        if (dispatcher == null) return this;
        long delay = getDelay();
        long interval;
        if (delay > 0) {
            job = CoroutineLRZContext.INSTANCE.executeDelay(dispatcher, task, delay);
        } else if ((interval = getInterval()) > 0) {
            job = CoroutineLRZContext.INSTANCE.executeTime(dispatcher, task, interval);
        } else {
            job = CoroutineLRZContext.INSTANCE.execute(dispatcher, task);
        }
        return this;
    }

    /**
     * 指定线程，并执行任务
     *
     * @param dispatcher 线程
     */
    public synchronized Observable<T> execute(Dispatcher dispatcher) {
        Task<?> task = getTask();
        Observable<?> observable = this;
        while (observable != null) {
            observable.taskDispatcher = dispatcher;
            observable = observable.preObservable;
        }
        if (task != null) {
            job = CoroutineLRZContext.INSTANCE.execute(dispatcher, task);
        }
        return this;
    }

    /**
     * 指定线程池并延迟执行任务
     *
     * @param dispatcher 线程
     * @param delay      延迟时间
     */
    public synchronized Observable<T> executeDelay(Dispatcher dispatcher, long delay) {
        Task<?> task = getTask();
        Observable<?> observable = this;
        while (observable != null) {
            observable.taskDispatcher = dispatcher;
            observable = observable.preObservable;
        }
        if (task != null)
            job = CoroutineLRZContext.INSTANCE.executeDelay(dispatcher, task, delay);
        return this;
    }

    /**
     * 指定线程池并循环执行任务
     *
     * @param dispatcher 线程
     * @param interval   循环间隔
     */
    public synchronized Observable<T> executeTime(Dispatcher dispatcher, long interval) {
        Task<?> task = getTask();
        Observable<?> observable = this;
        while (observable != null) {
            observable.taskDispatcher = dispatcher;
            observable = observable.preObservable;
        }
        if (task != null)
            job = CoroutineLRZContext.INSTANCE.executeTime(dispatcher, task, interval);
        return this;
    }

    /**
     * 从当前节点向上查找，获取链表中的task
     *
     * @return
     */
    protected synchronized Task<?> getTask() {
        Observable pre = preObservable;
        if (pre != null) {
            return pre.getTask();
        }
        return task;
    }

    protected synchronized IError<?> getError() {
        Observable pre = preObservable;
        if (pre != null) {
            return pre.getError();
        }
        return error;
    }

    protected void onError(Throwable e) {
        if (isCancel) return;
        IError error = this.error;
        if (error != null) {
            Dispatcher dispatcher = getErrorDispatcher();
            // 向链表上游获取就近的观察者线程
            if (dispatcher == null) dispatcher = getDispatcher();
            if (dispatcher == null) {
                error.onError(e);
            } else {
                CoroutineLRZContext.INSTANCE.execute(dispatcher, () -> error.onError(e));
            }
        } else {
            throw new CoroutineFlowException("coroutine inner error,look at Cause By...", e);
        }
    }

    /**
     * 任务结果回调
     *
     * @param t 结果
     */
    void onSubscribe(T t) {
        Observer<T> result = this.result;
        if (result != null) {
            if (dispatcher == null) {
                result.onSubscribe(t);
                Observable observable = nextObservable;
                if (observable != null) {
                    Function<T, ?> function = map;
                    if (function != null) {
                        observable.onSubscribe(map.apply(t));
                    } else {
                        observable.onSubscribe(t);
                    }
                }
            } else {
                CoroutineLRZContext.INSTANCE.execute(dispatcher, () -> {
                    try {
                        result.onSubscribe(t);
                        Observable observable = nextObservable;
                        if (observable != null) {
                            Function<T, ?> function = map;
                            if (function != null) {
                                observable.onSubscribe(map.apply(t));
                            } else {
                                observable.onSubscribe(t);
                            }
                        }
                    } catch (Exception e) {
                        dispatchError(e);
                    }
                });
            }
        } else {
            Observable observable = nextObservable;
            if (observable != null) {
                Function<T, ?> function = map;
                if (function != null) {
                    observable.onSubscribe(map.apply(t));
                } else {
                    observable.onSubscribe(t);
                }
            }
        }
    }

    private void dispatchError(Throwable e) {
        Observable observable = this;
        while (observable.error == null) {
            observable = preObservable;
            if (observable == null) return;
        }
        observable.onError(e);
    }


    /**
     * 取消任务执行
     * cancel 是向上传递取消任务流
     * 思考，如果当前observable 处在任务流中间呢，应该如果处置
     */
    public synchronized void cancel() {
        if (isCancel) return;
        if (job != null) {
            job.cancel();
            job = null;
            LLog.d("COROUTINE_OBS", "observable stream close");
        }
        task = null;
        map = null;
        error = null;
        result = null;
        //向上递归取消
        Observable<?> observable = preObservable;
        if (observable != null) {
            //断开双向链表
            observable.nextObservable = null;
            observable.cancel();
        }
        preObservable = null;

        Observable<?> next = nextObservable;
        if (next != null) {
            //断开双向链表
            next.preObservable = null;
            next.cancel();
        }
        nextObservable = null;
        isCancel = true;
    }

    @Override
    public void close() {
        cancel();
    }
}
