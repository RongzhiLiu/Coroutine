package com.lrz.coroutine.flow;

import com.lrz.coroutine.Dispatcher;
import com.lrz.coroutine.handler.CoroutineLRZContext;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author:  liurongzhi
 * CreateTime:  2023/1/29
 * Description: 多个事件流集合
 */
public class ObservableSet extends Observable<Boolean> {
    Observable<?>[] observables;
    AtomicInteger count = new AtomicInteger();
    boolean closeOnError = false;
    volatile long timeOut;
    private volatile boolean isTimeOut = false;

    protected ObservableSet() {
    }

    ObservableSet(Observable<?>[] observables) {
        this.observables = observables;
    }

    public static ObservableSet with(Observable<?>... observable) {
        ObservableSet set = new ObservableSet(observable);
        if (set.observables != null && set.observables.length > 0) {
            for (Observable<?> ob : set.observables) {
                ob.subscribe(o -> set.checkResult());
            }
        }
        return set;
    }

    public static ObservableSet with(boolean closeOnError, Observable<?>... observable) {
        ObservableSet set = new ObservableSet(observable);
        set.closeOnError = closeOnError;
        if (set.observables != null && set.observables.length > 0) {
            for (Observable<?> ob : set.observables) {
                ob.subscribe(o -> set.checkResult());
            }
        }
        return set;
    }

    private Observable<Boolean> timeObservable;
    private Dispatcher timeDispatcher;
    private boolean cancelOnTimeOut = false;

    public synchronized ObservableSet timeOut(long timeOut, Observer<Void> observer) {
        return timeOut(timeOut, Dispatcher.MAIN, observer);
    }

    public synchronized ObservableSet cancelOnTimeOut(boolean cancelOnTimeOut) {
        this.cancelOnTimeOut = cancelOnTimeOut;
        return this;
    }

    public Observable<Boolean> getTimeObservable() {
        ObservableSet set = this;
        while (set != null) {
            if (set.timeObservable != null) return set.timeObservable;
            set = (ObservableSet) set.preObservable;
        }
        return null;
    }

    public Dispatcher getTimeDispatcher() {
        ObservableSet set = this;
        while (set != null) {
            if (set.timeDispatcher != null) return set.timeDispatcher;
            set = (ObservableSet) set.preObservable;
        }
        return null;
    }

    public long getTimeOut() {
        ObservableSet set = this;
        while (set != null) {
            if (set.timeOut != 0) return set.timeOut;
            set = (ObservableSet) set.preObservable;
        }
        return 0;
    }

    /**
     * 是否已经超时
     *
     * @return true 超时
     */
    public synchronized boolean isTimeOut() {
        return isTimeOut;
    }

    /**
     * 设置是否已经超时，内部使用
     */
    private synchronized void timeOutAllReady() {
        this.isTimeOut = true;
    }

    /**
     * 从真正执行开始算超时
     */
    public synchronized ObservableSet timeOut(long timeOut, Dispatcher dispatcher, Observer<Void> observer) {
        if (timeOut == 0 || dispatcher == null) return this;
        this.timeOut = timeOut;
        this.timeDispatcher = dispatcher;
        timeObservable = CoroutineLRZContext.Create(new Task<Boolean>() {
            @Override
            public Boolean submit() {
                System.out.println("---" + count.get() + "   " + observables.length);
                return observables != null && count.get() < observables.length;
            }
        }).subscribe(aBoolean -> {
            if (Boolean.TRUE.equals(aBoolean)) {
                timeOutAllReady();
                //超时
                if (cancelOnTimeOut) {
                    cancel();
                }
                if (observer != null) observer.onSubscribe(null);
            }
        });
        return this;
    }

    private synchronized void checkResult() {
        if (!isTimeOut() && observables != null && count.incrementAndGet() >= observables.length) {
            if (result != null) {
                Observable<?> timeObservable = getTimeObservable();
                if (timeObservable != null) timeObservable.cancel();
                onSubscribe(true);
            }
        }
    }

    /**
     * 复写父类，不处理线程回调，只接收多任务结束事件
     *
     * @param aBoolean
     */
    @Override
    protected void onSubscribe(Boolean aBoolean) {
        if (observables == null || count.get() >= observables.length) {
            super.onSubscribe(aBoolean);
        }
    }

