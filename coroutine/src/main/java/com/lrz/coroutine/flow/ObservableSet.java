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

    protected ObservableSet() {
    }

    ObservableSet(Observable<?>[] observables) {
        this.observables = observables;
    }

    public static ObservableSet with(Observable<?>... observable) {
        ObservableSet set = new ObservableSet(observable);
        if (set.observables != null && set.observables.length > 0) {
            for (Observable<?> ob : set.observables) {
                ob.subscribe((Observer) o -> set.checkResult());
            }
        }
        return set;
    }

    public static ObservableSet with(boolean closeOnError, Observable<?>... observable) {
        ObservableSet set = new ObservableSet(observable);
        set.closeOnError = closeOnError;
        if (set.observables != null && set.observables.length > 0) {
            for (Observable<?> ob : set.observables) {
                ob.subscribe((Observer) o -> set.checkResult());
            }
        }
        return set;
    }

    private synchronized void checkResult() {
        if (observables != null && count.incrementAndGet() >= observables.length) {
            if (result != null) {
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
    void onSubscribe(Boolean aBoolean) {
        if (observables == null || count.get() >= observables.length) {
            super.onSubscribe(aBoolean);
        }
    }

    @Override
    protected synchronized Task<?> getTask() {
        Observable pre = preObservable;
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
            dispatcher = Dispatcher.MAIN;
        }
        long delay = getDelay();
        long interval;

        Task<?> task = getTask();
        if (delay > 0) {
            job = CoroutineLRZContext.INSTANCE.executeDelay(dispatcher, task, delay);
        } else if ((interval = getInterval()) > 0) {
            job = CoroutineLRZContext.INSTANCE.executeTime(dispatcher, task, interval);
        } else {
            job = CoroutineLRZContext.INSTANCE.execute(dispatcher, task);
        }
        return this;
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
        super.cancel();
    }
}
