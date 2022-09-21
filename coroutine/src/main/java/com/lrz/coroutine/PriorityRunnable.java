package com.lrz.coroutine;
public abstract class PriorityRunnable implements Runnable {

    private final Priority priority;

    public PriorityRunnable(Priority priority) {
        this.priority = priority;
    }

    public Priority getPriority() {
        return priority;
    }

}