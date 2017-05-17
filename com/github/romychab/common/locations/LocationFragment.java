package com.github.romychab.common.locations;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Fragment for checking permissions and location service availability.
 * All required conditions, requests and dialogs will be prepared, shown and processed such as
 * requesting permissions, launching location intents etc.
 *
 * Usage example:
 *
 * <pre>{@code
 *
 *  class MyAwesomeActivity extends FragmentActivity implements LocationFragment.ICallbacks {
 *
 *    ...
 *
 *    void onButtonClicked() {
 *        LocationFragment.initLocations(this);
 *    }
 *
 *    void onActivityResult(int rqCode, int resCode, Intent data) {
 *        LocationFragment.processActivityResults(this, rqCode, resCode, data);
 *    }
 *
 *    void onLocationServicesReady() {
 *        // permissions are granted, location service is enabled
 *    }
 *
 *    void onLocationError(LocationError error);
 *      // handle errors here
 *    }
 *
 * }</pre>
 */
public class LocationFragment
    extends Fragment
    implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    public static final String TAG = LocationFragment.class.getSimpleName();

    public static final int RQ_CLIENT_RESOLUTION = 31;
    public static final int RQ_SETTINGS_RESOLUTION = 33;

    private static final int RQ_PERMISSIONS = 12;

    private static final String ARG_TAG = "TAG";

    private static final String KEY_RESOLVING_ERROR = "RESOLVING_ERROR";
    private static final String KEY_RESOLVING_SETTINGS = "RESOLVING_SETTINGS";
    private static final String KEY_RESOLVING_PERMISSIONS = "RESOLVING_PERMISSIONS";
    private static final String KEY_SETTINGS_ASKED = "SETTINGS_ASKED";

    private String mTag;

    private ICallback mCallback;

    private GoogleApiClient mClient;

    private boolean mResolvingError = false;
    private boolean mResolvingSettings = false;
    private boolean mSettingsAsked = false;
    private boolean mResolvingPermissions = false;

    private LocationRequest mLocationRequest;

    private PendingResult<LocationSettingsResult> mSettingsPendingResult;

    private Set<ILocationListener> mListeners = new LinkedHashSet<>();

    private static LocationFragment newInstance(@Nullable String targetTag) {
        Bundle args = new Bundle();
        args.putString(ARG_TAG, targetTag);
        LocationFragment fragment = new LocationFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static <T extends FragmentActivity & LocationFragment.ICallback> LocationFragment initLocations(T activity) {
        return initLocations(activity.getSupportFragmentManager());
    }

    public static void processActivityResults(FragmentActivity activity, int requestCode, int resultCode, Intent data) {
        Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag(LocationFragment.TAG);
        if (null != fragment) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    private static LocationFragment initLocations(FragmentManager manager) {
        LocationFragment fragment = (LocationFragment) manager.findFragmentByTag(LocationFragment.TAG);
        if (null == fragment) {
            fragment = LocationFragment.newInstance(null);
            manager
                    .beginTransaction()
                    .add(fragment, LocationFragment.TAG)
                    .commit();
        }
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (null != savedInstanceState) {
            mResolvingError = savedInstanceState.getBoolean(KEY_RESOLVING_ERROR, false);
            mResolvingPermissions = savedInstanceState.getBoolean(KEY_RESOLVING_PERMISSIONS, false);
            mResolvingSettings = savedInstanceState.getBoolean(KEY_RESOLVING_SETTINGS, false);
            mSettingsAsked = savedInstanceState.getBoolean(KEY_SETTINGS_ASKED, false);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mCallback = resolveTarget(getTargetTag());
    }

    @Override
    public void onResume() {
        super.onResume();
        connect();
    }

    @Override
    public void onPause() {
        super.onPause();
        disconnect();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_RESOLVING_ERROR, mResolvingError);
        outState.putBoolean(KEY_RESOLVING_SETTINGS, mResolvingSettings);
        outState.putBoolean(KEY_RESOLVING_PERMISSIONS, mResolvingPermissions);
        outState.putBoolean(KEY_SETTINGS_ASKED, mSettingsAsked);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RQ_CLIENT_RESOLUTION) {
            mResolvingError = false;
            if (resultCode == Activity.RESULT_OK) {
                connect();
            }
            else {
                notifyError(LocationError.SERVICE_DISABLED);
            }
        }

        else if (requestCode == RQ_SETTINGS_RESOLUTION) {
            mResolvingSettings = false;
            mSettingsAsked = true;

            if (isConnected()) {
                initLocationService();
            }
            else {
                connect();
            }

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if ((requestCode & 0xFF) == RQ_PERMISSIONS) {
            mResolvingPermissions = false;
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    notifyError(LocationError.PERMISSIONS_DENIED);
                    return;
                }
            }

            if (isConnected()) {
                initLocationService();
            }
            else {
                connect();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mListeners.clear();
        disconnect();
    }

    // --- public

    public void addListener(ILocationListener listener) {
        mListeners.add(listener);
        connect();
    }

    public void removeListener(ILocationListener listener) {
        mListeners.remove(listener);
        disconnect();
    }


    // --- GoogleApiClient.ConnectionCallbacks

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (!hasPermissions()) {
            requestPermissions();
            return;
        }
        initLocationService();
    }

    @Override
    public void onConnectionSuspended(int i) {
        /* do nothing */
    }

    // --- GoogleApiClient.OnConnectionFailedListener

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (mResolvingError) {
            return;
        }

        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(getActivity(), RQ_CLIENT_RESOLUTION);
                mResolvingError = true;
            } catch (Exception e) {
                Log.e(TAG, "Error!", e);
                notifyError(LocationError.SERVICE_DISABLED);
            }
        }
        else {
            showErrorDialog(connectionResult.getErrorCode());
            notifyError(LocationError.SERVICE_DISABLED);
        }
    }

    // --- private

    private void connect() {
        if (mListeners.size() == 0) {
            return;
        }

        if (null == mClient) {
            mClient = createClient();
        }
        if (!mClient.isConnecting() && !mClient.isConnected()) {
            mClient.connect();
        }
    }

    private void disconnect() {
        if (mListeners.size() != 0) {
            return;
        }

        if (null != mSettingsPendingResult && mSettingsPendingResult.isCanceled()) {
            mSettingsPendingResult.cancel();
        }
        if (null != mClient && mClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mClient, mLocationListener);
        }
        if (null != mClient && (mClient.isConnected() || mClient.isConnecting())) {
            mClient.disconnect();
        }
        mClient = null;
        mSettingsPendingResult = null;
    }

    private GoogleApiClient createClient() {
        return new GoogleApiClient.Builder(getContext())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    private boolean isConnected() {
        return null != mClient && mClient.isConnected();
    }

    private void initLocationService() {
        if (mResolvingSettings) {
            return;
        }

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(getLocationRequest());
        mSettingsPendingResult = LocationServices.SettingsApi
                .checkLocationSettings(mClient, builder.build());

        mSettingsPendingResult.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        if (null != mClient && mClient.isConnected() &&
                                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            LocationServices.FusedLocationApi.requestLocationUpdates(mClient, getLocationRequest(), mLocationListener);
                        }
                        mCallback.onLocationServicesReady();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            if (mResolvingSettings) {
                                break;
                            }
                            if (mSettingsAsked) {
                                notifyError(LocationError.SERVICE_DISABLED);
                                break;
                            }
                            status.startResolutionForResult(getActivity(), RQ_SETTINGS_RESOLUTION);
                            mResolvingSettings = true;
                        } catch (Exception e) {
                            Log.e(TAG, "Error!", e);
                            notifyError(LocationError.SERVICE_DISABLED);
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        notifyError(LocationError.SERVICE_DISABLED);
                        break;
                }
                mSettingsAsked = false;
                mSettingsPendingResult = null;
            }
        });
    }

    private LocationRequest getLocationRequest() {
        if (null == mLocationRequest) {
            mLocationRequest = createLocationRequest();
        }
        return mLocationRequest;
    }

    private LocationRequest createLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    private ICallback resolveTarget(String tag) {
        if (TextUtils.isEmpty(tag)) {
            if (getActivity() instanceof ICallback) {
                return (ICallback) getActivity();
            }
            return EMPTY_CALLBACKS;
        }
        else {
            Fragment targetFragment = getFragmentManager().findFragmentByTag(tag);
            if (null == targetFragment || !(targetFragment instanceof ICallback)) {
                return EMPTY_CALLBACKS;
            }
            return (ICallback) targetFragment;
        }
    }

    private String getTargetTag() {
        if (null == mTag) {
            mTag = getArguments().getString(ARG_TAG, "");
        }
        return mTag;
    }

    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment.newInstance(errorCode, getTag()).show(
                getFragmentManager(), ErrorDialogFragment.TAG
        );
    }

    private boolean hasPermissions() {

        boolean fineLocation = ContextCompat.checkSelfPermission(
                getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        boolean coarseLocation = ContextCompat.checkSelfPermission(
                getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        return fineLocation && coarseLocation;
    }

    private void requestPermissions() {
        if (!mResolvingPermissions) {
            mResolvingPermissions = true;
            requestPermissions(new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    RQ_PERMISSIONS
            );
        }
    }

    private void notifyError(LocationError error) {
        mCallback.onLocationError(error);
    }

    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            for (ILocationListener listener : mListeners) {
                listener.onGotLocation(location);
            }
        }
    };

    public static class ErrorDialogFragment extends DialogFragment {
        public static final String TAG = ErrorDialogFragment.class.getSimpleName();

        private static final String ARG_CODE = "CODE";
        private static final String ARG_TAG = "TAG";

        public static ErrorDialogFragment newInstance(int errorCode, String targetTag) {
            Bundle args = new Bundle();
            args.putInt(ARG_CODE, errorCode);
            args.putString(ARG_TAG, targetTag);
            ErrorDialogFragment fragment = new ErrorDialogFragment();
            fragment.setArguments(args);
            return fragment;
        }

        public ErrorDialogFragment() { }

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(ARG_CODE);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, RQ_CLIENT_RESOLUTION);
        }
    }

    public interface ICallback {
        void onLocationServicesReady();

        void onLocationError(LocationError error);
    }

    private static final ICallback EMPTY_CALLBACKS = new ICallback() {
        @Override
        public void onLocationServicesReady() { log(); }

        @Override
        public void onLocationError(LocationError error) { log(); }

        private void log() {
            Log.e(TAG, "Location callback is not specified.");
            Log.e(TAG, "Target fragment or activity must implement LocationFragment.ICallback interface!");
        }
    };

}