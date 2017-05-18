package com.github.romychab.common.dialogs;


import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;

/**
 * AlertDialog impl. inside DialogFragment
 */
public class AlertDialogFragment
        extends DialogFragment
        implements
        DialogInterface.OnClickListener {

    public static final String TAG = AlertDialogFragment.class.getSimpleName();

    private static final String ARG_OPTIONS = "OPTIONS";
    private static final String ARG_TARGET_TAG = "TARGET";

    protected Options mOptions;

    /**
     * Create a new instance of {@link AlertDialog}
     * @param options options for a dialog, such as title, message, etc.
     * @param targetTag tag of fragment that implements {@link IAlertDialogCallbacks} and will receive
     *                  callbacks.
     * @return
     */
    public static AlertDialogFragment newInstance(Options options, @Nullable String targetTag) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_OPTIONS, options);
        args.putString(ARG_TARGET_TAG, targetTag);
        AlertDialogFragment fragment = new AlertDialogFragment();
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
        return create(getOptions());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ARG_OPTIONS, getOptions());
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        getCallbacks().onAlertDialogCancelled(getTag());
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        getCallbacks().onAlertDialogCallback(getTag(), which);
    }

    // --- protected

    protected AlertDialog create(Options options) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setMessage(options.mMessage)
                .setTitle(options.mTitle)
                .setCancelable(options.mCancellable);
        if (!TextUtils.isEmpty(options.mPositiveButton)) {
            builder.setPositiveButton(options.mPositiveButton, this);
        }
        if (!TextUtils.isEmpty(options.mNegativeButton)) {
            builder.setPositiveButton(options.mNegativeButton, this);
        }
        if (!TextUtils.isEmpty(options.mNeutralButton)) {
            builder.setPositiveButton(options.mNeutralButton, this);
        }
        return builder.create();
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

    private IAlertDialogCallbacks getCallbacks() {
        Object target = DialogFragmentUtils.searchTarget(this, getTargetTag(), EMPTY_CALLBACKS);
        return target instanceof IAlertDialogCallbacks ? (IAlertDialogCallbacks) target : EMPTY_CALLBACKS;
    }

    // ---

    public interface IAlertDialogCallbacks {
        void onAlertDialogCallback(String dialogTag, int which);
        void onAlertDialogCancelled(String dialogTag);
    }

    private static final IAlertDialogCallbacks EMPTY_CALLBACKS = new IAlertDialogCallbacks() {
        @Override
        public void onAlertDialogCallback(String dialogTag, int which) {
            Log.e(TAG, "Warn! No callbacks for AlertDialogFragment with tag '" + dialogTag + "'");
        }

        @Override
        public void onAlertDialogCancelled(String dialogTag) {
            Log.e(TAG, "Warn! No callbacks for AlertDialogFragment with tag '" + dialogTag + "'");
        }
    };

    public static class Options implements Parcelable {

        public String mTitle;

        public String mMessage;

        public String mPositiveButton;

        public String mNegativeButton;

        public String mNeutralButton;

        public boolean mCancellable = true;

        public Options setTitle(String title) {
            mTitle = title;
            return this;
        }

        public Options setMessage(String message) {
            mMessage = message;
            return this;
        }

        public Options setPositiveButton(String positiveButton) {
            mPositiveButton = positiveButton;
            return this;
        }

        public Options setNegativeButton(String negativeButton) {
            mNegativeButton = negativeButton;
            return this;
        }

        public Options setNeutralButton(String neutralButton) {
            mNeutralButton = neutralButton;
            return this;
        }

        public Options setCancellable(boolean cancellable) {
            mCancellable = cancellable;
            return this;
        }

        // --- auto-generated

        public Options() {
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.mTitle);
            dest.writeString(this.mMessage);
            dest.writeString(this.mPositiveButton);
            dest.writeString(this.mNegativeButton);
            dest.writeString(this.mNeutralButton);
            dest.writeByte(this.mCancellable ? (byte) 1 : (byte) 0);
        }

        protected Options(Parcel in) {
            this.mTitle = in.readString();
            this.mMessage = in.readString();
            this.mPositiveButton = in.readString();
            this.mNegativeButton = in.readString();
            this.mNeutralButton = in.readString();
            this.mCancellable = in.readByte() != 0;
        }

        public static final Creator<Options> CREATOR = new Creator<Options>() {
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
