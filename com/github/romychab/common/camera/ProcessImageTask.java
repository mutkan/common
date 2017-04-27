package com.github.romychab.common.camera;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Processing captured image.
 * Performing resizing and rotating image.
 */
public class ProcessImageTask extends AsyncTask<Void, Void, Boolean> {

    public static final String TAG = ProcessImageTask.class.getSimpleName();

    private IPhotoCallbacks mCallback;
    private File mImageFile;
    private int mMaxDimension;
    private boolean mRotate;

    ProcessImageTask(File imageFile, int maxDimension, boolean rotate, IPhotoCallbacks callback) {
        mCallback = callback;
        mImageFile = imageFile;
        mMaxDimension = maxDimension;
        mRotate = rotate;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mCallback.onStartPhotoProcessing();
    }

    @Override
    protected Boolean doInBackground(Void... params) {

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mImageFile.getAbsolutePath(), bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        if (mMaxDimension < photoW || mMaxDimension < photoH) {
            int scaleFactor = Math.min(photoW / mMaxDimension, photoH / mMaxDimension);
            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inSampleSize = scaleFactor;
        }
        bmOptions.inPurgeable = true;
        bmOptions.inJustDecodeBounds = false;

        try {
            Bitmap bitmap = BitmapFactory.decodeFile(mImageFile.getAbsolutePath(), bmOptions);
            bitmap = rotateIfNeeded(bitmap);
            OutputStream os = new FileOutputStream(mImageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, os); // 80 - JPEG quality (max - 100)
            bitmap.recycle();
        } catch (Exception e) {
            Log.e(TAG, "Error!", e);
            return false;
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean res) {
        super.onPostExecute(res);
        if (!res) {
            mCallback.onPhotoError(IPhotoCallbacks.ERR_CANT_PROCESS_IMAGE);
        }
        mCallback.onFinishPhotoProcessing();
        mCallback.onPhotoCaptured(mImageFile);
        mCallback = null;
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        mCallback.onFinishPhotoProcessing();
        mCallback.onPhotoError(IPhotoCallbacks.ERR_CANCELLED);
        mCallback = null;
    }

    private Bitmap rotateIfNeeded(Bitmap bitmap) {
        if (!mRotate) {
            return bitmap;
        }
        try {
            ExifInterface ei = new ExifInterface(mImageFile.getAbsolutePath());
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotateImage(bitmap, 90);
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotateImage(bitmap, 180);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotateImage(bitmap, 270);
                case ExifInterface.ORIENTATION_NORMAL:
                default:
                    return bitmap;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return bitmap;
        }
    }

    private Bitmap rotateImage(Bitmap bitmap, int angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        Bitmap resultBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        bitmap.recycle();
        return resultBitmap;
    }
}
