package com.lrz.coroutine;

import android.util.Log;

/**
 * Author:  liurongzhi
 * CreateTime:  2022/9/21
 * Description:
 */
public class LLog {
    public static byte logLevel = 0;
    public static final byte INFO = 0;
    public static final byte DEBUG = 1;
    public static final byte WARN = 2;
    public static final byte ERROR = 3;

    static {
        if (BuildConfig.DEBUG) {
            logLevel = INFO;
        }
    }

    public static void i(String tag, String log) {
        if (tag == null || log == null)
            return;
        if (logLevel <= INFO) {
            Log.i(tag, log);
        }
    }

    public static void i(String tag, String log, Throwable e) {
        if (tag == null || log == null)
            return;
        if (logLevel <= INFO) {
            Log.i(tag, log, e);
        }
    }

    public static void d(String tag, String log) {
        if (tag == null || log == null)
            return;
        if (logLevel <= DEBUG) {
            Log.d(tag, log);
        }
    }

    public static void d(String tag, String log, Throwable e) {
        if (tag == null || log == null)
            return;
        if (logLevel <= DEBUG) {
            Log.d(tag, log, e);
        }
    }

    public static void e(String tag, String log) {
        if (tag == null || log == null)
            return;
        if (logLevel <= ERROR) {
            Log.e(tag, log);
        }
    }

    public static void e(String tag, String log, Throwable e) {
        if (tag == null || log == null)
            return;
        if (logLevel <= ERROR) {
            Log.e(tag, log, e);
        }
    }

    public static void w(String tag, String log) {
        if (tag == null || log == null)
            return;
        if (logLevel <= WARN) {
            Log.w(tag, log);
        }
    }

    public static void w(String tag, String log, Throwable e) {
        if (tag == null || log == null)
            return;
        if (logLevel <= WARN) {
            Log.w(tag, log, e);
        }
    }
}
