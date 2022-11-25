package com.lrz.coroutine.flow;

/**
 * Author:  liurongzhi
 * CreateTime:  2022/9/22
 * Description:
 */
public abstract class Task<T> implements Runnable {
    protected volatile Observable<T> observable;

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
