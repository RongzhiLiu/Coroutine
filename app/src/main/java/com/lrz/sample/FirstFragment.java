package com.lrz.sample;

import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.lrz.coroutine.Dispatcher;
import com.lrz.coroutine.flow.Observable;
import com.lrz.coroutine.flow.ObservableSet;
import com.lrz.coroutine.flow.Observer;
import com.lrz.coroutine.flow.Task;
import com.lrz.coroutine.flow.net.CommonRequest;
import com.lrz.coroutine.flow.net.Method;
import com.lrz.coroutine.flow.net.ReqObservable;
import com.lrz.coroutine.flow.net.RequestBuilder;
import com.lrz.coroutine.handler.CoroutineLRZContext;
import com.lrz.sample.databinding.FragmentFirstBinding;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private volatile int mainExe = 0;
    private volatile int mainSub = 0;
    private volatile int ioExe = 0;
    private volatile int ioSub = 0;
    private volatile int backExe = 0;
    private volatile int backSub = 0;

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
        binding.buttonIo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CoroutineLRZContext.Create(new Task<String>() {
                    @Override
                    public String submit() {
                        return "null";
                    }
                }).subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(String s) {
                        System.out.println("onSubscribe--------thread=" + Thread.currentThread().getName());
                        int i = 1 / 0;
                    }
                }).error(error -> System.out.println("error--------thread=" + Thread.currentThread().getName())).execute(Dispatcher.IO);
            }
        });

        binding.buttonMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                streamSet();
            }
        });

        binding.buttonBackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("-------start " + SystemClock.uptimeMillis());
                CoroutineLRZContext.ExecuteTime(Dispatcher.MAIN, new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("-------ExecuteTime ");
                    }
                }, 1000);
            }
        });

//        Request request = CommonRequest.Create(new RequestBuilder<String>() {
//            {
//                url("https://www.baidu.com");//请求url，也可通过构造函数传入
//                addParam("wd", "glide");//添加请求参数
//                json("{}");//在请求体中添加json，在post时生效
//                addHeader("name", "mark");//添加自定义header
//            }
//        }).error(error -> {
//            Log.e("请求错误", "code=" + error.getCode());
//        }).map(new Function<String, Bean>() {
//            @Override
//            public Bean apply(String s) {
//                return new Bean(s);
//            }
//        }).subscribe(s -> {
//            Log.e("请求成功", "data=" + s);
//        }).POST();
//
//        RequestBuilder<Bean> requestBuilder = new RequestBuilder<Bean>("url") {
//            {
//                url("url");// 代码块里的url 和构造函数中的url 选一即可，不用都写
//                addParam("key", "value");
//                addHeader("header", "value");
//                json("{}");//在POST请求时上传json，只在POST()时有效
//            }
//        };

//        Request request = CommonRequest.Create(requestBuilder)
//                .error(error -> {
//                    error.printStackTrace();
//                    Log.e("请求错误", "code=" + error.getCode() + "   msg=" + error.getMessage());
//                }).subscribe(bean -> {
//                    Log.i("请求成功", "data=" + bean.str);
//                }).GET();
//
//        request.cancel();
//
//        CoroutineLRZContext.INSTANCE.create(new Task<String>() {
//            @Override
//            public String submit() {
//                return "任务结果，由task 的范型来限定返回类型";
//            }
//        }).subscribe(Dispatcher.IO, str -> {
//            //在io线程中接受结果
//            Log.i("CoroutineLRZContext", str);
//        }).map(str -> {
//            // 将结果转换为另一种类型，并交给下一个subscribe处理
//            Log.i("CoroutineLRZContext", str);
//            return new Bean(str);
//        }).subscribe(Dispatcher.MAIN, bean -> {
//            // 在主线程中接受结果，此时bean已经被map函数转换为Bean类型了
//            Log.i("CoroutineLRZContext", bean.str);
//        }).error(throwable -> {
//            //捕获一系列事件流处理过程中的异常，如果不设置，则抛出异常
//        }).execute(Dispatcher.BACKGROUND);//开始执行任务，并指定线程
//
//        // 先通过create函数创建任务
//        CoroutineLRZContext.INSTANCE.create(new Task<String>() {
//            @Override
//            public String submit() {
//                return "任务结果，由task 的范型来限定返回类型";
//            }
//        }).subscribe(Dispatcher.IO, str -> {//在指定线程中接收结果
//            //在io线程中接受结果
//            Log.i("CoroutineLRZContext",str);
//        }).execute(Dispatcher.BACKGROUND);//开始执行任务，并指定线程
//
        streamSet();
    }

    private void streamSet() {
        Observable<String> observable = CoroutineLRZContext.Create(new Task<String>() {
            @Override
            public String submit() {
                Log.i("---任务1", Thread.currentThread().getName());
                int i = 1 / 0;
                return "";
            }
        }).subscribe(Dispatcher.IO, str -> {
            Log.i("---任务1-io-subscribe", Thread.currentThread().getName());
        }).map().subscribe(Dispatcher.IO, bean -> { //第二个订阅者
            Log.i("---任务1-io2-subscribe", Thread.currentThread().getName());
        }).thread(Dispatcher.BACKGROUND).delay(1000);//开始执行任务，并指定线程

        Observable<String> observable2 = CoroutineLRZContext.Create(new Task<String>() {
            @Override
            public String submit() {
                Log.i("---任务2", Thread.currentThread().getName());
                return "";
            }
        }).subscribe(Dispatcher.IO, str -> {
            Log.i("---任务2-io-subscribe", Thread.currentThread().getName());
        }).map().subscribe(Dispatcher.IO, bean -> { //第二个订阅者
            Log.i("---任务2-io2-subscribe", Thread.currentThread().getName());
        }).thread(Dispatcher.MAIN);//开始执行任务，并指定线程

        ReqObservable<String> observable3 = CommonRequest.Create(new RequestBuilder<String>() {
            {
                url("https://baidu.com");
            }
        }).subscribe(s -> {
            Log.i("---任务request-subscribe", Thread.currentThread().getName());
        }).error(e -> {
            Log.i("---任务request-error", Thread.currentThread().getName());
            Log.e("", "", e);
        }).method(Method.GET);


        ObservableSet.with(observable, observable2, observable3).subscribe(Dispatcher.BACKGROUND, aBoolean -> {
            Log.i("---set-subscribe", Thread.currentThread().getName() + "   " + aBoolean);
        }).error(Dispatcher.MAIN, error -> {
            Log.i("---set-error", Thread.currentThread().getName());
        }).execute();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}