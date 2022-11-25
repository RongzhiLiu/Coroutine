package com.lrz.sample;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.lrz.coroutine.Dispatcher;
import com.lrz.coroutine.flow.Function;
import com.lrz.coroutine.flow.Observable;
import com.lrz.coroutine.flow.Observer;
import com.lrz.coroutine.flow.Task;
import com.lrz.coroutine.flow.net.CommonRequest;
import com.lrz.coroutine.flow.net.ReqError;
import com.lrz.coroutine.flow.net.Request;
import com.lrz.coroutine.flow.net.RequestBuilder;
import com.lrz.coroutine.flow.net.RequestException;
import com.lrz.coroutine.handler.CoroutineLRZContext;
import com.lrz.sample.databinding.FragmentFirstBinding;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
            }
        });


        CommonRequest.create(new RequestBuilder<String>() {
            {
                url("https://www.baidu.com");//请求url，也可通过构造函数传入
                addParam("wd", "glide");//添加请求参数
                json("{}");//在请求体中添加json，在post时生效
                addHeader("name", "mark");//添加自定义header
            }
        }).error(error -> {
            Log.e("请求错误", "code=" + error.getCode());
        }).subscribe(s -> {
            Log.e("请求成功", "data=" + s);
        }).POST();


        RequestBuilder<Bean> requestBuilder = new RequestBuilder<Bean>("url") {
            {
                url("url");// 代码块里的url 和构造函数中的url 选一即可，不用都写
                addParam("key", "value");
                addHeader("header", "value");
                json("{}");//在POST请求时上传json，只在POST()时有效
            }
        };

        Request request = CommonRequest.create(requestBuilder)
                .error(error -> {
                    error.printStackTrace();
                    Log.e("请求错误", "code=" + error.getCode() + "   msg=" + error.getMessage());
                }).subscribe(bean -> {
                    Log.i("请求成功", "data=" + bean.str);
                }).GET();

        request.cancel();

        CoroutineLRZContext.INSTANCE.create(new Task<String>() {
            @Override
            public String submit() {
                return "任务结果，由task 的范型来限定返回类型";
            }
        }).subscribe(Dispatcher.IO, str -> {
            //在io线程中接受结果
            Log.i("CoroutineLRZContext", str);
        }).map(str -> {
            // 将结果转换为另一种类型，并交给下一个subscribe处理
            Log.i("CoroutineLRZContext", str);
            return new Bean(str);
        }).subscribe(Dispatcher.MAIN, bean -> {
            // 在主线程中接受结果，此时bean已经被map函数转换为Bean类型了
            Log.i("CoroutineLRZContext", bean.str);
        }).error(throwable -> {
            //捕获一系列事件流处理过程中的异常，如果不设置，则抛出异常
        }).execute(Dispatcher.BACKGROUND);//开始执行任务，并指定线程

        // 先通过create函数创建任务
        CoroutineLRZContext.INSTANCE.create(new Task<String>() {
            @Override
            public String submit() {
                return "任务结果，由task 的范型来限定返回类型";
            }
        }).subscribe(Dispatcher.IO, str -> {//在指定线程中接收结果
            //在io线程中接受结果
            Log.i("CoroutineLRZContext",str);
        }).execute(Dispatcher.BACKGROUND);//开始执行任务，并指定线程


        CoroutineLRZContext.INSTANCE.create(new Task<String>() {
            @Override
            public String submit() {
                return "任务结果，由task 的范型来限定返回类型";
            }
        }).subscribe(Dispatcher.IO, str -> {
            Log.i("Coroutine", str);
        }).map().subscribe(bean -> { //第二个订阅者
            Log.i("Coroutine", bean);
        }).execute(Dispatcher.BACKGROUND);//开始执行任务，并指定线程
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public static class Bean {
        public String str = "";

        public Bean(String str) {
            this.str = str;
        }
    }
}