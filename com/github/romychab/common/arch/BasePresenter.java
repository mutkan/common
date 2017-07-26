package com.github.romychab.common.arch;

import android.os.Handler;
import android.util.Log;

import com.arellomobile.mvp.MvpPresenter;
import com.github.romychab.common.arch.IBaseView.ProgressAction;
import com.github.romychab.common.arch.IBaseView.ProgressType;
import com.github.romychab.common.utils.handlers.IHandler;
import com.github.romychab.common.utils.handlers.UiHandler;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.FlowableTransformer;
import io.reactivex.SingleTransformer;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;


public class BasePresenter<V extends IBaseView> extends MvpPresenter<V> {

    public static final String TAG = BasePresenter.class.getSimpleName();

    private IHandler mHandler;

    private SafeScheduler mSafeScheduler;

    private CompositeDisposable mCompositeDisposable;

    private long mMainThreadId;

    private Map<Integer, Integer> mProgresses = new HashMap<>();

    public BasePresenter() {
        init(new UiHandler(new Handler()));
    }

    public BasePresenter(IHandler handler) {
        init(handler);
    }

    private void init(IHandler handler) {
        mHandler = handler;

        mSafeScheduler = new SafeScheduler(mHandler);

        mCompositeDisposable = new CompositeDisposable();
        mMainThreadId = Thread.currentThread().getId();
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        getViewState().hideAllProgresses();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelTasks();
    }

    public SafeScheduler safeScheduler() {
        return mSafeScheduler;
    }

    public void cancelTasks() {
        mCompositeDisposable.dispose();
        mCompositeDisposable = null;
        mCompositeDisposable = new CompositeDisposable();
    }

    // --- Single composers

    protected <D> SingleTransformer<D, D> withSingleProgress(ProgressType progress) {
        return upstream -> upstream
            .doOnSubscribe( disposable -> onProgressStart(progress) )
            .doFinally( () -> onProgressFinish(progress) );
    }

    protected <D> SingleTransformer<D, D> withSingleProgress() {
        return withSingleProgress(ProgressType.DEFAULT);
    }

    protected <D> SingleTransformer<D, D> withSingleErrors() {
        return upstream -> upstream.doOnError(this::onError);
    }

    protected <D> SingleTransformer<D, D> withDefaultSingle() {
        return withDefaultSingle(ProgressType.DEFAULT);
    }

    protected <D> SingleTransformer<D, D> withDefaultSingle(ProgressType progressType) {
        return upstream -> upstream
            .compose(withSingleProgress(progressType))
            .observeOn(safeScheduler());
    }

    // --- Flowable composers

    protected <D> FlowableTransformer<D, D> withFlowableProgress(ProgressType progress) {
        return upstream -> upstream
            .doOnSubscribe( subscription -> onProgressStart(progress) )
            .doFinally( () -> onProgressFinish(progress) );
    }

    protected <D> FlowableTransformer<D, D> withFlowableProgress() {
        return withFlowableProgress(ProgressType.DEFAULT);
    }

    protected <D> FlowableTransformer<D, D> withFlowableErrors() {
        return upstream -> upstream.doOnError(this::onError);
    }

    protected <D> FlowableTransformer<D, D> withDefaultFlowable(ProgressType progressType) {
        return upstream -> upstream
            .compose(withFlowableProgress(progressType))
            .observeOn(safeScheduler());
    }

    protected <D> FlowableTransformer<D, D> withDefaultFlowable() {
        return withDefaultFlowable(ProgressType.DEFAULT);
    }

    // ---

    protected void registerDisposable(Disposable disposable) {
        mCompositeDisposable.add(disposable);
    }

    protected void safeRun(Runnable runnable) {
        if (mMainThreadId == Thread.currentThread().getId()) {
            runnable.run();
        }
        else {
            mHandler.post(runnable);
        }
    }

    protected void publishProgress(ProgressType progressType) {
        Integer count = mProgresses.get(progressType.getId());
        if (null == count || count == 0) {
            return;
        }
        getViewState().setProgress(ProgressAction.UPDATE, progressType);
    }

    private void onProgressStart(ProgressType progressType) {
        safeRun( () -> {
            Integer count = mProgresses.get(progressType.getId());
            if (null == count) {
                count = 0;
            }
            count++;
            mProgresses.put(progressType.getId(), count);
            if (count == 1) {
                getViewState().setProgress(ProgressAction.SHOW, progressType);
            }
            else {
                getViewState().setProgress(ProgressAction.UPDATE, progressType);
            }
        });
    }

    private void onProgressFinish(ProgressType progressType) {
        safeRun( () -> {
            Integer count = mProgresses.get(progressType.getId());
            if (null == count || count == 0) {
                return;
            }
            count--;
            mProgresses.put(progressType.getId(), count);
            if (count == 0) {
                getViewState().setProgress(ProgressAction.HIDE, progressType);
            }
            else {
                getViewState().setProgress(ProgressAction.UPDATE, progressType);
            }
        });
    }

    private void onError(Throwable throwable) {
        Log.e(TAG, "Error!", throwable);
        safeRun( () -> getViewState().onError(throwable) );
    }
}
