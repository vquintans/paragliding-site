package com.xcglobe.xcglobesites;

import com.google.android.gms.maps.model.LatLng;

public class Takeoff {
    public String id;
    public double lat;
    public double lon;
    public String name;
    public int flights, alt;
    public double avg;
    boolean drawn = false;
    boolean visited = false;

    boolean n,e,w,s,ne,nw,se,sw;

    public LatLng toPoint() {
        return new LatLng(lat,lon);
    }
}
