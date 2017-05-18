package com.github.romychab.common.dialogs;


import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;

/**
 * ProgressDialog implementation inside a DialogFragment.
 */
public class ProgressDialogFragment extends DialogFragment {

    public static final String TAG = ProgressDialogFragment.class.getSimpleName();

    private static final String ARG_OPTIONS = "OPTIONS";
    private static final String ARG_TARGET_TAG = "TARGET_TAG";

    protected Options mOptions;
    protected ProgressDialog mProgressDialog;

    private boolean mUpdatingInProgress = false;

    private Handler mUiHandler = new Handler(Looper.getMainLooper());

    /**
     * Create a new instance of {@link ProgressDialogFragment}.
     * @param options additional parameters, such as message, title etc.
     * @param targetTag tag of fragment that implements IProgressCallback interface and will receive
     *                  callbacks from this dialog
     */
    public static ProgressDialogFragment newInstance(Options options, @Nullable String targetTag) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_OPTIONS, options);
        args.putString(ARG_TARGET_TAG, targetTag);
        ProgressDialogFragment fragment = new ProgressDialogFragment();
        fragment.setArguments(args);
        fragment.mOptions = options;
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (null != savedInstanceState) {
            mOptions = savedInstanceState.getParcelable(ARG_OPTIONS);
        }
        mProgressDialog = createDialog();

        updateDialog(mProgressDialog, getOptions());

        return mProgressDialog;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ARG_OPTIONS, mOptions);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        getCallbacks().onProgressCancelled(getTag());
    }

    // --- public

    public ProgressDialogFragment updateMaxValue(int newMaxValue) {
        getOptions().mMaxValue = newMaxValue;
        commitUpdates();
        return this;
    }

    public ProgressDialogFragment updateValue(int newValue) {
        getOptions().mValue = newValue;
        commitUpdates();
        return this;
    }

    public ProgressDialogFragment updateMessage(String newMessage) {
        getOptions().mMessage = newMessage;
        commitUpdates();
        return this;
    }

    // --- protected

    protected void updateDialog(ProgressDialog progressDialog, Options options) {
        progressDialog.setTitle(options.mTitle);
        progressDialog.setMessage(options.mMessage);
        progressDialog.setIndeterminate(options.mIndeterminate);
        progressDialog.setMax(options.mMaxValue);
        progressDialog.setProgress(options.mValue);
    }

    protected ProgressDialog createDialog() {
        return new ProgressDialog(getContext());
    }

    // --- private

    private Options getOptions() {
        if (null == mOptions) {
            mOptions = getArguments().getParcelable(ARG_OPTIONS);
        }
        return mOptions;
    }

    private String getTargetTag() {
        return getArguments().getString(ARG_TARGET_TAG);
    }

    private IProgressCallbacks getCallbacks() {
        Object target = DialogFragmentUtils.searchTarget(this, getTargetTag(), EMPTY_CALLBACKS);
        return target instanceof IProgressCallbacks ? (IProgressCallbacks) target : EMPTY_CALLBACKS;
    }

    private void commitUpdates() {
        if (mUpdatingInProgress) {
            return;
        }
        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                if (null != mProgressDialog) {
                    updateDialog(mProgressDialog, getOptions());
                }
                else {
                    getArguments().putParcelable(ARG_OPTIONS, getOptions());
                }
                mUpdatingInProgress = false;
            }
        });
    }

    // ---

    public interface IProgressCallbacks {
        void onProgressCancelled(String progressTag);
    }

    private static final IProgressCallbacks EMPTY_CALLBACKS = new IProgressCallbacks() {
        @Override
        public void onProgressCancelled(String progressTag) {
            Log.e(TAG, "Warn! No callbacks for ProgressDialogFragment with tag '" + progressTag + "'");
        }
    };

    public static class Options implements Parcelable {

        public String mTitle;

        public String mMessage;

        public int mMaxValue;

        public int mValue;

        public boolean mIndeterminate = true;

        public Options setTitle(String title) {
            mTitle = title;
            return this;
        }

        public Options setMessage(String message) {
            mMessage = message;
            return this;
        }

        public Options setMaxValue(int maxValue) {
            this.mMaxValue = maxValue;
            return this;
        }

        public Options setValue(int value) {
            this.mValue = value;
            return this;
        }

        public Options setIndeterminate(boolean indeterminate) {
            this.mIndeterminate = indeterminate;
            return this;
        }

        // --- auto-generated

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.mTitle);
            dest.writeString(this.mMessage);
            dest.writeInt(this.mMaxValue);
            dest.writeInt(this.mValue);
            dest.writeByte(this.mIndeterminate ? (byte) 1 : (byte) 0);
        }

        public Options() { }

        protected Options(Parcel in) {
            this.mTitle = in.readString();
            this.mMessage = in.readString();
            this.mMaxValue = in.readInt();
            this.mValue = in.readInt();
            this.mIndeterminate = in.readByte() != 0;
        }

        public static final Parcelable.Creator<Options> CREATOR = new Parcelable.Creator<Options>() {
            @Override
            public Options createFromParcel(Parcel source) {
                return new Options(source);
            }

            @Override
            public Options[] newArray(int size) {
                return new Options[size];
            }
        };
    }

}
