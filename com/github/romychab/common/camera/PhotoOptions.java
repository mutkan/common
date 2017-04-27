package com.github.romychab.common.camera;


import android.support.annotation.Nullable;

/**
 * Options to create a {@link CameraFragment}.
 */
public class PhotoOptions {

    public static final int MAX_SIZE = 1200; // max dimension (either with or height) of image

    String mFileProvider;

    int mMaxDimension = MAX_SIZE;

    @Nullable
    String mTargetImageFile;

    @Nullable
    String mTargetFragmentTag;

    boolean mDetectRotation = false;

    /**
     * @param fileProvider Provider authority that can share app files to other applications
     *                     (see example in the comment to {@link CameraFragment}).
     *                     For more details see:
     *                     1) https://developer.android.com/reference/android/support/v4/content/FileProvider.html
     *                     2) https://developer.android.com/training/camera/photobasics.html#TaskPath
     */
    public PhotoOptions(String fileProvider) {
        mFileProvider = fileProvider;
    }

    /**
     * @param maxDimension Maximum height and width. If the captured photo has width or height larger than
     *                     this value it will be resized with keeping aspect ratio.
     *                     Used if photo is requested by {@link CameraFragment#requestPhoto()} call.
     *                     Max value: {@link #MAX_SIZE}.
     */
    public PhotoOptions setMaxDimension(int maxDimension) {
        mMaxDimension = maxDimension > MAX_SIZE ? MAX_SIZE : maxDimension;
        return this;
    }

    /**
     * @param targetFile path to file in which captured photo will be saved.
     *                   Used if photo is requested by {@link CameraFragment#requestPhoto()} call.
     */
    public PhotoOptions setTargetImageFile(@Nullable String targetFile) {
        mTargetImageFile = targetFile;
        return this;
    }

    /**
     * @param targetFragment tag of fragment that will receive callbacks, must implement {@link IPhotoCallback} interface.
     *                       If this parameter is NULL than callbacks will be passed to activity.
     */
    public PhotoOptions setTargetFragmentTag(@Nullable String targetFragment) {
        mTargetFragmentTag = targetFragment;
        return this;
    }

    /**
     * @param detectRotation TRUE to parse EXIF information about captured image and rotate it if needed.
     */
    public PhotoOptions setDetectRotation(boolean detectRotation) {
        mDetectRotation = detectRotation;
        return this;
    }
}
