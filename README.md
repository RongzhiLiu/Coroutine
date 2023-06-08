# Coroutine -Android 高性能线程池框架-事件流-http请求
​		**为什么会有这个一个框架，起源于18年开始使用kotlin和协程，虽然java 没有办法实现其类似的效果，所以暂且称呼为伪协程，但是我们可以追求让异步任务更加优雅，让事件流向更加明确，让线程池性能更加可控，在移动平台有限的资源下，将有限的线程资源压榨到极致。**

​		**后来不断的在业务和框架之间切换，在线程池的基础上，增加了事件流的控制，对网络请求这种特别的异步操作，增加了对okhttp 的支持，他比retrofit更加轻量，使用更加优雅。欢迎大家提出宝贵意见**

#### 一.导入库
```java
// 在project下的build.gradle中添加如下maven地址
maven { url 'https://jitpack.io' }
```
```java
implementation 'com.github.RongzhiLiu:Coroutine:1.2.8.18'
// 如果需要使用 http 请求功能，请添加以下依赖
implementation "com.squareup.okhttp3:okhttp:4.10.0"
implementation 'com.google.code.gson:gson:2.8.5'
```

#### 二.功能和有点

​	Java 线程池的缺点：

​		1.在提交新任务时，如果核心线程数没有达到最大，则会继续创建新的线程，而不是查看是否有空闲的线程，

​			在移动平台上，应尽可能利用已有资源，而不是新建线程，华为手机已经开始限制对线程的创建，导致应用崩溃

​		2.异步任务不可控，无法取消

​		3.异步任务出错，无法定位，无法看到发起位置的栈结构

​		4.线程的阻塞依赖起内部任务队列，性能较低

**解决以上痛点，于是有了这么一款框架,**

​	**1.线程池内部的线程采用HandlerThread来实现，epoll机制**

​	**2.采用一个线程池多种线程，可根据业务来定制异步任务的使用**

​	**3.一个线程池，统一管理所有线程，搞定所有不同场景的所有任务类型，如定时任务，队列任务，优先级任务，延迟任务等**

​	**4.独有的任务窃取机制，在有限线程数量下，将线程性能压榨到极致**

​	**5.事件流，优雅的实现http请求**

#### 三.异步的简单使用:

```java
//日志开关,正式环境可将其设置为ERROR，调试时设置为INFO或DEBUG，（INFO级别的日志，会输出任务执行耗时，线上环境请设置为ERROR）
LLog.logLevel = LLog.INFO;//LLog.DEBUG,LLog.WARN,LLog.ERROR
        
// 发起异步任务
Job job = CoroutineLRZContext.Execute(Dispatcher.IO, new Runnable() {
            @Override
            public void run() {
                
            }
        });
// 发起优先级任务
Job job2 = CoroutineLRZContext.Execute(Dispatcher.IO, new PriorityRunnable(Priority.HIGH) {
            @Override
            public void run() {

            }
        });


//取消任务（如果任务还没有开始执行）
job.cancel();

//更多方法见下文
```

##### 3.1Dispatcher 有三种类型

​	1.Dispatcher.MAIN：在主线程中执行任务

​	2.Dispatcher.IO：在子线程中执行

​	3.Dispatcher.BACKGROUND：在后台线程中执行

**Dispatcher.IO核心线程最大数量是cpu number的80%，非核心线程是核心线程的一半，超过10秒则释放，也可以自定义超时时间** 

**Dispatcher.BACKGROUND：后来线程最大为cpu number/4，最小是1，无非核心线程**

##### 3.1任务窃取机制

IO类型的线程和BACKGROUND线程会相互窃取对方的任务执行，以保证在有效资源下的最大并发

1.当IO核心线程满载时，且任务队列中已经积压了一定数量的任务等待执行，则BACKGROUND线程在空闲的情况下会窃取任务，协助IO线程执行任务，反之亦然

##### 3.2 函数简介：

