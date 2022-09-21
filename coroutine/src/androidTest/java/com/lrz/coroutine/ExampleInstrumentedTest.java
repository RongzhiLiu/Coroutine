package com.lrz.coroutine;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import com.lrz.coroutine.handler.CoroutineLRZContext;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.lrz.coroutine.test", appContext.getPackageName());
        for (int i = 0;i<10;i++) {
            CoroutineLRZContext.INSTANCE.execute(Dispatcher.IO, new Runnable() {
                @Override
                public void run() {

                }
            });
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (int i = 0;i<10;i++) {
            CoroutineLRZContext.INSTANCE.execute(Dispatcher.BACKGROUND, new Runnable() {
                @Override
                public void run() {

                }
            });
        }
    }
}