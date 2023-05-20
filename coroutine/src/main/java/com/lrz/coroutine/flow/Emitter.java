package com.lrz.coroutine.flow;

/**
 * Author:  liurongzhi
 * CreateTime:  2023/3/6
 * Description: 发射器
 * 发射器不是由线程主动执行，而是由外部调用，发射事件，事件类型由范性来控制
 */
public abstract class Emitter<T> extends Task<T> {
    @Override
    public void run() {

    }

    @Override
    public T submit() {
        return null;
    }

    /**
     * 发射器发射事件，由外部调用，可多次调用
     * 当 observable 被close后，不可调用
     *
     * @param t
     */
    public void next(T t) {
        Observable<T> observable = this.observable;
        if (observable == null || observable.isCancel()) return;
        try {
            observable.onSubscribe(t);
        } catch (Throwable throwable) {
            observable.onError(throwable);
        }
    }

    /**
     * 发送错误事件
     * @param throwable 自定义或已有的异常
     */
    public void next(Throwable throwable) {
        Observable<T> observable = this.observable;
        if (observable == null || observable.isCancel()) return;
        observable.onError(throwable);
    }
}
