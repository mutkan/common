package com.github.romychab.common.locations;

import android.location.Location;


public interface ILocationListener {
    void onGotLocation(Location location);
    void onLocationError(LocationError error);
}
