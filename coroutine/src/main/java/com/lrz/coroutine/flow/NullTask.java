package com.lrz.coroutine.flow;

/**
 * Author:  liurongzhi
 * CreateTime:  2023/5/21
 * Description: 空任务
 */
public class NullTask extends Task<Void> {
    @Override
    public Void submit() {
        return null;
    }
}
