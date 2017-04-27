package com.github.romychab.common.camera;


import android.graphics.Bitmap;

import java.io.File;

public interface IPhotoCallbacks {

    int ERR_PERMISSIONS_DENIED = 1;
    int ERR_CANT_CREATE_IMAGE_FILE = 2;
    int ERR_CANT_PROCESS_IMAGE = 3;
    int ERR_LAUNCH_CAMERA = 4;
    int ERR_CANCELLED = 5;

    void onPhotoCaptured(File photoFile);

    void onThumbnailCaptured(Bitmap bitmap);

    void onStartPhotoProcessing();

    void onFinishPhotoProcessing();

    void onPhotoError(int code);

}