```java
 /**
     * 启动异步任务
     *
     * @param dispatcher 任务调度器类型
     * @param runnable   任务
     * @return job
     */
    Job Execute(Dispatcher dispatcher, Runnable runnable);

    /**
     * 启动周期任务
     *
     * @param dispatcher 任务调度器类型
     * @param runnable   任务
     * @param spaceTime  周期时间
     * @return job
     */
    Job ExecuteTime(Dispatcher dispatcher, Runnable runnable, long spaceTime);

    /**
     * 启动延时任务
     *
     * @param dispatcher 任务调度器
     * @param runnable   任务
     * @param delayTime  延时时间
     * @return job
     */

    Job ExecuteDelay(Dispatcher dispatcher, Runnable runnable, long delayTime);

    /**
     * 同时执行多个job，不保证先后顺序,
     * @param dispatcher 任务调度器
     * @param runnable   任务
     * @return job 返回第一个job，调用job.cancel(),会取消所有袭相关任务任务
     */
    Job ExecuteJobs(Dispatcher dispatcher, Runnable... runnable);

    /**
     * 清除所有任务
     */
    void Clear();


    /**
     * 设置非核心线程最大空闲时间，默认值10000毫秒
     * @param time 毫秒
     */
    void SetKeepTime(long time);

	/** 
     * 设置非核心线程最大数量，非核心线程在空闲KeepTime后将结束(1.0.7版本新增)
     * 一般用于突发性的并发任务，移动平台该数量不应设置过大
     * @param count 非核心线程最大数量，默认非核心线程数量是核心线程数的一半
     */
    void setElasticCount(int count);

    /**
     * 设置是否开启额外调用堆栈,开启后，若异步线程发生异常，可查看任务发起时的堆栈（线上环境请关闭此开关）
     * @param enable
     */
     void setStackTraceExtraEnable(boolean enable);
```

#### 四.高级用法一

##### 4.1事件流的使用

类似rxjava，却区别于rxjava，因为rxjava更加突出多个事件的事件流，而此框架更加突出单个事件的流向，一对n的消费模型，更加强调事件生产者和多个消费者的关系，让同一个结果在不同的消费者模型中做不同的处理，让不同的订阅者扮演任务链中的不同角色

