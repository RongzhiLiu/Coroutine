package com.lrz.coroutine.flow.net;

import com.lrz.coroutine.Priority;
import com.lrz.coroutine.flow.Task;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/**
 * Author:  liurongzhi
 * CreateTime:  2022/11/15
 * Description:
 */
public abstract class RequestBuilder<B> extends Task<B> {
    static final ArrayDeque<RequestBuilder<?>> REQUEST_BUILDERS = new ArrayDeque<>();
    protected CommonRequest request;
    private String url;
    protected Map<String, String> headers;
    //参数
    private Map<String, String> params;
    //提交json
    private String json;
    //0get,1post 2post_json
    private int method;

    public RequestBuilder(String url) {
        super(Priority.HIGH);
        this.url = url;
    }

    public RequestBuilder(String url, Priority priority) {
        super(priority);
        this.url = url;
    }

    public RequestBuilder() {
        super();
    }

    @Override
    public B submit() {
        Type superClass = getClass().getGenericSuperclass();
        Type type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
        Class<B> bClass;
        if (type instanceof ParameterizedType) {
            bClass = (Class<B>) ((ParameterizedType) type).getRawType();
        } else {
            bClass = (Class<B>) type;
        }
        try {
            if (method == 0)
                return request.requestGet(url, params, bClass, headers, observable.hashCode());
            else {
                if (json != null) {
                    return request.postJson(url, params, json, bClass, headers, observable.hashCode());
                }
                return request.requestPost(url, params, bClass, headers, observable.hashCode());
            }
        } catch (RequestException e) {
            if (e.getCode() == ResponseCode.CODE_ERROR_WAIT) {
                //如果是队列已满
                synchronized (REQUEST_BUILDERS) {
                    REQUEST_BUILDERS.add(this);
                }
                return null;
            } else throw e;
        } finally {
            RequestBuilder.exeWait();
        }

    }

    public RequestBuilder<B> url(String url) {
        this.url = url;
        return this;
    }

    public RequestBuilder<B> addHeader(String header, String value) {
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put(header, value);
        return this;
    }

    public RequestBuilder<B> addParam(String key, String value) {
        if (this.params == null) {
            this.params = new HashMap<>();
        }
        this.params.put(key, value);
        return this;
    }

    public RequestBuilder<B> json(String json) {
        this.json = json;
        return this;
    }


    /**
     * 设置请求方式
     *
     * @param method
     */
    public RequestBuilder<B> method(int method) {
        this.method = method;
        return this;
    }

    public int getMethod() {
        return method;
    }

    public void setRequest(CommonRequest request) {
        this.request = request;
    }

    public CommonRequest getRequest() {
        return request;
    }

    public static void exeWait() {
        synchronized (REQUEST_BUILDERS) {
            Task<?> task = REQUEST_BUILDERS.pollFirst();
            if (task != null) {
                task.getObservable().execute();
            }
        }
    }
}
