package com.xcglobe.xcglobesites;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.HashMap;

public class MapsActivity extends ActionBarActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private JSONArray data;
    private Takeoff[] sites;
    private HashMap<String, Takeoff> markers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        markers = new HashMap<>();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        mMap.setMyLocationEnabled(true);
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                fetchData(bounds);
            }
        });
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://xcglobe.com/olc/index.php/catalog/#sa&flights&" + markers.get(marker.getId()).id + "&&&&site_latest"));
                startActivity(browserIntent);
            }
        });

        zoomToMyPos();

        loadData();
    }

    private void fetchData(LatLngBounds bounds) {
        //mMap.clear();
        if(sites != null) {
            for (Takeoff t : sites) {
                if(t != null && !t.drawn) {
                    if (bounds.contains(t.toPoint())) {
                        addMarker(t);
                    }
                }
            }
        }
    }

    private void addMarker(Takeoff t) {
        MarkerOptions marker = new MarkerOptions().position(t.toPoint());

        // Changing marker icon
        marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.dot));

        // adding marker
        t.drawn = true;
        Marker m = mMap.addMarker(marker);
        m.setTitle(t.name);
        m.setSnippet(t.flights + " flights");
        markers.put(m.getId(), t);
    }

    private void zoomToMyPos() {
        Location location = lastKnownLocation();
        if (location != null) {
            zoomTo(new LatLng(location.getLatitude(), location.getLongitude()));
        } else {
            Log.e("Navigacija", "Last known location?");
        }
    }

    private Location lastKnownLocation() {
        LocationManager locmgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locmgr.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
    }

    private void zoomTo(LatLng loc) {
        zoomTo(loc, 0);
    }

    private void zoomTo(LatLng loc, float bearing) {
        //map.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 13));
        if(mMap == null) {
            Log.e("xcg", "Can't zoom to "+loc+", map is null.");
        }

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(loc)        // Sets the center of the map to location user
                .zoom(14)   // Sets the zoom
                .bearing(bearing)   // Sets the orientation of the camera to east
                .tilt(0)  // Sets the tilt of the camera to 30 degrees
                .build();           // Creates a CameraPosition from the builder
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private void loadData() {
        new LoadDataTask().execute();
    }

    class LoadDataTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                data = loadJSONFromAsset();
                sites = new Takeoff[data.length()];
                for(int i = 0; i<data.length();i++) {
                    JSONObject p = data.getJSONObject(i);

                    Takeoff t = new Takeoff();
                    t.name = p.getString("name");
                    t.lat = p.getDouble("lat");
                    t.lon = p.getDouble("lon");
                    t.flights = p.getInt("flights");
                    t.id = p.getString("id");

                    sites[i] = t;
                }
            } catch (Exception e) {
                Log.e("XCGlobe","data",e);
            }
            return null;
        }
    }

    private JSONArray loadJSONFromAsset() {
        JSONArray json = new JSONArray();;
        try {
            InputStream is = getAssets().open("xcg.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String str = new String(buffer, "UTF-8");
            json = new JSONArray(str);
        } catch (Exception ex) {
            Log.e("XCGlobe", "loadjson", ex);
        }
        return json;
    }
}
