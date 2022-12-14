package com.lrz.coroutine.flow.net;

import android.util.Log;

import com.lrz.coroutine.LLog;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Author And Date: liurongzhi on 2019/11/6.
 * Description: com.yilan.sdk.common.net
 */
public class HttpClient {
    public static volatile HttpClient instance = new HttpClient();
    private OkHttpClient mOkHttp;
    private ClientFactory factory;

    public OkHttpClient getClient() {
        if (mOkHttp == null) {
            synchronized (this) {
                if (mOkHttp == null) {
                    if (factory == null) {
                        mOkHttp = new OkHttpClient.Builder().
                                retryOnConnectionFailure(true)
                                .connectTimeout(10, TimeUnit.SECONDS)
                                .readTimeout(10, TimeUnit.SECONDS)
                                .writeTimeout(10, TimeUnit.SECONDS)
                                .addInterceptor(interceptor)
                                .build();
                    } else {
                        mOkHttp = factory.createClient().addInterceptor(interceptor).build();
                    }
                }
            }
        }
        return mOkHttp;
    }

    public void setHttpFactory(ClientFactory factory) {
        this.factory = factory;
    }

    public interface ClientFactory {
        OkHttpClient.Builder createClient();
    }

    private final Interceptor interceptor = new Interceptor() {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response proceed = chain.proceed(request);
            if (proceed.isSuccessful()) {
                if (LLog.logLevel >= LLog.WARN) {
                    return proceed;
                }
                logRequest(request);
                logResponse(proceed);
            } else {
                String message = proceed.message();
                Log.i("REQUEST", message);
            }
            return proceed;
        }

        private void logRequest(Request request) {
            StringBuilder sb = new StringBuilder();
            sb.append(" ");
            sb.append("\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  ===== Request =====  >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n");
            sb.append("Url:");
            sb.append(request.url()).append("\n");
            sb.append("Method:");
            sb.append(request.method()).append("\n");
            Headers headers = request.headers();
            for (int i = 0; i < headers.size(); i++) {
                sb.append("Headers: ").append(headers.name(i)).append("=").append(headers.value(i));
                if (i != headers.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("\n");
            sb.append("Body:");
            RequestBody body = request.body();
            if (body != null) {
                sb.append(body);
            }
            sb.append("\n······························  ===== HOLD ON =====  ······························");
            Log.i("REQUEST", sb.toString());
        }

        private void logResponse(Response response) {
            StringBuilder sb = new StringBuilder();
            sb.append(" ");
            sb.append("\n<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<  ===== Response =====  <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n");
            sb.append("Url:");
            sb.append(response.request().url()).append("\n");
            sb.append("Method:");
            sb.append(response.request().method()).append("\n");
            sb.append("Code:");
            sb.append(response.code()).append("\n");
            sb.append("Message:");
            sb.append(response.message()).append("\n");
            Headers headers = response.headers();
            sb.append("Headers: ");
            for (int i = 0; i < headers.size(); i++) {
                sb.append(headers.name(i)).append("=").append(headers.value(i));
                if (i != headers.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("\n");
            sb.append("Body:\n");
            try {
                sb.append(new String(response.peekBody(1024 * 1024).bytes()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            sb.append("\n······························  ===== SUCCESS =====  ······························");
            Log.i("REQUEST", sb.toString());
        }
    };
}
