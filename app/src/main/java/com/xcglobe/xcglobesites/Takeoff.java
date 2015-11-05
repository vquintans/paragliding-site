package com.xcglobe.xcglobesites;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by kafol on 5.11.2015.
 */
public class Takeoff {
    public String id;
    public double lat;
    public double lon;
    public String name;
    public int flights;
    boolean drawn = false;

    public LatLng toPoint() {
        return new LatLng(lat,lon);
    }
}
