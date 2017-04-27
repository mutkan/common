package com.github.romychab.common.locations;


import java.util.LinkedHashSet;
import java.util.Set;

public class PostCall {
    protected Set<Runnable> mPostCalls = new LinkedHashSet<>();
    protected boolean mActive = false;

    public PostCall() {

    }

    public PostCall call(Runnable runnable) {
        if (mActive) {
            runnable.run();
        }
        else {
            mPostCalls.add(runnable);
        }
        return this;
    }

    public void activate() {
        mActive = true;
        for (Runnable runnable : mPostCalls) {
            runnable.run();
        }
        mPostCalls.clear();
    }

    public void deactivate() {
        mActive = false;
    }

    public void clear() {
        mPostCalls.clear();
    }
}
