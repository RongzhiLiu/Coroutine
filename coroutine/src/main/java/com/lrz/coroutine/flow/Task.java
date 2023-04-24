package com.lrz.coroutine.flow;

import com.lrz.coroutine.Priority;
import com.lrz.coroutine.PriorityRunnable;

import java.util.Arrays;

/**
 * Author:  liurongzhi
 * CreateTime:  2022/9/22
 * Description:
 */
public abstract class Task<T> extends PriorityRunnable {
    protected volatile Observable<T> observable;
    /**
     * 额外的方法栈信息，记录了异步在执行之前的发起位置
     */
    private volatile StackTraceElement[] stackTraceExtra;

    public Task(Priority priority) {
        super(priority);
    }

    public Task() {
        super(Priority.MEDIUM);
    }

    public StackTraceElement[] getStackTraceExtra() {
        return stackTraceExtra;
    }

    @Override
    public void run() {
        Observable<T> observable = this.observable;
        if (observable == null || observable.isCancel()) return;
        try {
            T t = submit();
            observable.onSubscribe(t);
        } catch (Throwable throwable) {
            observable.onError(throwable);
        }
    }

    public synchronized void setObservable(Observable<T> observable) {
        this.observable = observable;
    }

    public abstract T submit();

    public synchronized void setStackTraceExtra(StackTraceElement[] stackTraceExtra) {
        this.stackTraceExtra = stackTraceExtra;
    }
}
