package com.github.romychab.common.arch;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.widget.Toast;

import com.github.romychab.common.R;
import com.github.romychab.common.arch.IBaseView.ProgressAction;
import com.github.romychab.common.arch.IBaseView.ProgressType;
import com.github.romychab.common.dialogs.ProgressDialogFragment;

import java.util.HashSet;
import java.util.Set;


public class BaseActivityDelegate
    implements
        IDefaultProgressDialogHolder,
        ProgressDialogFragment.IProgressCallbacks {

    private Set<ProgressDialogFragment.IProgressCallbacks> mProgressCallbacks = new HashSet<>();

    private BasePresenter[] mPresenters;

    private Context mContext;
    private FragmentManager mFragmentManager;

    public BaseActivityDelegate(Context context, FragmentManager fragmentManager, BasePresenter[] presenters) {
        mContext = context;
        mFragmentManager = fragmentManager;
        mPresenters = presenters;
    }

    @Override
    public void registerProgressCallback(ProgressDialogFragment.IProgressCallbacks callbacks) {
        mProgressCallbacks.add(callbacks);
    }

    @Override
    public void unregisterProgressCallback(ProgressDialogFragment.IProgressCallbacks callbacks) {
        mProgressCallbacks.remove(callbacks);
    }

    @Override
    public void onProgressCancelled(String progressTag) {
        if (ProgressDialogFragment.TAG.equals(progressTag)) {
            for (ProgressDialogFragment.IProgressCallbacks callbacks : mProgressCallbacks) {
                callbacks.onProgressCancelled(progressTag);
            }
            for (BasePresenter presenter : mPresenters) {
                presenter.cancelTasks();
            }
        }
    }

    public void onDestroy() {
        mProgressCallbacks.clear();
    }

    public void onError(Throwable error) {
        if (error instanceof BaseException) {
            Toast.makeText(mContext, ((BaseException) error).getUserReadableMessage(), Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(mContext, R.string.err_unknown, Toast.LENGTH_SHORT).show();
        }
    }

    public void setProgress(ProgressAction action, ProgressType progressType) {
        if (progressType.isDefault()) {
            switch (action) {
                case SHOW:
                    showProgressDialog(progressType);
                    break;
                case HIDE:
                    hideProgressDialog(progressType);
                    break;
                case UPDATE:
                    updateProgressDialog(progressType);
                    break;
            }
        }

    }

    public void hideAllProgresses() {
        hideProgressDialog(ProgressType.DEFAULT);
    }


    private void updateProgressDialog(ProgressType progressType) {
        ProgressDialogFragment fragment = (ProgressDialogFragment)
            mFragmentManager.findFragmentByTag(ProgressDialogFragment.TAG);
        if (null != fragment) {
            fragment.updateMessage(null == progressType.getMessage() ? mContext.getString(R.string.waiting_message) : progressType.getMessage());
        }
    }

    private void hideProgressDialog(ProgressType progressType) {
        ProgressDialogFragment fragment = (ProgressDialogFragment)
            mFragmentManager.findFragmentByTag(ProgressDialogFragment.TAG);
        if (null != fragment) {
            fragment.dismiss();
        }
    }

    private void showProgressDialog(ProgressType progressType) {
        ProgressDialogFragment fragment = ProgressDialogFragment.newInstance(
            new ProgressDialogFragment.Options()
                .setTitle(mContext.getString(R.string.waiting_title))
                .setIndeterminate(true)
                .setMessage(null == progressType.getMessage() ? mContext.getString(R.string.waiting_message) : progressType.getMessage()),
            null
        );
        fragment.show(mFragmentManager, ProgressDialogFragment.TAG);
    }
}
