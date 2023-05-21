package com.lrz.coroutine.flow;

import com.lrz.coroutine.Dispatcher;
import com.lrz.coroutine.handler.CoroutineLRZContext;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author:  liurongzhi
 * CreateTime:  2023/1/29
 * Description: 多个事件流集合
 * <p>
 * 重构 2023/5/21
 * 1.事件集的出现目的是什么
 * 	事件集是为了管理多个事件流的执行情况，观察多个事件流是否完成，和完成情况
 * 2.为什么事件集在修改了多个版本后，仍然达不到预期
 * 	我只想到了多个事件流并发的情况，忽视了事件流之间的关系？无法兼容多种场景，而事件流之间的相互关系，影响到了事件集对什么时候是完成，什么时候是错误的定义出现了问题
 * 3.事件集的已知使用场景，分析这个问题需要从事件流之间的关系入手，事件流之间的关系分以下两种
 *
 * 			-->  success（1）--> onSubscribe ->		    ｜
 * 	ob1----|				                    1 or 0	｜
 * 			--> execption（0--> onError    -->		    ｜
 *                                                      ｜
 * 									                    ｜	        ---> onSubscribe
 * 			-->  success（1）  --> onSubscribe ->		｜	        ｜
 * ob2----|				                         1 or 0	｜---set--->
 * 			--> execption（0） --> onError    -->		｜	        ｜
 * 									                    ｜	         ---> onError
 * 									                    ｜
 * 			-->  success（1）  --> onSubscribe ->		｜
 * ob3----|				                         1 or 0	｜
 * 			--> execption（0） --> onError    -->		｜
 *
 * 		1.and，多个事件流均完成且无错误，则事件集执行完毕，如果某一个事件流出现错误，则事件集执行失败，未完成的流需要关闭，事件集只执行error
 * 			每个流执行的结果都有成功和失败 也就是 1或0，当所有事件都是1，则set结果是1，即 1&1&1=1，有一个执行失败，则set是0即 0&1&1=0
 * 			公式 集的结果 y = x1&x2&x3
 *
 * 			举例：更新用户信息需要request两个接口，一个返回用户基本信息，一个返回用户vip信息，则两个流执行结果都是1.则集的结果是1，否则，是0
 *
 *
 * 		2.or，多个事件流的执行只要有一个成功，则集的执行就算成功，即 集的结果 y = x1|x2|x3,只有x都是0，y才是0
 * 			举例：在获取不同地址的相同资源，只要有一个商返回，则本次的请求就算成功
 * 		</p>
 */
public class ObservableSet extends Observable<Integer> {
    Observable<?>[] observables;
    //完成多少个
    AtomicInteger count = new AtomicInteger();
    //成功多少个
    AtomicInteger successNum = new AtomicInteger();
    //执行模式，1 and，2 or
    private int setMode = 1;
    private volatile boolean closeOnComplete = true;

    protected ObservableSet() {
    }

    ObservableSet(Observable<?>[] observables) {
        this.observables = observables;
    }

    public static @NotNull
    ObservableSet CreateAnd(Observable<?>... observable) {
        ObservableSet set = create(observable);
        set.setMode = 1;
        return set;
    }

    public static @NotNull
    ObservableSet CreateOr(Observable<?>... observable) {
        ObservableSet set = create(observable);
        set.setMode = 2;
        return set;
    }

    public static @NotNull
    ObservableSet CreateOr(boolean closeOnComplete, Observable<?>... observable) {
        ObservableSet set = create(observable);
        set.setMode = 2;
        set.closeOnComplete = closeOnComplete;
        return set;
    }

    private static @NotNull
    ObservableSet create(Observable<?>... observable) {
        ObservableSet set = new ObservableSet(observable);
        if (set.observables != null && set.observables.length > 0) {
            for (Observable<?> ob : set.observables) {
                ob.subscribe(o -> set.onComplete(null, ob));
            }
        }
        return set;
    }

    //标记是否有错误发生
    private synchronized void onComplete(Throwable throwable, Observable<?> observable) {
        count.incrementAndGet();
        if (throwable == null) successNum.incrementAndGet();
        if (setMode == 1) {
            if (observables != null && count.get() >= observables.length) {
                //都完成了,且没有发生错误,本轮success个数
                onSubscribe(successNum.get());
            }
        } else if (setMode == 2) {
            if (observables != null && count.get() == 1) {
                //在or模式下 只要有一个成功了，则代表本次事件集成功
                onSubscribe(successNum.get());
                //完成后，是否需要关闭其他流
                if (closeOnComplete) {
                    for (Observable<?> ob : observables) {
                        if (ob != observable) ob.cancel();
                    }
                }
            }
        }
    }

    /**
     * 复写父类，不处理线程回调，只接收多任务结束事件
     *
     */
    @Override
    protected void onSubscribe(Integer num) {
        //拦截task的调用，这里只允许内部手动调用
        if (num >= 0) {
            super.onSubscribe(num);
        }
    }

    @Override
    protected synchronized Task<?> getTask() {
        Observable<?> pre = preObservable;
        if (pre != null) {
            return pre.getTask();
        } else if (task == null) {
            task = new Task<Integer>() {
                @Override
                public Integer submit() {
                    doObservables();
                    return -1;
                }
            };
            task.setObservable(this);
        }
        return task;
    }

    /**
     * 执行多个任务
     */
    private synchronized void doObservables() {
        if (observables != null && observables.length > 0) {
            for (Observable<?> ob : observables) {
                proxyError(ob);
                ob.execute();
            }
        }
    }

    @Override
    public synchronized <F> Observable<F> map(Function<Integer, F> function) {
        return super.map(function);
    }

    /**
     * 当前线程，立即执行所有任务
     *
     */
    @Override
    public synchronized Observable<Integer> execute() {
        Dispatcher dispatcher = getTaskDispatch();
        if (dispatcher == null) {
            thread(Dispatcher.MAIN);
        }
        return super.execute();
    }

    private void proxyError(Observable<?> ob) {
        IError<?> oldError = ob.getError();
        Dispatcher dispatcher = ob.getErrorDispatcher();
        if (dispatcher == null) dispatcher = ob.getDispatcher();
        ob.error(dispatcher, new InnerError(ob, oldError, dispatcher, this));
    }

    static class InnerError implements IError<Throwable> {
        //原来的error
        private final IError<?> error;
        private final Dispatcher oldDispatch;
        private final ObservableSet observableSet;
        final Observable<?> ob;

        InnerError(Observable<?> ob, IError<?> error, Dispatcher oldDispatch, ObservableSet observableSet) {
            this.error = error;
            this.oldDispatch = oldDispatch;
            this.observableSet = observableSet;
            this.ob = ob;
        }

        @Override
        public void onError(Throwable throwable) {
            IError error = this.error;
            //先把流的错误发送出去
            if (error != null) {
                if (oldDispatch != null) {
                    CoroutineLRZContext.INSTANCE.execute(oldDispatch, () -> {
                        error.onError(throwable);
                    });
                } else {
                    error.onError(throwable);
                }
            } else {
                // 如果流没有处理error事件，则交给set处理
                observableSet.onError(new CoroutineFlowException("stream has an error,and did not deal it self", throwable));
            }
            //再把集的错误发送出去
            observableSet.onComplete(throwable, ob);
        }
    }

    @Override
    public synchronized void cancel() {
        if (observables != null && observables.length > 0) {
            for (Observable<?> ob : observables) {
                ob.cancel();
            }
            observables = null;
        }
        super.cancel();
    }
}
