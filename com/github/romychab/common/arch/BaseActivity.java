package com.github.romychab.common.arch;

import android.widget.Toast;

import com.arellomobile.mvp.MvpAppCompatActivity;
import com.github.romychab.common.R;
import com.github.romychab.common.arch.IBaseView.ProgressAction;
import com.github.romychab.common.arch.IBaseView.ProgressType;
import com.github.romychab.common.dialogs.ProgressDialogFragment;

import java.util.HashSet;
import java.util.Set;

public abstract class BaseActivity
    extends MvpAppCompatActivity
    implements
        ProgressDialogFragment.IProgressCallbacks,
        IDefaultProgressDialogHolder {


    private Set<ProgressDialogFragment.IProgressCallbacks> mProgressCallbacks = new HashSet<>();

    @Override
    public void registerProgressCallback(ProgressDialogFragment.IProgressCallbacks callbacks) {
        mProgressCallbacks.add(callbacks);
    }

    @Override
    public void unregisterProgressCallback(ProgressDialogFragment.IProgressCallbacks callbacks) {
        mProgressCallbacks.remove(callbacks);
    }

    @Override
    protected void onDestroy() {
        mProgressCallbacks.clear();
        super.onDestroy();
    }

    public void onError(Throwable error) {
        if (error instanceof BaseException) {
            Toast.makeText(this, ((BaseException) error).getUserReadableMessage(), Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(this, R.string.err_unknown, Toast.LENGTH_SHORT).show();
        }
    }

    public void setProgress(ProgressAction action, ProgressType progressType) {
        if (progressType.isDefault()) {
            switch (action) {
                case SHOW:
                    showProgressDialog(progressType);
                    break;
                case HIDE:
                    hideProgressDialog();
                    break;
                case UPDATE:
                    updateProgressDialog(progressType);
                    break;
            }
        }
    }

    private void updateProgressDialog(ProgressType progressType) {
        ProgressDialogFragment fragment = (ProgressDialogFragment)
                getSupportFragmentManager().findFragmentByTag(ProgressDialogFragment.TAG);
        if (null != fragment) {
            fragment.updateMessage(null == progressType.getMessage() ? "Application is performing required operations..." : progressType.getMessage());
        }
    }

    @Override
    public void onProgressCancelled(String progressTag) {
        if (ProgressDialogFragment.TAG.equals(progressTag)) {
            for (ProgressDialogFragment.IProgressCallbacks callbacks : mProgressCallbacks) {
                callbacks.onProgressCancelled(progressTag);
            }
            cancelPresenterTasks();
        }
    }

    protected abstract void cancelPresenterTasks();

    private void hideProgressDialog() {
        ProgressDialogFragment fragment = (ProgressDialogFragment)
            getSupportFragmentManager().findFragmentByTag(ProgressDialogFragment.TAG);
        if (null != fragment) {
            fragment.dismiss();
        }
    }

    private void showProgressDialog(ProgressType progressType) {
        ProgressDialogFragment fragment = ProgressDialogFragment.newInstance(
            new ProgressDialogFragment.Options()
                .setTitle("Please, wait")
                .setIndeterminate(true)
                .setMessage(null == progressType.getMessage() ? "Application is performing required operations..." : progressType.getMessage()),
            null
        );
        fragment.show(getSupportFragmentManager(), ProgressDialogFragment.TAG);
    }

}
