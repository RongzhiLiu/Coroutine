package com.lrz.coroutine.flow.net;

import com.lrz.coroutine.flow.Observable;

import java.io.Closeable;
import okhttp3.Call;

/**
 * Author:  liurongzhi
 * CreateTime:  2022/11/17
 * Description:
 */
public class Request implements Closeable {
    private final Observable observable;

    public Request(Observable observable) {
        this.observable = observable;
    }

    public void cancel() {
        observable.cancel();
        if (HttpClient.instance != null) {
            for (Call call : HttpClient.instance.getClient().dispatcher().queuedCalls()) {
                if (Integer.valueOf(observable.hashCode()).equals(call.request().tag())) {
                    call.cancel();
                    return;
                }
            }
            for (Call call : HttpClient.instance.getClient().dispatcher().runningCalls()) {
                if (Integer.valueOf(observable.hashCode()).equals(call.request().tag())) {
                    call.cancel();
                    return;
                }
            }
        }
    }

    @Override
    public void close() {
        cancel();
    }
}