    @Override
    protected synchronized Task<?> getTask() {
        Observable<?> pre = preObservable;
        if (pre != null) {
            return pre.getTask();
        } else if (task == null) {
            task = new Task<Boolean>() {
                @Override
                public Boolean submit() {
                    doObservables();
                    return true;
                }
            };
            task.setObservable(this);
        }
        return task;
    }

    /**
     * 执行多个任务
     */
    private synchronized void doObservables() {
        if (observables != null && observables.length > 0) {
            for (Observable<?> ob : observables) {
                proxyError(ob);
                ob.execute();
            }
        }
    }

    @Override
    public synchronized <F> Observable<F> map(Function<Boolean, F> function) {
        return super.map(function);
    }

    /**
     * 当前线程，立即执行所有任务
     *
     * @return
     */
    @Override
    public synchronized Observable<Boolean> execute() {
        Dispatcher dispatcher = getTaskDispatch();
        if (dispatcher == null) {
            thread(Dispatcher.MAIN);
        }
        Observable<?> timeObservable = getTimeObservable();
        if (timeObservable != null) {
            timeObservable.executeDelay(getTimeDispatcher(), getTimeOut());
        }
        return super.execute();
    }

    @Override
    public synchronized Observable<Boolean> execute(Dispatcher dispatcher) {
        Observable<?> timeObservable = getTimeObservable();
        if (timeObservable != null) {
            timeObservable.executeDelay(getTimeDispatcher(), getTimeOut());
        }
        return super.execute(dispatcher);
    }

    @Override
    public synchronized Observable<Boolean> executeDelay(Dispatcher dispatcher, long delay) {
        Observable<?> timeObservable = getTimeObservable();
        if (timeObservable != null) {
            timeObservable.executeDelay(getTimeDispatcher(), getTimeOut() + delay);
        }
        return super.executeDelay(dispatcher, delay);
    }

    @Override
    public synchronized Observable<Boolean> executeTime(Dispatcher dispatcher, long interval) {
        return super.executeTime(dispatcher, interval);
    }

    private void proxyError(Observable<?> ob) {
        if (getError() != null) {
            IError<?> oldError = ob.getError();
            Dispatcher dispatcher = ob.getErrorDispatcher();
            if (dispatcher == null) dispatcher = ob.getDispatcher();
            ob.error(dispatcher, new InnerError(oldError, dispatcher, this));
        }
    }

    static class InnerError implements IError<Throwable> {
        //原来的error
        private final IError error;
        private final Dispatcher oldDispatch;
        private final ObservableSet observableSet;

        InnerError(IError error, Dispatcher oldDispatch, ObservableSet observableSet) {
            this.error = error;
            this.oldDispatch = oldDispatch;
            this.observableSet = observableSet;
        }

        @Override
        public void onError(Throwable throwable) {
            IError error = this.error;
            if (observableSet.getErrorDispatcher() == null) {
                observableSet.errorDispatcher = observableSet.dispatcher;
            }
            synchronized (observableSet) {
                if (!observableSet.isTimeOut()
                        && observableSet.observables != null
                        && observableSet.count.incrementAndGet() >= observableSet.observables.length){
                    if (observableSet.timeObservable != null) observableSet.timeObservable.cancel();
                }
            }
            if (error != null) {
                if (oldDispatch != null) {
                    CoroutineLRZContext.INSTANCE.execute(oldDispatch, () -> {
                        error.onError(throwable);
                        observableSet.onError(throwable);
                    });
                } else {
                    error.onError(throwable);
                    observableSet.onError(throwable);
                }
            }
        }
    }

    @Override
    protected void onError(Throwable e) {
        super.onError(e);
        if (closeOnError) cancel();
    }

    @Override
    public synchronized void cancel() {
        if (observables != null && observables.length > 0) {
            for (Observable<?> ob : observables) {
                ob.cancel();
            }
            observables = null;
        }
        if (timeObservable != null) {
            timeObservable.cancel();
            timeObservable = null;
        }
        super.cancel();
    }
}