![时序图](https://github.com/RongzhiLiu/Coroutine/blob/master/liuchengtu.jpg)

```java
		/**方法介绍
     * 通过task 创建可以被订阅的Observable
     * @param task 任务
     * @param <T> 范型及task任务的返回值类型
     * @return
     */
    <T> Observable<T> create(Task<T> task);

		/**
     * 设置订阅者
     * 如果用此方法，没有指定订阅者线程，则其会在默认线程中（上一个切换的线程中执行订阅回调，下面会详细讲到）
     * @param result 任务结果回调
     * @return 任务描述
     */
		Observable<T> subscribe(Observer<T> result)
      
    /**
     * 设置订阅者，并指定订阅者所在线程
     *
     * @param dispatcher 线程
     * @param result     回调
     * @return 任务描述
     */
    Observable<T> subscribe(Dispatcher dispatcher, Observer<T> result)
    
    /**
     * 设置错误订阅
     * 注意：如果不设置错误捕获订阅，则在事件流中发生的任务错误都会被抛出，可根据自身业务来选择
     * @param error 回调接口
     * @return 任务描述
     */
    Observable<T> error(IError error)
   /**
     * 设置错误订阅 并设置订阅线程，如果没有指定，则在默认线程中回调
     *
     * @param dispatcher 回调线程
     * @param error      回调接口
     * @return 任务描述
     */
    Observable<T> error(Dispatcher dispatcher, IError error)
      
    /**
     * 按照一定规则转换事件类型，并生成一个新的可被订阅者Observable
     * 此函数用于 在一个生产事件，多个订阅者的情况下
     * @param function 转换函数
     * @param <F>      转换的类型
     * @return 可被订阅的Observable
     */
    <F> Observable<F> map(Function<T, F> function)
      
    /**
     * 设置任务延迟时间
     *
     * @param delay 任务延迟时间
     */
    Observable<T> delay(long delay)
      
    /**
     * 设置任务循环时间间隔
     *
     * @param interval 任务循环时间
     */
    Observable<T> interval(long interval)
      
    /**
     * 在当前线程执行，该线程可能是thread()设置的，如果是null，则不执行
     */
    Observable<T> execute()
      
    /**
     * 在指定线程执行任务
     */  
    Observable<T> execute(Dispatcher dispatcher)
      
    /**
     * 在指定线程执行延迟任务
     */  
    Observable<T> executeDelay(Dispatcher dispatcher, long delay)  
      
    /**
     * 在指定线程执行循环任务
     */ 
    Observable<T> executeTime(Dispatcher dispatcher, long interval)  
    /**
     * 取消任务执行
     */  
    void cancel()
```

##### 4.2举例1:一个生产事件，一个订阅的消费者

```java
// 先通过create函数创建任务
  Observable<T> observable = CoroutineLRZContext.Create(new Task<String>() {
      @Override
      public String submit() {
           return "任务结果，由task 的范型来限定返回类型";
      }
  }).subscribe(Dispatcher.IO, str -> {//在指定线程中接收结果
     //在io线程中接受结果
     Log.i("CoroutineLRZContext",str);
  }).execute(Dispatcher.BACKGROUND);//开始执行任务，并指定线程


// 取消任务
observable.cancel();

```

##### 4.3举例2:一个生产事件，多个订阅的消费者

```java
	CoroutineLRZContext.Create(new Task<String>() {
	    @Override
	    public String submit() {
	        return "任务结果，由task 的范型来限定返回类型";
	    }
	}).subscribe(Dispatcher.IO, str -> {
	    //在io线程中接受结果
	    Log.i("CoroutineLRZContext",str);
	}).map(str -> {
	    // 将结果转换为另一种类型，并交给下一个subscribe处理
	    Log.i("CoroutineLRZContext",str);
	    return new Bean(str);
	}).subscribe(Dispatcher.MAIN, bean -> {
	    // 在主线程中接受结果，此时bean已经被map函数转换为Bean类型了
	    Log.i("CoroutineLRZContext",bean.str);
	}).error(throwable -> {
	    //捕获一系列事件流处理过程中的异常，如果不设置，则抛出异常
	}).execute(Dispatcher.BACKGROUND);//开始执行任务，并指定线程


	// 注意：多订阅者需要map 函数支持，也就是说，每多一个订阅者，前面必须有一个map函数转换
```

#### 五.默认线程问题

##### 	5.1举例1 多个订阅者时，有未指定线程的订阅者

```java
// 每一个表达式都可以指定线程，如果不指定，则按照最近原则使用上面最近的线程	
CoroutineLRZContext.Create(new Task<String>() {
	    @Override
	    public String submit() {
	        return "任务结果，由task 的范型来限定返回类型";
	    }
	}).subscribe(str -> {//第一个订阅者
	    Log.i("Coroutine",str);
	}).map().subscribe(bean -> { //第二个订阅者
	    Log.i("Coroutine",bean);
	}).error(Dispatcher.MAIN,error -> {
      Log.e("Coroutine","error",error);
  }).execute(Dispatcher.BACKGROUND);//开始执行任务，并指定线程

// 上面例子可以看到，第一个订阅者没有指定线程，那么其默认会跟随上一个切换的线程，但是他上面没有别的订阅者改变线程，则其跟随生产者线
// 程，即Dispatcher.BACKGROUND
// 第二个订阅者没有指定线程，往上推，第一个订阅者的线程是BACKGROUND，那么他的订阅线程也是BACKGROUND
// error表达式的线程也是同理（向上靠近原则）


CoroutineLRZContext.Create(new Task<String>() {
	    @Override
	    public String submit() {
	        return "任务结果，由task 的范型来限定返回类型";
	    }
	}).subscribe(Dispatcher.IO, str -> {//第一个订阅者
	    Log.i("Coroutine",str);
	}).map().subscribe(bean -> { //第二个订阅者
	    Log.i("Coroutine",bean);
	}).execute(Dispatcher.BACKGROUND);//开始执行任务，并指定线程
// 上面例子，如果我们把第一个订阅者线程指定为IO，第二个订阅者将会跟随第一个订阅者线程，也就是io
```

那么通过上面的例子，举一反三，error 的订阅线程 在不指定的情况下也采取就近原则，如果所有Observable都没有指定线程，则默认使用execute/thread所设置的线程。

#### 六.高级用法二 如何优雅的发起http请求

在事件流的架构下，该框架可以极大减少http请求代码量，让控制链更加清晰

##### 	6.1发起一条完整的网络请求

```java
// 发起网络请求，范型表示网络返回后解析的bean，如果不需要json解析则写string 如下	
Request request = CommonRequest.Create(new RequestBuilder<String>("https://www.baidu.com") {
	    {
	        addParam("wd", "xxx");
	        addParam("wk", "xxx");
	    }
	}).error(error -> {//error 是一个exception类型，其code 是失败的http code
	    error.printStackTrace();
	    Log.e("请求错误", "code=" + error.getCode()+ "   msg="+error.getMessage());
	}).subscribe(str -> {
	    Log.i("请求成功", "data=" + str);
	}).GET();// 发起get请求
	// 请求可以取消
	request.cancel();
```

##### 	6.2RequestBuilder 详解

​	RequestBuilder：请求构造器

```java
RequestBuilder<Bean> requestBuilder = new RequestBuilder<Bean>("url"){
    {
        url("url");// 代码块里的url 和构造函数中的url 选一即可，不用都写
        addParam("key","value");
        addHeader("header","value");
        json("{}");//在POST请求时上传json，只在POST()时有效
    }
};
```

##### 	6.3发起请求

```java
// 区别于事件流 在error 和 subscribe 不指定线程的情况下，默认 是 MAIN线程，其他多订阅者等用法和事件流相同
ReqObservable<Bean> request = CommonRequest.Create(requestBuilder)
		.error(error -> {
		    error.printStackTrace();
		    Log.e("请求错误", "code=" + error.getCode() + "   msg=" + error.getMessage());
		}).subscribe(bean -> {
		    Log.i("请求成功", "data=" + bean.str);
		}).GET();

```

#### 七.并行事件流（多事件流）

在很多业务场景下，会有多个异步任务并行，并统一监测执行结果

```java
// 创建第一个事件流
Observable<String> observable1 = CoroutineLRZContext.Create(new Task<String>() {
    @Override
    public String submit() {
        return "";
    }
}).subscribe(Dispatcher.IO, str -> {
    Log.i("Coroutine",str);
}).error(error -> {
    Log.e("Coroutine","error",error);
}).thread(Dispatcher.BACKGROUND)//通过thread()指定执行线程
  .delay(1000);//如果需要延迟执行，则设置延迟时间
//注意不要调用execute();

//创建第二个事件流
ReqObservable<String> observable2 = CommonRequest.Create(new RequestBuilder<String>() {
    {
        url("https://baidu.com");
    }
}).subscribe(s -> {
    Log.i("Coroutine", s);
}).error(e -> {
    Log.e("Coroutine", "error", e);
}).method(Method.GET);//指定网络get请求

// 将两个事件流通过ObservableSet.with方法组合起来，并调用execute()
// 当两个事件流均成功执行完成，则会回调到ObservableSet 的subscribe中
// 如果两个事件流中，有一个发生error，或者中断，则会取消set中所有的事件流执行
ObservableSet.with(observable, observable2, observable3).subscribe(Dispatcher.BACKGROUND, aBoolean -> {
    Log.i("Coroutine", aBoolean);
}).error(Dispatcher.MAIN, error -> {
    Log.e("Coroutine", "error", e);
}).execute();
```

