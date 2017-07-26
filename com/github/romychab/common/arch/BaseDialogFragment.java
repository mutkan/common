package com.github.romychab.common.arch;

import android.content.Context;

import com.arellomobile.mvp.MvpAppCompatDialogFragment;
import com.github.romychab.common.dialogs.ProgressDialogFragment;

public abstract class BaseDialogFragment
    extends MvpAppCompatDialogFragment
    implements
        ProgressDialogFragment.IProgressCallbacks {
    private BaseFragmentDelegate mDelegate;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mDelegate = new BaseFragmentDelegate(
            (IBaseView) context,
            (IDefaultProgressDialogHolder) context,
            registerPresenters()
        );
        mDelegate.onAttach(context);
    }

    @Override
    public void onDetach() {
        mDelegate.onDetach();
        super.onDetach();
    }

    public void onError(Throwable error) {
        mDelegate.onError(error);
    }

    public void setProgress(IBaseView.ProgressAction action, IBaseView.ProgressType progressType) {
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
