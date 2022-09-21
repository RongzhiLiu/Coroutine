package com.lrz.coroutine.handler;

import java.lang.ref.WeakReference;

/**
 * Author And Date: liurongzhi on 2020/12/7.
 * Description: com.yilan.sdk.common.executor.handler
 */
public class Job {
    Job next;
    private WeakReference<LJob> job;
    private int jobHash = 0x000000;

    public Job(LJob job) {
        this.job = new WeakReference<>(job);
        if (job != null) {
            jobHash = job.getRunnableHash();
        }
    }

    public void cancel() {
        if (job != null && job.get() != null && job.get().getRunnableHash() == jobHash && jobHash != 0x000000) {
            job.get().cancel();
            job = null;
        }

        if (next != null) {
            next.cancel();
            next = null;
        }
    }
}
