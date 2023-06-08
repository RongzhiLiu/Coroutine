package com.lrz.sample;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.lrz.coroutine.Dispatcher;
import com.lrz.coroutine.LLog;
import com.lrz.coroutine.Priority;
import com.lrz.coroutine.flow.Emitter;
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
//                streamSet();
                steam();
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
                return "";
            }
        }).subscribe(Dispatcher.IO, str -> {
            Log.i("---任务1-io-subscribe", Thread.currentThread().getName());
        }).error(error -> Log.i("---任务1-error", Thread.currentThread().getName(), error)).thread(Dispatcher.BACKGROUND);//开始执行任务，并指定线程

        Observable<String> observable2 = CoroutineLRZContext.Create(new Task<String>() {
            @Override
            public String submit() {
                return "";
            }
        }).subscribe(Dispatcher.IO, str -> {
            Log.i("---任务2-io-subscribe", Thread.currentThread().getName());
        }).error(error -> Log.i("---任务2-error", Thread.currentThread().getName(), error)).thread(Dispatcher.BACKGROUND);//开始执行任务，并指定线程


        ReqObservable<String> observable3 = CommonRequest.Create(new RequestBuilder<String>() {
                    {
                        url("https://www.baidu.com");
                    }
                }).subscribe(s -> {
                    Log.i("---任务request-sub", Thread.currentThread().getName());
                }).error(error -> Log.i("---任务request-error", Thread.currentThread().getName()))
                .method(Method.GET);

        Observable<Integer> obSet = ObservableSet.CreateAnd(observable, observable2, observable3)
                .subscribe(aBoolean -> {
                    Log.i("---set1-subscribe", Thread.currentThread().getName() + "   " + aBoolean);
                }).error(error -> {
                    Log.i("---set1-error", Thread.currentThread().getName(), error);
                });
        Observable<?> time = CoroutineLRZContext.Create(new Task<Void>() {
            @Override
            public Void submit() {
                return null;
            }
        }).delay(5000).thread(Dispatcher.MAIN).subscribe(unused -> {
            Log.i("---定时任务", Thread.currentThread().getName());
        });
        ObservableSet.CreateOr(false, obSet, time)
                .subscribe(aBoolean -> {
                    time.cancel();
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
        start()
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Integer integer) {
                        Log.i("---2subscribe", Thread.currentThread().getName() + "   " + integer);
                    }
                });
    }

    public Observable<Integer> start() {
        Emitter<Integer> emitter = new Emitter<Integer>() {
        };
        Observable<Integer> observable = CoroutineLRZContext.Create(emitter).subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Integer integer) {
                Log.i("---subscribe", Thread.currentThread().getName() + "   1");
            }
        });
        CoroutineLRZContext.ExecuteDelay(Dispatcher.IO, new Runnable() {
            @Override
            public void run() {
                emitter.next(2);
            }
        },1000);

        return observable;
    }
}