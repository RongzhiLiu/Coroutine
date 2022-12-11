package com.lrz.coroutine.flow.net;

import com.lrz.coroutine.Priority;
import com.lrz.coroutine.flow.Task;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Author:  liurongzhi
 * CreateTime:  2022/11/15
 * Description:
 */
public abstract class RequestBuilder<B> extends Task<B> {
    private String url;
    protected Map<String, String> headers;
    //参数
    private Map<String, String> params;
    //提交json
    private String json;
    //0get,1post 2post_json
    private int method;

    public RequestBuilder(String url) {
        super();
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
        if (method == 0)
            return CommonRequest.request.requestGet(url, params, bClass, headers, observable.hashCode());
        else {
            if (json != null) {
                return CommonRequest.request.postJson(url, params, json, bClass, headers, observable.hashCode());
            }
            return CommonRequest.request.requestPost(url, params, bClass, headers, observable.hashCode());
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
    public void setMethod(int method) {
        this.method = method;
    }
}
