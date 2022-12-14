package com.lrz.coroutine.handler;

import java.io.Closeable;
import java.lang.ref.WeakReference;

/**
 * Author And Date: liurongzhi on 2020/12/7.
 * Description: com.yilan.sdk.common.executor.handler
 */
public class Job implements Closeable {
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
        LJob lJob;
        if (job != null && (lJob = job.get()) != null) {
            if (lJob.getRunnableHash() == jobHash && jobHash != 0x000000) {
                lJob.cancel();
                job = null;
            }
        }
        if (next != null) {
            next.cancel();
            next = null;
        }
    }

    @Override
    public void close() {
        cancel();
    }
}
