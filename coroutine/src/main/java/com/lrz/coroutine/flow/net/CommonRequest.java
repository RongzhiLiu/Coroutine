package com.lrz.coroutine.flow.net;

import android.os.Looper;
import android.text.TextUtils;

import com.google.gson.Gson;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Author And Date: liurongzhi on 2020/7/28.
 * Description: http请求类
 */
public class CommonRequest {
    //预解析字段，可将 自定义的code归类到错误中
    // code的值不等于200，则表示服务获取失败
    private static String codeStr;
    private static String msgStr;
    private static final Gson GSON = new Gson();

    /**
     * 设置设置预解析的code字段
     */
    public static void setCodeStr(String codeStr) {
        CommonRequest.codeStr = codeStr;
    }

    /**
     * 设置设置预解析的message字段
     */
    public static void setMsgStr(String msgStr) {
        CommonRequest.msgStr = msgStr;
    }

    public static <T> ReqObservable<T> Create(RequestBuilder<T> task) {
        ReqObservable<T> observable;
        observable = new ReqObservable<>(task);
        task.setObservable(observable);
        return observable;
    }


    public static final CommonRequest request = new CommonRequest(HttpClient.instance.getClient());
    private final OkHttpClient client;

    public CommonRequest(OkHttpClient client) {
        this.client = client;
    }

    public <D> D requestGet(String url, Class<D> dClass) throws RequestException {
        return requestGet(url, null, dClass, null, 0);
    }

    public String requestGet(String url, Map<String, String> params) throws RequestException {
        return requestGet(url, params, String.class, null, 0);
    }

    public <D> D requestGet(String url, Map<String, String> params, Class<D> dClass) throws RequestException {
        return requestGet(url, params, dClass, null, 0);
    }


    public <D> D requestGet(final String url, final Map<String, String> params, Class<D> dClass, Map<String, String> header, int tag) throws RequestException {
        if (url == null || url.length() < 1) {
            throw new RequestException("url is illegal,please check you url", ResponseCode.CODE_ERROR_URL_ILLEGAL);
        }
        OkHttpClient mOkHttp = client;
        HttpUrl httpUrl = HttpUrl.parse(url);
        if (httpUrl == null) {
            throw new RequestException("url parse error,please check you url", ResponseCode.CODE_ERROR_URL_ILLEGAL);
        }
        HttpUrl.Builder httpBuilder = httpUrl.newBuilder();
        if (params != null && !params.isEmpty()) {
            for (String key : params.keySet()) {
                Object value = params.get(key);
                String svalue = value == null ? "" : value.toString();
                httpBuilder.addQueryParameter(key, svalue);
            }
        }

        okhttp3.Request.Builder builder = new okhttp3.Request.Builder();
        if (header != null && !header.isEmpty()) {
            for (Map.Entry<String, String> entry : header.entrySet()) {
                if (!TextUtils.isEmpty(entry.getKey()) && !TextUtils.isEmpty(entry.getValue())) {
                    builder.addHeader(entry.getKey(), entry.getValue());
                }
            }
        }
        builder.url(httpBuilder.build());
        return execute(mOkHttp, builder, dClass, tag);
    }

    public String requestPost(final String url, Map<String, String> params) throws RequestException {
        return requestPost(url, params, String.class);
    }

    public <D> D requestPost(final String url, Class<D> dClass) throws RequestException {
        return requestPost(url, null, dClass);
    }

    public <D> D requestPost(final String url, Map<String, String> params, Class<D> dClass) throws RequestException {
        return requestPost(url, params, dClass, null, 0);
    }

