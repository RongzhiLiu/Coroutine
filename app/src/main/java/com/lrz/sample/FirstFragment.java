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
import com.lrz.coroutine.Priority;
import com.lrz.coroutine.flow.Emitter;
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

import java.util.concurrent.PriorityBlockingQueue;

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
                Emitter<String> emitter = new Emitter<String>() {
                };
                Observable<String> observable3 = CoroutineLRZContext.Create(emitter).subscribe(Dispatcher.IO, s -> {
                    Log.i("---request:", "执行" + Thread.currentThread().getName());
                }).error(error -> {
                    Log.e("---request-error", Thread.currentThread().getName());
                }).subscribe(Dispatcher.BACKGROUND,new Observer<String>() {
                    @Override
                    public void onSubscribe(String s) {
                        Log.i("---request2:", "执行" + Thread.currentThread().getName());
                    }
                }).execute(Dispatcher.IO);
                emitter.next(new Throwable());
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
                emit();
            }
        });
    }

    ReqObservable<String> observable4;
    ReqObservable<String> observable8;

    private void emit() {
        for (int i = 0; i < 10; i++) {
            final int ii = i;
            ReqObservable<String> observable3 = CommonRequest.Create(new RequestBuilder<String>() {
                {
                    url("https://www.baidu.com");
                }
            }).subscribe(Dispatcher.IO, s -> {
                Log.i("---request:" + ii, "执行" + Thread.currentThread().getName());

            }).error(error -> {
                Log.e("---request-error" + ii, "", error);
            }).GET();
            if (ii == 4) {
                observable4 = observable3;
            }
            if (ii == 8) {
                observable8 = observable3;
            }
        }
        CoroutineLRZContext.ExecuteDelay(Dispatcher.MAIN, new Runnable() {
            @Override
            public void run() {
                if (observable4 != null) {
                    observable4.cancel();
                    observable4 = null;
                    Log.e("---request-cancel4", "");
                }
                if (observable8 != null) {
                    observable8.cancel();
                    observable8 = null;
                    Log.e("---request-cancel8", "");
                }
            }
        }, 50);
    }

    private void streamSet() {
        Observable<String> observable = CoroutineLRZContext.Create(new Task<String>() {
            @Override
            public String submit() {
                Log.i("---任务1", Thread.currentThread().getName());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                int i= 1/0;
                return "";
            }
        }).subscribe(Dispatcher.IO, str -> {
            Log.i("---任务1-io-subscribe", Thread.currentThread().getName());
        }).subscribe(bean -> { //第二个订阅者
            Log.i("---任务1-def-subscribe", Thread.currentThread().getName());
        }).error(error -> {
            Log.i("---任务1-error", Thread.currentThread().getName());
        }).thread(Dispatcher.BACKGROUND);//开始执行任务，并指定线程

        Observable<String> observable2 = CoroutineLRZContext.Create(new Task<String>() {
            @Override
            public String submit() {
                Log.i("---任务2", Thread.currentThread().getName());
                return "";
            }
        }).subscribe(Dispatcher.IO, str -> {
            Log.i("---任务2-io-subscribe", Thread.currentThread().getName());
        }).subscribe(Dispatcher.BACKGROUND, bean -> { //第二个订阅者
            Log.i("---任务2-BACK-subscribe", Thread.currentThread().getName());
        }).thread(Dispatcher.MAIN);//开始执行任务，并指定线程

        ReqObservable<String> observable3 = CommonRequest.Create(new RequestBuilder<String>() {
            {
                url("https://www.baidu.com");
            }
        }).subscribe(s -> {
            Log.i("---任务request-sub", Thread.currentThread().getName());
        }).subscribe(Dispatcher.IO, s -> {
            Log.i("---任务request-subIO", Thread.currentThread().getName());
        }).error(error -> Log.i("---任务request-error", Thread.currentThread().getName())).subscribe(Dispatcher.IO, s -> {
            Log.i("---任务request-subscribe2", Thread.currentThread().getName());
        }).method(Method.GET);

        ObservableSet.with(false, observable, observable2, observable3)
                .cancelOnTimeOut(true)
                .timeOut(2000, unused -> Log.i("---set-timeout", Thread.currentThread().getName()))
                .subscribe(Dispatcher.BACKGROUND, aBoolean -> {
                    Log.i("---set-subscribe", Thread.currentThread().getName() + "   " + aBoolean);
                }).error(Dispatcher.MAIN, error -> {
                    Log.i("---set-error", Thread.currentThread().getName());
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
        Observable<String> observable1 = CoroutineLRZContext.Create(new Emitter<String>() {
        }).subscribe(Dispatcher.MAIN, s -> {
            // 展示dialog1
        });

        Observable<Integer> observable2 = CoroutineLRZContext.Create(new Emitter<Integer>() {
        }).subscribe(Dispatcher.MAIN, i -> {
            // 展示dialog2
        });

        ObservableSet.with(observable1, observable2);
    }
}