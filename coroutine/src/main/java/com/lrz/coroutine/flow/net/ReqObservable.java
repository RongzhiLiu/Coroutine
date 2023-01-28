package com.lrz.coroutine.flow.net;

import android.util.Log;

import com.lrz.coroutine.Dispatcher;
import com.lrz.coroutine.flow.Function;
import com.lrz.coroutine.flow.Observable;
import com.lrz.coroutine.flow.Observer;
import com.lrz.coroutine.flow.Task;

/**
 * Author:  liurongzhi
 * CreateTime:  2022/11/15
 * Description:
 */
public class ReqObservable<T> extends Observable<T> {

    public ReqObservable(Task<T> task) {
        super(task);
    }

    public ReqObservable() {
        super();
    }

    public synchronized ReqObservable<T> error(ReqError error) {
        super.error(error);
        this.errorDispatcher = Dispatcher.MAIN;
        return this;
    }

    public synchronized ReqObservable<T> error(Dispatcher dispatcher, ReqError error) {
        super.error(dispatcher, error);
        return this;
    }

    @Override
    protected void onError(Throwable e) {
        if (!(e instanceof RequestException)) {
            e = new RequestException("local logic error!,look at Caused by ...", e, ResponseCode.CODE_ERROR_LOCAL);
        }
        super.onError(e);
    }

    @Override
    public synchronized ReqObservable<T> subscribe(Observer<T> result) {
        super.subscribe(result);
        dispatcher = Dispatcher.MAIN;
        return this;
    }

    public synchronized ReqObservable<T> subscribe(Dispatcher dispatcher, Observer<T> result) {
        this.dispatcher = dispatcher;
        this.result = result;
        return this;
    }

    /**
     * 发起get请求
     *
     * @return Request
     */
    public final synchronized Request GET() {
        ((RequestBuilder<?>) getTask()).setMethod(0);
        // 如果有订阅者，则使用io线程，如果没有，则使用后台线程，表示是非紧急的任务
        Dispatcher dispatcher = hasSubscriber() ? Dispatcher.IO : Dispatcher.BACKGROUND;
        if (error == null) {
            error(new DefReqError());
        }
        execute(dispatcher);
        return new Request(this);
    }

    /**
     * 发起post请求
     *
     * @return Request
     */
    public final synchronized Request POST() {
        ((RequestBuilder<?>) getTask()).setMethod(1);
        Dispatcher dispatcher = hasSubscriber() ? Dispatcher.IO : Dispatcher.BACKGROUND;
        if (error == null) {
            error(new DefReqError());
        }
        execute(dispatcher);
        return new Request(this);
    }

    @Override
    public synchronized ReqObservable<T> map() {
        ReqObservable<T> observable = new ReqObservable<>();
        observable.preObservable = this;
        nextObservable = observable;
        return observable;
    }

    @Override
    public synchronized <F> ReqObservable<F> map(Function<T, F> function) {
        this.map = function;
        ReqObservable<F> observableF = new ReqObservable<>();
        observableF.preObservable = this;
        nextObservable = observableF;
        return observableF;
    }

    private boolean hasSubscriber() {
        Observable<?> pre = this;
        Observable<?> next = getNextObservable();
        while (pre != null || next != null) {
            if ((pre != null && pre.getResult() != null) || (next != null && next.getResult() != null)) {
                return true;
            }
            if (pre != null) {
                pre = pre.getPreObservable();
            }
            if (next != null) {
                next = next.getNextObservable();
            }
        }
        return false;
    }

    public static class DefReqError implements ReqError {

        @Override
        public void onError(RequestException error) {
            Log.e("coroutine_def_error", "未处理的错误", error);
        }
    }
}
