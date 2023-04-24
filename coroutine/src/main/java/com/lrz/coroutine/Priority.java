package com.lrz.coroutine;

public class Priority implements Comparable<Priority> {
    public final int value;

    public Priority(int value) {
        this.value = value;
    }

    /**
     * Lowest priority level. Used for prefetches of data.
     */
    public static final Priority LOW = new Priority(0);

    /**
     * Medium priority level. Used for warming of data that might soon get visible.
     */
    public static final Priority MEDIUM = new Priority(1);

    /**
     * Highest priority level. Used for data that are currently visible on screen.
     */
    public static final Priority HIGH = new Priority(2);

    /**
     * Highest priority level. Used for data that are required instantly(mainly for emergency).
     */
    public static final Priority IMMEDIATE = new Priority(3);

    @Override
    public int compareTo(Priority o) {
        if (o == null) return -1;
        return this.value - o.value;
    }
}