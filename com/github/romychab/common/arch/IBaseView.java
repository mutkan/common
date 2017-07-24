package com.github.romychab.common.arch;


import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.arellomobile.mvp.MvpView;
import com.arellomobile.mvp.viewstate.strategy.OneExecutionStateStrategy;
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType;

public interface IBaseView extends MvpView {

    class ProgressType implements Parcelable {
        public static final ProgressType DEFAULT = new ProgressType(0, null);

        int mId;

        @Nullable
        String mMessage;

        public static ProgressType withId(int type) {
            return withMessage(type, null);
        }

        public static ProgressType withMessage(int type, String message) {
            return new ProgressType(type, message);
        }

        ProgressType() {
        }

        ProgressType(int id, @Nullable String message) {
            mId = id;
            mMessage = message;
        }

        public boolean isDefault() {
            return mId == 0;
        }

        public int getId() {
            return mId;
        }

        @Nullable
        public String getMessage() {
            return mMessage;
        }

        @Override
        public int describeContents() { return 0; }

        @Override
        public void writeToParcel(android.os.Parcel dest, int flags) {
            dest.writeInt(this.mId);
            dest.writeString(this.mMessage);
        }

        protected ProgressType(android.os.Parcel in) {
            this.mId = in.readInt();
            this.mMessage = in.readString();
        }

        public static final Parcelable.Creator<ProgressType> CREATOR = new Parcelable.Creator<ProgressType>() {
            @Override public ProgressType createFromParcel(android.os.Parcel source) { return new ProgressType(source); }
            @Override public ProgressType[] newArray(int size) { return new ProgressType[size]; }
        };
    }

    enum ProgressAction {
        SHOW,
        HIDE,
        UPDATE
    }

    @StateStrategyType(value = OneExecutionStateStrategy.class)
    void onError(Throwable error);

    @StateStrategyType(value = OneExecutionStateStrategy.class)
    void setProgress(ProgressAction action, ProgressType progressType);

}
