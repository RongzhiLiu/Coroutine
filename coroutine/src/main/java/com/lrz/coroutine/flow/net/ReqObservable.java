package com.lrz.coroutine.flow.net;

import android.util.Log;

import com.lrz.coroutine.Dispatcher;
import com.lrz.coroutine.flow.Function;
import com.lrz.coroutine.flow.Observable;
import com.lrz.coroutine.flow.Observer;
import com.lrz.coroutine.flow.Task;

import okhttp3.Call;

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
        return error(Dispatcher.MAIN, error);
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
        return subscribe(Dispatcher.MAIN, result);
    }

    public synchronized ReqObservable<T> subscribe(Dispatcher dispatcher, Observer<T> result) {
        return (ReqObservable<T>) super.subscribe(dispatcher, result);
    }

    /**
     * 发起get请求
     *
     * @return Request
     */
    @Override
    public final synchronized ReqObservable<T> GET() {
        ((RequestBuilder<?>) getTask()).method(0);
        // 如果有订阅者，则使用io线程，如果没有，则使用后台线程，表示是非紧急的任务
        if (taskDispatcher == null) {
            taskDispatcher = hasSubscriber() ? Dispatcher.IO : Dispatcher.BACKGROUND;
        }
        execute(taskDispatcher);
        return this;
    }

    /**
     * 发起post请求
     *
     * @return Request
     */
    @Override
    public final synchronized ReqObservable<T> POST() {
        ((RequestBuilder<?>) getTask()).method(1);
        if (taskDispatcher == null) {
            taskDispatcher = hasSubscriber() ? Dispatcher.IO : Dispatcher.BACKGROUND;
        }
        execute(taskDispatcher);
        return this;
    }

    public ReqObservable<T> method(int method) {
        ((RequestBuilder<?>) getTask()).method(method);
        return this;
    }

    @Override
    public synchronized Observable<T> execute(Dispatcher dispatcher) {
        if (getError() == null) {
            error(new DefReqError());
        }
        return super.execute(dispatcher);
    }

    @Override
    public synchronized Observable<T> execute() {
        if (taskDispatcher == null) {
            taskDispatcher = hasSubscriber() ? Dispatcher.IO : Dispatcher.BACKGROUND;
        }
        if (getError() == null) {
            error(new DefReqError());
        }
        return super.execute();
    }

    @Override
    public synchronized void cancel() {
        super.cancel();
        if (HttpClient.instance != null) {
            for (Call call : HttpClient.instance.getClient().dispatcher().queuedCalls()) {
                if (Integer.valueOf(hashCode()).equals(call.request().tag())) {
                    call.cancel();
                    return;
                }
            }
            for (Call call : HttpClient.instance.getClient().dispatcher().runningCalls()) {
                if (Integer.valueOf(hashCode()).equals(call.request().tag())) {
                    call.cancel();
                    return;
                }
            }
        }
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
