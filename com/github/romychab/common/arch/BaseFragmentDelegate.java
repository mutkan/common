package com.github.romychab.common.arch;

import android.app.Activity;
import android.content.Context;

import com.github.romychab.common.dialogs.ProgressDialogFragment;

public class BaseFragmentDelegate
    implements
        ProgressDialogFragment.IProgressCallbacks {

    private IBaseView mActivityView;
    private IDefaultProgressDialogHolder mProgressDialogHolder;

    private BasePresenter[] mPresenters;

    public BaseFragmentDelegate(IBaseView activityView, IDefaultProgressDialogHolder dialogHolder, BasePresenter[] presenters) {
        mActivityView = activityView;
        mProgressDialogHolder = dialogHolder;
        mPresenters = presenters;
    }

    public void onAttach(Context context) {
        mProgressDialogHolder.registerProgressCallback(this);
    }

    public void onDetach() {
        mProgressDialogHolder.unregisterProgressCallback(this);
    }

    public void onError(Throwable error) {
        mActivityView.onError(error);
    }

    public void setProgress(IBaseView.ProgressAction action, IBaseView.ProgressType progressType) {
        mActivityView.setProgress(action, progressType);
    }

    public void hideAllProgresses() {
        mActivityView.hideAllProgresses();
    }

    @Override
    public void onProgressCancelled(String progressTag) {
        if (ProgressDialogFragment.TAG.equals(progressTag)) {
            for (BasePresenter presenter : mPresenters) {
                presenter.cancelTasks();
            }
        }
    }
}
