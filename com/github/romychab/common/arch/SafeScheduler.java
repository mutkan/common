package com.github.romychab.common.arch;

import com.github.romychab.common.utils.handlers.IHandler;

import java.util.concurrent.TimeUnit;

import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.plugins.RxJavaPlugins;


public class SafeScheduler extends Scheduler {

    private final IHandler handler;

    SafeScheduler(IHandler handler) {
        this.handler = handler;
    }

    @Override
    public Disposable scheduleDirect(Runnable run, long delay, TimeUnit unit) {
        if (run == null) throw new NullPointerException("run == null");
        if (unit == null) throw new NullPointerException("unit == null");

        run = RxJavaPlugins.onSchedule(run);
        ScheduledRunnable scheduled = new ScheduledRunnable(handler, run);
        handler.postDelayed(Math.max(0L, unit.toMillis(delay)), scheduled);
        return scheduled;
    }

    @Override
    public Worker createWorker() {
        return new SafeWorker(handler);
    }

    private static final class SafeWorker extends Worker {
        private final IHandler handler;

        private volatile boolean disposed;

        SafeWorker(IHandler handler) {
            this.handler = handler;
        }

        @Override
        public Disposable schedule(Runnable run, long delay, TimeUnit unit) {
            if (run == null) throw new NullPointerException("run == null");
            if (unit == null) throw new NullPointerException("unit == null");

            if (disposed) {
                return Disposables.disposed();
            }

            Runnable targetRunnable = RxJavaPlugins.onSchedule(run);

            ScheduledRunnable scheduled = new ScheduledRunnable(handler, targetRunnable);

            delay = Math.max(0L, unit.toMillis(delay));
            handler.postDelayed(delay, scheduled, this);

            // Re-check disposed state for removing in case we were racing a call to dispose().
            if (disposed) {
                handler.cancel(scheduled);
                return Disposables.disposed();
            }

            return scheduled;
        }

        @Override
        public void dispose() {
            disposed = true;
            handler.cancel(this);
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }
    }

    private static final class ScheduledRunnable implements Runnable, Disposable {
        private final IHandler handler;
        private final Runnable delegate;

        private volatile boolean disposed;

        ScheduledRunnable(IHandler handler, Runnable delegate) {
            this.handler = handler;
            this.delegate = delegate;
        }

        @Override
        public void run() {
            try {
                delegate.run();
            } catch (Throwable t) {
                IllegalStateException ie =
                        new IllegalStateException("Fatal Exception thrown on Scheduler.", t);
                RxJavaPlugins.onError(ie);
                Thread thread = Thread.currentThread();
                thread.getUncaughtExceptionHandler().uncaughtException(thread, ie);
            }
        }

        @Override
        public void dispose() {
            disposed = true;
            handler.cancel(this);
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }
    }
}
