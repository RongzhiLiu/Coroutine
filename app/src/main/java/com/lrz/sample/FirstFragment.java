package com.lrz.sample;

import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.lrz.coroutine.Dispatcher;
import com.lrz.coroutine.flow.Error;
import com.lrz.coroutine.flow.Function;
import com.lrz.coroutine.flow.Observer;
import com.lrz.coroutine.flow.Task;
import com.lrz.coroutine.flow.net.CommonRequest;
import com.lrz.coroutine.flow.net.Request;
import com.lrz.coroutine.flow.net.RequestBuilder;
import com.lrz.coroutine.handler.CoroutineLRZContext;
import com.lrz.coroutine.handler.Job;
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

    Job job;

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
                }).error(new Error() {
                    @Override
                    public void onError(Throwable error) {
                        System.out.println("error--------thread=" + Thread.currentThread().getName());
                    }
                }).execute(Dispatcher.IO);
            }
        });

        binding.buttonMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (job != null) {
                    job.cancel();
                    job = null;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    long start = SystemClock.uptimeMillis();
                    for (int i = 0; i < 100000; i++) {
                        String s = WebSettings.getDefaultUserAgent(getActivity());
                    }

                    System.out.println("-----time = " + (SystemClock.uptimeMillis() - start));
                }

//                for (int i = 0; i < 5; i++) {
//                    synchronized (FirstFragment.this) {
//                        mainExe += 1;
//                    }
//                    CoroutineLRZContext.Create(new Task<String>() {
//                        @Override
//                        public String submit() {
//                            return Thread.currentThread().getName();
//                        }
//                    }).subscribe(new Observer<String>() {
//                        @Override
//                        public void onSubscribe(String s) {
//                            synchronized (FirstFragment.this) {
//                                mainSub += 1;
//                                System.out.println("-----main ??????????????????" + mainExe + " ,????????????" + mainSub + "  thread=" + s);
//                            }
//
//                        }
//                    }).execute(Dispatcher.MAIN);
//                }
            }
        });
        CoroutineLRZContext.Execute(Dispatcher.IO, new Runnable() {
            @Override
            public void run() {
                String name = Thread.currentThread().getName();
                System.out.println("---------io=" + name);
            }
        });
        System.out.println("---------???????????????=" + Thread.currentThread().getName());
        CoroutineLRZContext.ExecuteDelay(Dispatcher.MAIN, new Runnable() {
            @Override
            public void run() {
//                String name = Thread.currentThread().getName();
//                if (!name.contains("main")) {
//                    System.out.println("++++++???????????????=" + name);
//                }
                int i= 1/0;
            }
        }, 3000);
        CoroutineLRZContext.Execute(Dispatcher.IO, new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10000; i++) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("++++++1 send");
                    CoroutineLRZContext.ExecuteDelay(Dispatcher.MAIN, new Runnable() {
                        @Override
                        public void run() {
                            String name = Thread.currentThread().getName();
                            System.out.println("++++++1 do=" + name);
                        }
                    }, 1);
                }
            }
        });

        CoroutineLRZContext.ExecuteTime(Dispatcher.IO, new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    System.out.println("++++++2 send");
                    CoroutineLRZContext.ExecuteDelay(Dispatcher.MAIN, new Runnable() {
                        @Override
                        public void run() {
                            String name = Thread.currentThread().getName();
                            System.out.println("++++++2 do=" + name);

                        }
                    }, 1);
                }
            }
        }, 200);


        binding.buttonBackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });

//        Request request = CommonRequest.Create(new RequestBuilder<String>() {
//            {
//                url("https://www.baidu.com");//??????url?????????????????????????????????
//                addParam("wd", "glide");//??????????????????
//                json("{}");//?????????????????????json??????post?????????
//                addHeader("name", "mark");//???????????????header
//            }
//        }).error(error -> {
//            Log.e("????????????", "code=" + error.getCode());
//        }).map(new Function<String, Bean>() {
//            @Override
//            public Bean apply(String s) {
//                return new Bean(s);
//            }
//        }).subscribe(s -> {
//            Log.e("????????????", "data=" + s);
//        }).POST();
//
//        RequestBuilder<Bean> requestBuilder = new RequestBuilder<Bean>("url") {
//            {
//                url("url");// ???????????????url ?????????????????????url ???????????????????????????
//                addParam("key", "value");
//                addHeader("header", "value");
//                json("{}");//???POST???????????????json?????????POST()?????????
//            }
//        };

//        Request request = CommonRequest.Create(requestBuilder)
//                .error(error -> {
//                    error.printStackTrace();
//                    Log.e("????????????", "code=" + error.getCode() + "   msg=" + error.getMessage());
//                }).subscribe(bean -> {
//                    Log.i("????????????", "data=" + bean.str);
//                }).GET();
//
//        request.cancel();
//
//        CoroutineLRZContext.INSTANCE.create(new Task<String>() {
//            @Override
//            public String submit() {
//                return "??????????????????task ??????????????????????????????";
//            }
//        }).subscribe(Dispatcher.IO, str -> {
//            //???io?????????????????????
//            Log.i("CoroutineLRZContext", str);
//        }).map(str -> {
//            // ??????????????????????????????????????????????????????subscribe??????
//            Log.i("CoroutineLRZContext", str);
//            return new Bean(str);
//        }).subscribe(Dispatcher.MAIN, bean -> {
//            // ????????????????????????????????????bean?????????map???????????????Bean?????????
//            Log.i("CoroutineLRZContext", bean.str);
//        }).error(throwable -> {
//            //????????????????????????????????????????????????????????????????????????????????????
//        }).execute(Dispatcher.BACKGROUND);//????????????????????????????????????
//
//        // ?????????create??????????????????
//        CoroutineLRZContext.INSTANCE.create(new Task<String>() {
//            @Override
//            public String submit() {
//                return "??????????????????task ??????????????????????????????";
//            }
//        }).subscribe(Dispatcher.IO, str -> {//??????????????????????????????
//            //???io?????????????????????
//            Log.i("CoroutineLRZContext",str);
//        }).execute(Dispatcher.BACKGROUND);//????????????????????????????????????
//
//
//        CoroutineLRZContext.Create(new Task<String>() {
//            @Override
//            public String submit() {
//                return "??????????????????task ??????????????????????????????";
//            }
//        }).subscribe(Dispatcher.IO, str -> {
//            Log.i("Coroutine", str);
//        }).map().subscribe(bean -> { //??????????????????
//            Log.i("Coroutine", bean);
//        }).execute(Dispatcher.BACKGROUND);//????????????????????????????????????

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