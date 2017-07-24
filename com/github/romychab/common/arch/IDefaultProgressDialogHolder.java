package com.github.romychab.common.arch;


import com.github.romychab.common.dialogs.ProgressDialogFragment;

public interface IDefaultProgressDialogHolder {

    void registerProgressCallback(ProgressDialogFragment.IProgressCallbacks callbacks);

    void unregisterProgressCallback(ProgressDialogFragment.IProgressCallbacks callbacks);

}
