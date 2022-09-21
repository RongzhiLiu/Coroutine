package com.lrz.coroutine;

import android.util.Log;

/**
 * Author:  liurongzhi
 * CreateTime:  2022/9/21
 * Description:
 */
public class LLog {
    public static boolean DEBUG = false;

    static {
        if (BuildConfig.DEBUG) {
            DEBUG = true;
        }
    }

    public static void i(String tag, String log) {
        if (tag == null || log == null)
            return;
        if (DEBUG) {
            Log.i(tag, log);
        }
    }

    public static void i(String tag, String log, Throwable e) {
        if (tag == null || log == null)
            return;
        if (DEBUG) {
            Log.i(tag, log);
            e.printStackTrace();
        }
    }

    public static void d(String tag, String log) {
        if (tag == null || log == null)
            return;
        if (DEBUG) {
            Log.d(tag, log);
        }
    }

    public static void d(String tag, String log, Throwable e) {
        if (tag == null || log == null)
            return;
        if (DEBUG) {
            Log.d(tag, log);
            e.printStackTrace();
        }
    }

    public static void e(String tag, String log) {
        if (tag == null || log == null)
            return;
        if (DEBUG) {
            Log.e(tag, log);
        }
    }

    public static void e(String tag, String log, Throwable e) {
        if (tag == null || log == null)
            return;
        if (DEBUG) {
            Log.e(tag, log);
            e.printStackTrace();
        }
    }

    public static void w(String tag, String log) {
        if (tag == null || log == null)
            return;
        if (DEBUG) {
            Log.w(tag, log);
        }
    }
}
