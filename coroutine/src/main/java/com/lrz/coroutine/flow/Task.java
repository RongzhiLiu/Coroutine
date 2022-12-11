package com.lrz.coroutine.flow;

import com.lrz.coroutine.Priority;
import com.lrz.coroutine.PriorityRunnable;

/**
 * Author:  liurongzhi
 * CreateTime:  2022/9/22
 * Description:
 */
public abstract class Task<T> extends PriorityRunnable {
    protected volatile Observable<T> observable;

    public Task(Priority priority) {
        super(priority);
    }
    public Task() {
        super(Priority.MEDIUM);
    }


    @Override
    public void run() {
        Observable<T> observable = this.observable;
        if (observable == null) return;
        try {
            T t = submit();
            observable.onSubscribe(t);
        } catch (Throwable e) {
            observable.onError(e);
        }
    }

    public synchronized void setObservable(Observable<T> observable) {
        this.observable = observable;
    }

    public abstract T submit();
}
