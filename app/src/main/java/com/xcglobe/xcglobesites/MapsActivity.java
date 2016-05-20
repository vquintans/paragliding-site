package com.xcglobe.xcglobesites;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
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
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;

public class MapsActivity extends ActionBarActivity implements OnMapReadyCallback {

    private static final CharSequence[] MAP_TYPE_ITEMS =
            {"Road Map", "Hybrid", "Satellite", "Terrain"};
    private static float minZoom = 9.0f;
    private GoogleMap mMap;
    private JSONArray data;
    private Takeoff[] sites;
    private HashMap<String, Takeoff> markers;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        uid = Util.getString(this, "uid");
        if(!uid.isEmpty()) {
            Util.getSites(this, uid);
        }

        markers = new HashMap<>();
    }

    @Override
    public void onResume() {
        super.onResume();

        if(mMap != null) {
            mMap.clear();
            markers.clear();
            if(sites != null) {
                for(Takeoff t:sites) {
                    if(t != null) {
                        t.drawn = false;
                    }
                }
            }
            drawAirspace();
            drawPointsInBounds();
            drawLiveTrack();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_items, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item1:
                showMapTypeSelectorDialog();
                break;
            case R.id.item2:
                startActivity(new Intent(this, GetSites.class));
                break;
            case R.id.item3:
                startActivity(new Intent(this, Settings.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        loadData();

        int maptype = Util.get(this, "maptype");
        maptype = maptype == -1 ? GoogleMap.MAP_TYPE_TERRAIN : maptype;

        zoomTo(new LatLng(46.1221, 14.8153), 11, 0);

        mMap.setMapType(maptype);
        mMap.setMyLocationEnabled(true);
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                if (cameraPosition.zoom < minZoom) {
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(minZoom));
                } else {
                    drawPointsInBounds();
                }
            }
        });
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                try {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://xcglobe.com/olc/index.php/catalog/#sa&flights&" + markers.get(marker.getId()).id + "&&&&site_latest"));
                    startActivity(browserIntent);
                } catch (Exception e) {
                    Log.e("XCSites", "Info window click", e);
                }
            }
        });

        zoomToMyPos();
    }

    private void drawPointsInBounds() {
        LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        if(mMap.getCameraPosition().zoom >= minZoom) {
            fetchData(bounds);
        }
    }

    private void drawLiveTrack() {
        if (Util.getBoolean(this, "showlivetrack", true)) {
            Log.i("LiveTrack", "Fetching...");
            RequestQueue queue = Volley.newRequestQueue(this);
            StringRequest stringRequest = new StringRequest(Request.Method.GET, "http://xcglobe.com/olc/index.php/livetrack/get_lives_txt?full=1", new Response.Listener<String>() {
                @Override
                public void onResponse(String r) {
                    Log.i("LiveTrack", "Parsing...");
                    try {
                        String response = new String(r.getBytes("ISO-8859-1"), "UTF-8");
                        String[] lines = response.split("\n");
                        for (String line : lines) {
                            String[] tokens = line.split("#");
                            if (tokens.length > 8) {
                                String pilot = tokens[8];
                                double lat = new Double(tokens[2]);
                                double lon = new Double(tokens[3]);
                                drawPilot(new LatLng(lat, lon), pilot);
                            }
                        }
                    } catch (Exception e) {
                        Log.e("Paragliding sites", "Live tracking", e);
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("LiveTrack", "FetchError", error);
                }
            });
            queue.add(stringRequest);
        } else {
            Log.e("LiveTrack", "disabled");
        }
    }

    private void drawPilot(LatLng pos, String pilot) {
        MarkerOptions marker = new MarkerOptions().position(pos);
        marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.livetrack));

        Marker m = mMap.addMarker(marker);
        m.setTitle(pilot);
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
        int icon = R.drawable.dot;
        if(t.flights < Util.getInt(this,"popular",50)) {
            icon = R.drawable.small;
        }
        if(t.flights < Util.getInt(this, "minflights", 1)) {
            return;
        }
        if(Util.get(this,t.id) > 0) {
            icon = R.drawable.visited;
            t.visited = true;
        }

        if (Util.getBoolean(this, "showvisited", false) && !t.visited) return;
        if (Util.getBoolean(this, "hidevisited", false) && t.visited) return;

        int minalt = Util.getInt(this, "minalt", 0);
        int maxalt = Util.getInt(this, "maxalt", 0);
        int avg = Util.getInt(this, "avg", 0);
        if (minalt > 0 && t.alt < minalt) return;
        if (maxalt > 0 && t.alt > maxalt) return;
        if (avg > 0 && t.avg < avg) return;

        if(Util.getBoolean(this, "nowind", true)) {

        } else {
            /*
            if(Util.getBoolean(this, "n", true) && !t.n) return;
            if(Util.getBoolean(this, "e", true) && !t.e) return;
            if(Util.getBoolean(this, "w", true) && !t.w) return;
            if(Util.getBoolean(this, "s", true) && !t.s) return;

            if(Util.getBoolean(this, "ne", true) && !t.ne) return;
            if(Util.getBoolean(this, "nw", true) && !t.nw) return;
            if(Util.getBoolean(this, "se", true) && !t.se) return;
            if(Util.getBoolean(this, "sw", true) && !t.sw) return;
            */

            boolean show = false;

            if(Util.getBoolean(this, "n", true) && t.n) show = true;
            if(Util.getBoolean(this, "e", true) && t.e) show = true;
            if(Util.getBoolean(this, "w", true) && t.w) show = true;
            if(Util.getBoolean(this, "s", true) && t.s) show = true;

            if(Util.getBoolean(this, "ne", true) && t.ne) show = true;
            if(Util.getBoolean(this, "nw", true) && t.nw) show = true;
            if(Util.getBoolean(this, "se", true) && t.se) show = true;
            if(Util.getBoolean(this, "sw", true) && t.sw) show = true;

            if(!show) return;
        }

        marker.icon(BitmapDescriptorFactory.fromResource(icon));

        // adding marker
        t.drawn = true;
        Marker m = mMap.addMarker(marker);
        m.setTitle(t.name.isEmpty() ? "?" : t.name);
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
        zoomTo(loc, 14, 0);
    }

    private void zoomTo(LatLng loc, float zoom, float bearing) {
        //map.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 13));
        if(mMap == null) {
            Log.e("xcg", "Can't zoom to "+loc+", map is null.");
        }

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(loc)        // Sets the center of the map to location user
                .zoom(zoom)   // Sets the zoom
                .bearing(bearing)   // Sets the orientation of the camera to east
                .tilt(0)  // Sets the tilt of the camera to 30 degrees
                .build();           // Creates a CameraPosition from the builder
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private void loadData() {
        //new LoadDataTask().execute();
        drawTakeoffs();
        drawAirspace();
        drawLiveTrack();
    }

    /*
    class LoadDataTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            drawTakeoffs();
            return null;
        }
    }
    */

    public JSONArray getTakeoffs() {
        File f = Util.getSitesFile(this);
        if (f.exists()) {
            Log.i("XCG", "Loaded sites from cache");
            return Util.loadJSONFromFile(this, f);
        } else {
            Log.i("XCG", "Loaded sites from assets");
            return Util.loadJSONFromAsset(this, "xcg.json");
        }
    }

    private void drawTakeoffs() {
        try {
            data = getTakeoffs();
            sites = new Takeoff[data.length()];
            for (int i = 0; i < data.length(); i++) {
                JSONObject p = data.getJSONObject(i);

                Takeoff t = new Takeoff();
                t.name = p.getString("name");
                t.lat = p.getDouble("lat");
                t.lon = p.getDouble("lon");
                t.flights = p.getInt("flights");
                t.id = p.getString("id");

                try {
                    t.alt = p.getInt("alt");
                    t.avg = p.getDouble("avg");
                } catch (Exception e) {

                }

                String wind = p.getString("wind");
                String[] winds = wind.split(",");
                for(String w : winds) {
                    if(w.equals("N")) t.n = true;
                    if(w.equals("E")) t.e = true;
                    if(w.equals("W")) t.w = true;
                    if(w.equals("S")) t.s = true;

                    if(w.equals("NE")) t.ne = true;
                    if(w.equals("NW")) t.nw = true;
                    if(w.equals("SE")) t.se = true;
                    if(w.equals("SW")) t.sw = true;
                }

                sites[i] = t;
            }
        } catch (Exception e) {
            Log.e("XCGlobe", "data", e);
        }
    }

    private void drawAirspace() {
        if(Util.getBoolean(this, "showairspace")) {
            try {
                JSONArray a = Util.loadJSONFromAsset(this, "airspace.json");
                for (int i = 0; i < a.length(); i++) {
                    JSONObject p = a.getJSONObject(i);

                    addPolygon(p.getJSONArray("points"), p.getInt("alt"));
                }
            } catch (Exception e) {
                Log.e("XCGlobe", "airspace", e);
            }
        }
    }

    private void addPolygon(JSONArray pts,int alt) {
        PolygonOptions opts = new PolygonOptions().strokeColor(Color.RED);
        if(alt == 0) {
            opts.fillColor(Color.argb(40, 255, 0, 0));
        } else {
            opts.fillColor(Color.argb((4500-alt)/60,0,0,255));
        }

        try {
            for(int i = 0;i<pts.length();i++) {
                if(i%2 == 1) {
                    opts.add(new LatLng(pts.getDouble(i-1),pts.getDouble(i)));
                }
            }
        } catch (Exception e) {
            Log.e("XCGlobe","addPolygon",e);
        }

        Polygon polygon = mMap.addPolygon(opts);
    }

    private void showMapTypeSelectorDialog() {
        // Prepare the dialog by setting up a Builder.
        final String fDialogTitle = "Select Map Type";
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(fDialogTitle);

        // Find the current map type to pre-check the item representing the current state.
        int checkItem = 0;
        switch(mMap.getMapType()) {
            case GoogleMap.MAP_TYPE_HYBRID: checkItem = 1; break;
            case GoogleMap.MAP_TYPE_SATELLITE: checkItem = 2; break;
            case GoogleMap.MAP_TYPE_TERRAIN: checkItem = 3; break;
        }

        // Add an OnClickListener to the dialog, so that the selection will be handled.
        builder.setSingleChoiceItems(
                MAP_TYPE_ITEMS,
                checkItem,
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int item) {
                        // Locally create a finalised object.

                        // Perform an action depending on which item was selected.
                        switch (item) {
                            case 1:
                                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                                break;
                            case 2:
                                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                                break;
                            case 3:
                                mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                                break;
                            default:
                                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        }
                        Util.save(getApplicationContext(), "maptype", mMap.getMapType());
                        dialog.dismiss();
                    }
                }
        );

        // Build the dialog and show it.
        AlertDialog fMapTypeDialog = builder.create();
        fMapTypeDialog.setCanceledOnTouchOutside(true);
        fMapTypeDialog.show();
    }
}
