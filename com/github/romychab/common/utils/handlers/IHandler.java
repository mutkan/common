package com.github.romychab.common.utils.handlers;


public interface IHandler {

    void post(Runnable runnable);

    void postDelayed(long millis, Runnable runnable);

    void cancel(Runnable runnable);

    void postDelayed(long millis, Runnable runnable, Object token);

    void cancel(Object token);

}
