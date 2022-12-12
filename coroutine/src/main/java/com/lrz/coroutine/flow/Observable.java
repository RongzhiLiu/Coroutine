package com.lrz.coroutine.flow;

import com.lrz.coroutine.Dispatcher;
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
    protected Dispatcher errorDispatcher;
    protected Job job;

    /**
     * 双向链表结构，用于管理责任链中的 Observable，当使用map 函数时，会生成链表
     */
    protected Observable preObservable;
    protected Observable nextObservable;

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
        Observable<T> observable = new Observable<>();
        observable.preObservable = this;
        nextObservable = observable;
        return observable;
    }

    /**
     * 设置错误回调
     *
     * @param error 回调接口
     * @return 任务描述
     */
    public synchronized Observable<T> error(IError error) {
        return error(dispatcher, error);
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
            }
            observable = observable.preObservable;
        }
        return this;
    }

    /**
     * 指定线程，并执行任务
     *
     * @param dispatcher
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
     * @param time       循环间隔
     */
    public synchronized Observable<T> executeTime(Dispatcher dispatcher, long time) {
        Task<?> task = getTask();
        Observable<?> observable = this;
        while (observable != null) {
            observable.taskDispatcher = dispatcher;
            observable = observable.preObservable;
        }
        if (task != null)
            job = CoroutineLRZContext.INSTANCE.executeTime(dispatcher, task, time);
        return this;
    }

    /**
     * 从当前节点向上查找，获取链表中的task
     *
     * @return
     */
    protected Task<?> getTask() {
        if (preObservable != null) {
            return preObservable.getTask();
        }
        return task;
    }

    protected void onError(Throwable e) {
        IError error = this.error;
        if (error != null) {
            Dispatcher dispatcher = errorDispatcher;
            if (dispatcher == null) dispatcher = taskDispatcher;
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
     */
    public void cancel() {
        if (job != null) {
            job.cancel();
            job = null;
        }
        task = null;
        map = null;
        error = null;
        result = null;
        //向上递归取消
        Observable<?> observable = preObservable;
        observable.cancel();
    }

    @Override
    public void close() {
        cancel();
    }
}