    public <D> D requestPost(final String url, Map<String, String> params, Class<D> dClass, Map<String, String> header, int tag) throws RequestException {
        if (url == null || url.length() < 1) {
            throw new RequestException("url is illegal,please check you url", ResponseCode.CODE_ERROR_URL_ILLEGAL);
        }

        OkHttpClient mOkHttp = client;
        HttpUrl httpUrl = HttpUrl.parse(url);
        if (httpUrl == null) {
            throw new RequestException("url parse error,please check you url", ResponseCode.CODE_ERROR_URL_ILLEGAL);
        }
        RequestBody requestBody;
        FormBody.Builder formBuilder = new FormBody.Builder();
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                formBuilder.add(entry.getKey(), entry.getValue());
            }

        }
        requestBody = formBuilder.build();
        HttpUrl.Builder httpBuilder = httpUrl.newBuilder();
        okhttp3.Request.Builder builder = new okhttp3.Request.Builder()
                .post(requestBody)
                .url(httpBuilder.build());
        if (header != null && !header.isEmpty()) {
            for (Map.Entry<String, String> entry : header.entrySet()) {
                if (!TextUtils.isEmpty(entry.getKey()) && !TextUtils.isEmpty(entry.getValue())) {
                    builder.addHeader(entry.getKey(), entry.getValue());
                }
            }
        }
        return execute(mOkHttp, builder, dClass, tag);
    }

    private <D> D execute(OkHttpClient mOkHttp, okhttp3.Request.Builder builder, Class<D> dClass, int tag) throws RequestException {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            throw new RequestException("can not request in main thread!", ResponseCode.CODE_ERROR_THREAD);
        }
        Call call;
        Response response;
        try {
            call = mOkHttp.newCall(builder.tag(tag).build());
            response = call.execute();
        } catch (Exception e) {
            throw new RequestException("Network exception, please check the network! or look at Caused by ...", e, ResponseCode.CODE_ERROR_NO_NET);
        }
        ResponseBody body = response.body();
        if (body != null) {
            if (response.code() == 200) {
                final String json;
                try {
                    json = body.string();
                } catch (IOException e) {
                    throw new RequestException("response.body stream read error! or look at Caused by ...", e, ResponseCode.CODE_ERROR_IO);
                }
                // 尝试预解析 code 和msg 字段
                if (codeStr != null && msgStr != null) {
                    try {
                        JSONObject jo = new JSONObject(json);
                        int code = jo.getInt(codeStr);
                        if (code != 200) {
                            String msg = jo.getString(msgStr);
                            throw new RequestException(msg, code);
                        }
                    } catch (JSONException e) {
                        throw new RequestException("code or msg decode error! or look at Caused by ...", e, ResponseCode.CODE_ERROR_JSON_FORMAT);
                    }
                }
                if (dClass == String.class || dClass == null) {
                    return (D) json;
                } else {
                    try {
                        return GSON.fromJson(json, dClass);
                    } catch (Exception e) {
                        throw new RequestException("json decode error! or look at Caused by ...", e, ResponseCode.CODE_ERROR_JSON_FORMAT);
                    }
                }
            } else {
                body.close();
                throw new RequestException("Request Error, please check the error code!", response.code());
            }
        } else {
            throw new RequestException("No data requested!", ResponseCode.CODE_ERROR_RESPONSE_NULL);
        }
    }

    public <D> D postJson(final String url, Map<String, String> urlParams, String json, Class<D> dClass, Map<String, String> header, int tag) throws RequestException {
        if (url == null || url.length() < 1) {
            throw new RequestException("url is illegal,please check you url", ResponseCode.CODE_ERROR_URL_ILLEGAL);
        }
        OkHttpClient mOkHttp = client;
        HttpUrl httpUrl = HttpUrl.parse(url);
        if (httpUrl == null) {
            throw new RequestException("url parse error,please check you url", ResponseCode.CODE_ERROR_URL_ILLEGAL);
        }
        RequestBody requestBody = FormBody.create(MediaType.parse("application/json; charset=utf-8"), json);

        HttpUrl.Builder httpBuilder = httpUrl.newBuilder();
        if (urlParams != null) {
            for (String key : urlParams.keySet()) {
                Object value = urlParams.get(key);
                String svalue = value == null ? "" : value.toString();
                httpBuilder.addQueryParameter(key, svalue);
            }
        }
        okhttp3.Request.Builder builder = new okhttp3.Request.Builder()
                .post(requestBody)
                .url(httpBuilder.build());
        if (header != null && !header.isEmpty()) {
            for (Map.Entry<String, String> entry : header.entrySet()) {
                if (!TextUtils.isEmpty(entry.getKey()) && !TextUtils.isEmpty(entry.getValue())) {
                    builder.addHeader(entry.getKey(), entry.getValue());
                }
            }
        }
        return execute(mOkHttp, builder, dClass, tag);
    }
}
