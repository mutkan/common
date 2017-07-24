package com.github.romychab.common.utils.handlers;

import android.os.Handler;
import android.os.Message;

public class UiHandler implements IHandler {
    private Handler mHandler;

    public UiHandler(Handler handler) {
        mHandler = handler;
    }

    @Override
    public void post(Runnable runnable) {
        mHandler.post(runnable);
    }

    @Override
    public void postDelayed(long millis, Runnable runnable) {
        mHandler.postDelayed(runnable, millis);
    }

    @Override
    public void cancel(Runnable runnable) {
        mHandler.removeCallbacks(runnable);
    }

    @Override
    public void postDelayed(long millis, Runnable runnable, Object token) {
        Message message = Message.obtain(mHandler, runnable);
        message.obj = this; // Used as token for batch disposal of this worker's runnables.
        mHandler.sendMessageDelayed(message, millis);
    }

    @Override
    public void cancel(Object token) {
        mHandler.removeCallbacksAndMessages(token);
    }
}
