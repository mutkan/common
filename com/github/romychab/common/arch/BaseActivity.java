package com.github.romychab.common.arch;

import android.os.Bundle;
import android.widget.Toast;

import com.arellomobile.mvp.MvpAppCompatActivity;
import com.github.romychab.common.R;
import com.github.romychab.common.arch.IBaseView.ProgressAction;
import com.github.romychab.common.arch.IBaseView.ProgressType;
import com.github.romychab.common.dialogs.ProgressDialogFragment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public abstract class BaseActivity
    extends MvpAppCompatActivity
    implements
        ProgressDialogFragment.IProgressCallbacks,
        IDefaultProgressDialogHolder {

    private BaseActivityDelegate mDelegate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDelegate = new BaseActivityDelegate(this, getSupportFragmentManager(), registerPresenters());
    }

    @Override
    public void registerProgressCallback(ProgressDialogFragment.IProgressCallbacks callbacks) {
        mDelegate.registerProgressCallback(callbacks);
    }

    @Override
    public void unregisterProgressCallback(ProgressDialogFragment.IProgressCallbacks callbacks) {
        mDelegate.unregisterProgressCallback(callbacks);
    }

    @Override
    protected void onDestroy() {
        mDelegate.onDestroy();
        super.onDestroy();
    }

    public void onError(Throwable error) {
        mDelegate.onError(error);
    }

    public void setProgress(ProgressAction action, ProgressType progressType) {
        mDelegate.setProgress(action, progressType);
    }

    public void hideAllProgresses() {
        mDelegate.hideAllProgresses();
    }

    @Override
    public void onProgressCancelled(String progressTag) {
        mDelegate.onProgressCancelled(progressTag);
    }

    protected abstract BasePresenter[] registerPresenters();

}
