package com.github.romychab.common.arch;


import android.support.annotation.Nullable;

public class BaseException extends RuntimeException {

    private String mUserReadableMessage;

    public BaseException(String message, String userReadableMessage) {
        super(message);
        mUserReadableMessage = userReadableMessage;
    }

    public BaseException(String message) {
        super(message);
    }

    @Nullable
    public String getUserReadableMessage() {
        return mUserReadableMessage;
    }
}
