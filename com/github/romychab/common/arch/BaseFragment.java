package com.github.romychab.common.arch;

import android.content.Context;

import com.arellomobile.mvp.MvpAppCompatFragment;
import com.github.romychab.common.dialogs.ProgressDialogFragment;

public abstract class BaseFragment
    extends MvpAppCompatFragment
    implements
        ProgressDialogFragment.IProgressCallbacks {

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        ((IDefaultProgressDialogHolder) context)
            .registerProgressCallback(this);
    }

    @Override
    public void onDetach() {
        ((IDefaultProgressDialogHolder) getActivity())
            .unregisterProgressCallback(this);

        super.onDetach();
    }

    public void onError(Throwable error) {
        ((IBaseView) getActivity()).onError(error);
    }

    public void setProgress(IBaseView.ProgressAction action, IBaseView.ProgressType progressType) {
        ((IBaseView) getActivity()).setProgress(action, progressType);
    }

    @Override
    public void onProgressCancelled(String progressTag) {
        if (ProgressDialogFragment.TAG.equals(progressTag)) {
            cancelPresenterTasks();
        }
    }

    protected abstract void cancelPresenterTasks();

}
