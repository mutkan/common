package com.github.romychab.common.camera;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Capture photos using camera.
 * Photos are saved in dir returned by getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES) if
 * the parameter 'targetFile' is NULL in {@link #newInstance(PhotoOptions)} )};
 *
 * To capture photo use {@link #requestPhoto()} method.
 * But if app needs only thumbnails it's better to use {@link #requestThumbnail()} method.
 *
 * If you want to save photo in a file on external storage in non-app folder than you need to request
 * {@link Manifest.permission#WRITE_EXTERNAL_STORAGE} permission. You can do it manually or
 * use {@link #addPermission(String)}, so all added permissions will be requested by this fragment
 * automatically.
 *
 * You have to configure file provider and pass it to {@link #newInstance(PhotoOptions)}).
 * File providers are required starting with API = 24 and higher. Otherwise FileUriExposedException
 * will be thrown.
 *
 * https://developer.android.com/reference/android/support/v4/content/FileProvider.html
 *
 * Example:
 *
 * <pre>{@code
 *
 * <application>
 *   ...
 *   <provider
 *     android:name="android.support.v4.content.FileProvider"
 *     android:authorities="com.example.android.fileprovider"
 *     android:exported="false"
 *     android:grantUriPermissions="true">
 *     <meta-data
 *       android:name="android.support.FILE_PROVIDER_PATHS"
 *       android:resource="@xml/file_paths"></meta-data>
 *   </provider>
 *   ...
 * </application>
 *
 * }</pre>
 *
 *
 * Usage example:
 *
 * <pre>{@code
 *
 *   class MyAwesomeActivity extends Activity implements IPhotoCallbacks {
 *
 *      CameraFragment mCameraFragment;
 *
 *      protected void onCreate(@Nullable Bundle savedState) {
 *          ...
 *          mCameraFragment = getFragmentManager().findFragmentByTag(CameraFragment.TAG);
 *          if (null == mCameraFragment) {
 *               mCameraFragment = CameraFragment.newInstance(new PhotoOptions("com.example.photo_provider")
 *                  .setMaxDimension(800)
 *                  .setTargetImageFile(Const.getPhotoPath(getActivity()).getAbsolutePath() + "/" + TEMP_IMAGE_FILE)
 *                  .setDetectRotation(true)
 *                  .setTargetFragmentTag(null));
 *
 *              getFragmentManager().beginTransaction()
 *                  .addFragment(mCameraFragment, CameraFragment.TAG)
 *                  .commit();
 *          }
 *      }
 *
 *      void onCaptureImageClicked() {
 *          mCameraFragment.requestPhoto();
 *      }
 *
 *      void onCaptureThumbnailClicked() {
 *          mCameraFragment.requestThumbnail();
 *      }
 *
 *      // --- IPhotoCallbacks methods
 *
 *      public void onPhotoCaptured(File photoFile) {
 *          // process photo file here
 *      }
 *
 *      public void onThumbnailCaptured(Bitmap thumbnail) {
 *          // process thumbnail here
 *      }
 *
 *      ...
 *   }
 *
 * }</pre>
 *
 */
public class CameraFragment extends Fragment {

    public static final String TAG = CameraFragment.class.getSimpleName();

    private static final String ARG_FILE_PROVIDER = "FILE_PROVIDER";
    private static final String ARG_MAX_DIMENSION = "MAX_DIMENSION";
    private static final String ARG_TARGET_FILE = "FILE";
    private static final String ARG_TARGET_FRAGMENT = "TARGET";
    private static final String ARG_DETECT_ROTATION = "ROTATION";

    private static final String KEY_IMAGE_FILE = "IMAGE_FILE";
    private static final String KEY_PERMISSIONS = "PERMISSIONS";
    private static final String KEY_ACTION = "ACTION";

    private static final int RQ_PERMISSIONS = 0x14; // request code for acquiring permissions
    private static final int RQ_CAMERA_INTENT = 0x4; // request code for camera intent
    private static final int RQ_CAMERA_THUMBNAIL_INTENT = 0x5; // request code for camera intent

    public static final int ACTION_FULL_SIZE_PHOTO = 1;
    public static final int ACTION_THUMBNAIL = 2;

    private File mImageFile; // file in which captured image will be places

    private IPhotoCallbacks mPhotoCallback; // target activity/fragment that implements IPhotoCallbacks interface

    private ProcessImageTask mResizePhotoTask; // task for processing captured image, may be NULL

    private ArrayList<String> mPermissions = new ArrayList<>();

    private int mAction = 0;

    // --- construct

    /**
     * Create non-UI fragment that can handle requests to camera
     */
    public static CameraFragment newInstance(PhotoOptions photoOptions) {
        Bundle args = new Bundle();
        args.putString(ARG_FILE_PROVIDER, photoOptions.mFileProvider);
        args.putInt(ARG_MAX_DIMENSION, photoOptions.mMaxDimension);
        args.putString(ARG_TARGET_FILE, photoOptions.mTargetImageFile);
        args.putString(ARG_TARGET_FRAGMENT, photoOptions.mTargetFragmentTag);
        args.putBoolean(ARG_DETECT_ROTATION, photoOptions.mDetectRotation);
        CameraFragment fragment = new CameraFragment();
        fragment.setArguments(args);
        return fragment;
    }

    // --- lifecycle

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true); // keep fragment during configuration changes

        if (null != savedInstanceState) {
            String path = savedInstanceState.getString(KEY_IMAGE_FILE);
            if (!TextUtils.isEmpty(path)) {
                mImageFile = new File(path);
            }
            mPermissions = savedInstanceState.getStringArrayList(KEY_PERMISSIONS);
            mAction = savedInstanceState.getInt(KEY_ACTION, 0);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (null != mImageFile) {
            outState.putString(KEY_IMAGE_FILE, mImageFile.getAbsolutePath());
        }
        outState.putStringArrayList(KEY_PERMISSIONS, mPermissions);
        outState.putInt(KEY_ACTION, mAction);
    }

    @Override
    public void onDestroy() {
        // fragment is going to be destroyed, so let's cancel the processing task and delete captured file
        if (null != mResizePhotoTask) {
            mResizePhotoTask.cancel(false);
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if ((requestCode & 0xFF) == RQ_PERMISSIONS) { // mask 0xFF to clip higher bits
            if (getNonGrantedPermissions().size() == 0) {
                // all permissions has been granted
                continueActions();
            }
            else {
                getCallback().onPhotoError(IPhotoCallbacks.ERR_PERMISSIONS_DENIED);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RQ_CAMERA_INTENT && resultCode == Activity.RESULT_OK) {
            // the captured image must be resized
            mResizePhotoTask = new ProcessImageTask(mImageFile, getMaxDimension(), isDetectRotation(), getCallback());
            mResizePhotoTask.execute();
        }
        else if (requestCode == RQ_CAMERA_THUMBNAIL_INTENT && resultCode == Activity.RESULT_OK) {
            Bundle extras = data.getExtras();
            if (null != extras) {
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                if (null != imageBitmap) {
                    getCallback().onThumbnailCaptured(imageBitmap);
                    return;
                }
            }
            getCallback().onPhotoError(IPhotoCallbacks.ERR_CANT_PROCESS_IMAGE);
        }
    }

    // --- public

    /**
     * Add additional permission to be requested
     */
    public CameraFragment addPermission(String permission) {
        if (!mPermissions.contains(permission)) {
            mPermissions.add(permission);
        }
        return this;
    }

    /**
     * Do request photo from camera and save it to file.
     * Additional parameters are specified during creation, by calling {@link #newInstance(PhotoOptions)}
     * method.
     * The result image file will be delivered into {@link IPhotoCallbacks#onPhotoCaptured(File)} call.
     */
    public void requestPhoto() {
        mAction = ACTION_FULL_SIZE_PHOTO;
        if (needPermissions() && Build.VERSION.SDK_INT >= 23) {
            doRequestPermissions();
            return;
        }

        File f;
        try {
            f = createImageFile();
        } catch (Exception e) {
            Log.e(TAG, "Error!", e);
            getCallback().onPhotoError(IPhotoCallbacks.ERR_CANT_CREATE_IMAGE_FILE);
            return;
        }

        mImageFile = f;

        if (!dispatchTakePictureIntent(f)) {
            getCallback().onPhotoError(IPhotoCallbacks.ERR_LAUNCH_CAMERA);
        }
    }

    /**
     * Do request small thumbnail of photo.
     * It will not be saved to file.
     * {@link IPhotoCallbacks#onThumbnailCaptured(Bitmap)} will be called after all.
     */
    public void requestThumbnail() {
        mAction = ACTION_THUMBNAIL;
        if (needPermissions() && Build.VERSION.SDK_INT >= 23) {
            doRequestPermissions();
            return;
        }

        if (!dispatchTakeThumbnailIntent()) {
            getCallback().onPhotoError(IPhotoCallbacks.ERR_LAUNCH_CAMERA);
        }
    }

    // --- private

    private File createImageFile() throws Exception {
        File targetFile;
        if (!TextUtils.isEmpty(getTargetFile())) {
            targetFile = new File(getTargetFile());
            targetFile.createNewFile(); // may be ignored without any problems
        }
        else {
            // target file is not specified
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "photo_" + timeStamp;

            File dir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (null == dir) {
                throw new Exception("No external storage or it is unavailable!");
            }

            File storageDir = TextUtils.isEmpty(getTargetFile()) ?
                    dir :
                    new File(getTargetFile()).getParentFile();

            targetFile = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
        }
        return targetFile;
    }

    private boolean dispatchTakePictureIntent(File photoFile) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            // Continue only if the File was successfully created
            if (null != photoFile) {
                Uri photoURI = FileProvider.getUriForFile(
                        getActivity(),
                        getFileProvider(),
                        photoFile
                );
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, RQ_CAMERA_INTENT);
                return true;
            }
        }
        return false;
    }

    private boolean dispatchTakeThumbnailIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, RQ_CAMERA_THUMBNAIL_INTENT);
            return true;
        }
        return false;
    }

    private String getFileProvider() {
        return getArguments().getString(ARG_FILE_PROVIDER);
    }

    private int getMaxDimension() {
        return getArguments().getInt(ARG_MAX_DIMENSION);
    }

    private String getTargetFile() {
        return getArguments().getString(ARG_TARGET_FILE, "");
    }

    private String getTargetTag() {
        return getArguments().getString(ARG_TARGET_FRAGMENT);
    }

    private boolean isDetectRotation() { return getArguments().getBoolean(ARG_DETECT_ROTATION, false); }

    private IPhotoCallbacks getCallback() {
        if (null == mPhotoCallback) {
            // first call, callback hasn't been specified yet
            mPhotoCallback = EMPTY_CALLBACK; // use empty callback by default
            String tag = getTargetTag();
            if (TextUtils.isEmpty(tag)) {
                // target fragment's tag is not specified;
                // assume that the host activity have to receive all callbacks
                if (null != getActivity() && getActivity() instanceof IPhotoCallbacks) {
                    // yep, activity implements required interface
                    mPhotoCallback = (IPhotoCallbacks) getActivity();
                }
            }
            else {
                // target tag is specified; searching target fragment by tag
                Fragment fragment = getFragmentManager().findFragmentByTag(tag);
                if (null != fragment && fragment instanceof IPhotoCallbacks) {
                    // it's found and impl. IPhotoCallback, so we use it
                    mPhotoCallback = (IPhotoCallbacks) fragment;
                }
            }
        }
        return mPhotoCallback;
    }

    private boolean needPermissions() {
        return getNonGrantedPermissions().size() > 0;
    }

    private List<String> getNonGrantedPermissions() {
        List<String> nonGrantedPermissions = new LinkedList<>();
        for (String permission : mPermissions) {
            if (ContextCompat.checkSelfPermission(getActivity(), permission) != PackageManager.PERMISSION_GRANTED) {
                nonGrantedPermissions.add(permission);
            }
        }
        return nonGrantedPermissions;
    }

    @TargetApi(value = 23)
    private void doRequestPermissions() {
        List<String> nonGrantedPermissions = getNonGrantedPermissions();
        String[] permissions = nonGrantedPermissions.toArray(new String[nonGrantedPermissions.size()]);
        requestPermissions(permissions, RQ_PERMISSIONS);
    }

    private void continueActions() {
        switch (mAction) {
            case ACTION_FULL_SIZE_PHOTO:
                requestPhoto();
                break;
            case ACTION_THUMBNAIL:
                requestThumbnail();
                break;
        }
    }

    private static final IPhotoCallbacks EMPTY_CALLBACK = new IPhotoCallbacks() {
        @Override
        public void onPhotoCaptured(File photoFile) { ; }
        @Override
        public void onThumbnailCaptured(Bitmap bitmap) { ; }
        @Override
        public void onStartPhotoProcessing() { ; }
        @Override
        public void onFinishPhotoProcessing() { ; }
        @Override
        public void onPhotoError(int code) { ; }
    };

    // ---


}
