package com.lrz.sample;

import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.lrz.coroutine.Dispatcher;
import com.lrz.coroutine.Priority;
import com.lrz.coroutine.flow.Function;
import com.lrz.coroutine.flow.IError;
import com.lrz.coroutine.flow.Observable;
import com.lrz.coroutine.flow.ObservableSet;
import com.lrz.coroutine.flow.Observer;
import com.lrz.coroutine.flow.Task;
import com.lrz.coroutine.flow.net.CommonRequest;
import com.lrz.coroutine.flow.net.Method;
import com.lrz.coroutine.flow.net.ReqError;
import com.lrz.coroutine.flow.net.ReqObservable;
import com.lrz.coroutine.flow.net.RequestBuilder;
import com.lrz.coroutine.flow.net.RequestException;
import com.lrz.coroutine.handler.CoroutineLRZContext;
import com.lrz.coroutine.handler.Job;
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
                Debug.startMethodTracing();
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
    }

    private void streamSet() {
        Observable<String> observable = CoroutineLRZContext.Create(new Task<String>() {
            @Override
            public String submit() {
                Log.i("---任务1", Thread.currentThread().getName());
                return "";
            }
        }).subscribe(Dispatcher.IO, str -> {
            Log.i("---任务1-io-subscribe", Thread.currentThread().getName());
        }).subscribe(bean -> { //第二个订阅者
            Log.i("---任务1-def-subscribe", Thread.currentThread().getName());
        }).error(error -> {
            Log.i("---任务1-error", Thread.currentThread().getName(), error);
        }).thread(Dispatcher.BACKGROUND);//开始执行任务，并指定线程


//        Observable<String> observable2 = CoroutineLRZContext.Create(new Task<String>() {
//            @Override
//            public String submit() {
//                Log.i("---任务2", Thread.currentThread().getName());
//                return "";
//            }
//        }).subscribe(Dispatcher.IO, str -> {
//            Log.i("---任务2-io-subscribe", Thread.currentThread().getName());
//        }).subscribe(Dispatcher.BACKGROUND, bean -> { //第二个订阅者
//            Log.i("---任务2-BACK-subscribe", Thread.currentThread().getName());
//        }).thread(Dispatcher.MAIN);//开始执行任务，并指定线程

        ReqObservable<String> observable3 = CommonRequest.Create(new RequestBuilder<String>() {
            {
                url("https://baidu.com");
            }
        }).subscribe(s -> {
            Log.i("---任务request-subscribe", Thread.currentThread().getName());
        }).error(error -> Log.i("---任务request-error", Thread.currentThread().getName())).subscribe(Dispatcher.IO, s -> {
            Log.i("---任务request-subscribe2", Thread.currentThread().getName());
        }).method(Method.GET);
//
//
        ObservableSet.with(observable, observable3).subscribe(Dispatcher.BACKGROUND, aBoolean -> {
            Log.i("---set-subscribe", Thread.currentThread().getName() + "   " + aBoolean);
        }).error(Dispatcher.MAIN, error -> {
            Log.i("---set-error", Thread.currentThread().getName(), error);
        }).subscribe(Dispatcher.IO, aBoolean -> {
            Log.i("---set-subscribe2-io", Thread.currentThread().getName() + "   " + aBoolean);
        }).execute();


    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


    public void steam() {
        Job job = CoroutineLRZContext.ExecuteTime(Dispatcher.BACKGROUND, () -> {

        }, 1000);
        job.cancel();

        Observable<?> observable = CoroutineLRZContext.Create(new Task<String>(Priority.IMMEDIATE) {
            @Override
            public String submit() {
                return "result";
            }
        }).thread(Dispatcher.BACKGROUND).subscribe(Dispatcher.MAIN, s -> {
            // update ui
        }).subscribe(Dispatcher.IO, s -> {
            // save file
        }).map(s -> Integer.parseInt(s)).subscribe(Dispatcher.IO, integer -> {

        });
        observable.cancel();

        Observable<?> observable2 = CoroutineLRZContext.Create(new Task<String>() {
            @Override
            public String submit() {
                return null;
            }
        }).subscribe(s -> {

        }).thread(Dispatcher.IO);

        ObservableSet.with(observable, observable2).subscribe(Dispatcher.MAIN, aBoolean -> {

        }).subscribe(Dispatcher.IO, aBoolean -> {

        }).execute();


        ReqObservable reqObservable = CommonRequest.Create(new RequestBuilder<String>("") {
            {
                url("");
                addHeader("", "");
                addParam("", "");
                json("");
            }
        }).POST();
        reqObservable.cancel();
    }
}