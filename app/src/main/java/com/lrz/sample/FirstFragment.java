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
import com.lrz.coroutine.flow.net.ReqError;
import com.lrz.coroutine.flow.net.ReqObservable;
import com.lrz.coroutine.flow.net.RequestBuilder;
import com.lrz.coroutine.flow.net.RequestException;
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
                }).subscribe(Dispatcher.BACKGROUND, new Observer<String>() {
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
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return "";
            }
        }).subscribe(Dispatcher.IO, str -> {
            Log.i("---任务1-io-subscribe", Thread.currentThread().getName());
        }).error(error -> Log.i("---任务1-error", Thread.currentThread().getName(), error)).thread(Dispatcher.BACKGROUND);//开始执行任务，并指定线程

        ReqObservable<String> observable3 = CommonRequest.Create(new RequestBuilder<String>() {
                    {
                        url("https://www.baidu.com");
                    }
                }).subscribe(s -> {
                    Log.i("---任务request-sub", Thread.currentThread().getName());
                }).error(error -> Log.i("---任务request-error", Thread.currentThread().getName()))
                .method(Method.GET);

        Observable<Integer> obSet = ObservableSet.CreateAnd(observable, observable3)
                .subscribe(Dispatcher.BACKGROUND, aBoolean -> {
                    Log.i("---set-subscribe", Thread.currentThread().getName() + "   " + aBoolean);
                }).error(error -> {
                    Log.i("---set-error", Thread.currentThread().getName(), error);
                });
        Observable<?> time = CoroutineLRZContext.Create(new Task<Void>() {
            @Override
            public Void submit() {
                return null;
            }
        }).delay(1000).thread(Dispatcher.IO).subscribe(unused -> {
            Log.i("---定时任务", Thread.currentThread().getName());
        });
        ObservableSet.CreateOr(false, obSet, time)
                .subscribe(aBoolean -> {
                    Log.i("---set2-subscribe", Thread.currentThread().getName() + "   " + aBoolean);
                }).error(error -> {
                    Log.i("---set2-error", Thread.currentThread().getName(), error);
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

    }
}