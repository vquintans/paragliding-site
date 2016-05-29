package com.xcglobe.xcglobesites;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;

import java.util.ArrayList;

/**
 * Created by Jean on 28.5.2016.
 */
public class Pilot {
    String name;
    int alt;
    ArrayList<LatLng> track;
    LatLng loc;
    int id;
    int timestamp;
    double speed;
    Marker marker;
    Polyline line;

    public Pilot(String name) {
        this.name = name;
    }

    public long getAge() {
        long unixTime = System.currentTimeMillis() / 1000L;

        return unixTime - timestamp;
    }
}
