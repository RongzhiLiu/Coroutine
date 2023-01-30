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

    private ObservableSet() {
    }

    public static ObservableSet with(Observable<?>... observable) {
        ObservableSet set = new ObservableSet();
        set.observables = observable;
        if (set.observables != null && set.observables.length > 0) {
            for (Observable<?> ob : set.observables) {
                ob.map().subscribe((Observer) o -> set.checkResult());
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

    @Override
    public synchronized Observable<Boolean> execute() {
        if (observables != null && observables.length > 0) {
            for (Observable<?> ob : observables) {
                proxyError(ob);
                ob.execute();
            }
        }
        return this;
    }

    @Override
    public synchronized Observable<Boolean> execute(Dispatcher dispatcher) {
        if (observables != null && observables.length > 0) {
            for (Observable<?> ob : observables) {
                proxyError(ob);
                ob.execute(dispatcher);
            }
        }
        return this;
    }

    @Override
    public synchronized Observable<Boolean> executeDelay(Dispatcher dispatcher, long delay) {
        if (observables != null && observables.length > 0) {
            for (Observable<?> ob : observables) {
                proxyError(ob);
                ob.executeDelay(dispatcher, delay);
            }
        }
        return this;
    }

    @Override
    public synchronized Observable<Boolean> executeTime(Dispatcher dispatcher, long time) {
        if (observables != null && observables.length > 0) {
            for (Observable<?> ob : observables) {
                proxyError(ob);
                ob.executeTime(dispatcher, time);
            }
        }
        return this;
    }

    private void proxyError(Observable<?> ob) {
        if (error != null) {
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
            if (error != null) {
                error.onError(throwable);
            }
            if (observableSet.getErrorDispatcher() == null) {
                observableSet.errorDispatcher = observableSet.dispatcher;
            }
            observableSet.onError(throwable);
        }
    }

    @Override
    protected void onError(Throwable e) {
        super.onError(e);
        cancel();
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
