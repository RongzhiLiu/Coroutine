package com.lrz.coroutine.flow.net;

import com.lrz.coroutine.Dispatcher;
import com.lrz.coroutine.LLog;
import com.lrz.coroutine.flow.Function;
import com.lrz.coroutine.flow.Observable;
import com.lrz.coroutine.flow.Observer;
import com.lrz.coroutine.flow.Task;

import okhttp3.Call;
import okhttp3.OkHttpClient;

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
        return (ReqObservable<T>) super.error(dispatcher, error);
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

    @Override
    protected void onSubscribe(T t) {
        if (t == null && getTask() instanceof RequestBuilder) return;
        super.onSubscribe(t);
    }

    /**
     * 发起get请求
     *
     * @return Request
     */
    @Override
    public final synchronized ReqObservable<T> GET() {
        if (getTask() instanceof RequestBuilder) {
            ((RequestBuilder<?>) getTask()).method(0);
        }
        return execute(taskDispatcher);
    }

    /**
     * 发起post请求
     *
     * @return Request
     */
    @Override
    public final synchronized ReqObservable<T> POST() {
        if (getTask() instanceof RequestBuilder) {
            ((RequestBuilder<?>) getTask()).method(1);
        }
        return execute(taskDispatcher);
    }

    public ReqObservable<T> method(int method) {
        if (getTask() instanceof RequestBuilder) {
            ((RequestBuilder<?>) getTask()).method(method);
        }
        return this;
    }

    @Override
    public synchronized ReqObservable<T> execute(Dispatcher dispatcher) {
        return (ReqObservable<T>) super.execute(dispatcher);
    }

    @Override
    public synchronized ReqObservable<T> execute() {
        if (getTask() instanceof RequestBuilder) {
            // 如果有订阅者，则使用io线程，如果没有，则使用后台线程，表示是非紧急的任务
            if (taskDispatcher == null) {
                taskDispatcher = hasSubscriber() ? Dispatcher.IO : Dispatcher.BACKGROUND;
            }
            if (getError() == null) {
                error(new DefReqError());
            }
            // 提高性能，在这里拦截一部分请求，可以减少分配线程后再判断，浪费资源
            synchronized (RequestBuilder.REQUEST_BUILDERS) {
                if (CommonRequest.requestNum >= CommonRequest.MAX_REQUEST) {
                    RequestBuilder.REQUEST_BUILDERS.add((RequestBuilder<?>) getTask());
                    return this;
                }
            }
        }
        return (ReqObservable<T>) super.execute();
    }

    @Override
    public synchronized void cancel() {
        Task<?> task = getTask();
        int realHash = -1;
        if (task instanceof RequestBuilder) {
            realHash = task.getObservable().hashCode();
            synchronized (RequestBuilder.REQUEST_BUILDERS) {
                RequestBuilder.REQUEST_BUILDERS.remove(task);
            }
        }
        super.cancel();
        if (task instanceof RequestBuilder) {
            OkHttpClient client = ((RequestBuilder<?>) task).getRequest().getClient();
            if (client != null) {
                for (Call call : client.dispatcher().queuedCalls()) {
                    if (Integer.valueOf(realHash).equals(call.request().tag())) {
                        call.cancel();
                        return;
                    }
                }
                for (Call call : client.dispatcher().runningCalls()) {
                    if (Integer.valueOf(realHash).equals(call.request().tag())) {
                        call.cancel();
                        return;
                    }
                }
            }
        }
    }

    @Override
    public synchronized <F> ReqObservable<F> map(Function<T, F> function) {
        return (ReqObservable<F>) super.map(function);
    }

    @Override
    protected synchronized ReqObservable<T> map() {
        return (ReqObservable<T>) super.map();
    }

    private boolean hasSubscriber() {
        Observable<?> pre = this;
        Observable<?> next = getNextObservable();
        while (pre != null || next != null) {
            if ((pre != null && pre.getObserver() != null) || (next != null && next.getObserver() != null)) {
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
            LLog.e("coroutine_def_error", "未处理的错误", error);
        }
    }
}
