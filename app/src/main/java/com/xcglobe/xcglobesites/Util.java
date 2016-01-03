package com.xcglobe.xcglobesites;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

    public static final String SITES_URL = "https://kafol.net/code/xcg/json.php";

    public static String prefkey = "xcg";

    public static void save(Context c, String key, int val) {
        SharedPreferences sharedPref = c.getSharedPreferences(prefkey, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(key, val);
        editor.commit();
    }

    public static void save(Context c, String key, String val) {
        SharedPreferences sharedPref = c.getSharedPreferences(prefkey, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, val);
        editor.commit();
    }

    public static void save(Context c, String key, boolean val) {
        SharedPreferences sharedPref = c.getSharedPreferences(prefkey, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(key, val);
        editor.commit();
    }

    public static int get(Context c, String key) {
        SharedPreferences sharedPref = c.getSharedPreferences(prefkey, Context.MODE_PRIVATE);
        return sharedPref.getInt(key, -1);
    }

    public static String getString(Context c, String key) {
        SharedPreferences sharedPref = c.getSharedPreferences(prefkey, Context.MODE_PRIVATE);
        return sharedPref.getString(key, "");
    }

    public static boolean getBoolean(Context c, String key, boolean val) {
        SharedPreferences sharedPref = c.getSharedPreferences(prefkey, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(key, val);
    }

    public static boolean getBoolean(Context c, String key) { {
        return getBoolean(c, key, false);
    }}

    public static int getInt(Context c, String key, int i) {
        SharedPreferences sharedPref = c.getSharedPreferences(prefkey, Context.MODE_PRIVATE);
        return sharedPref.getInt(key, i);
    }

    public static void getSites(final Context ctx, String uid) {
        RequestQueue queue = Volley.newRequestQueue(ctx);
        String url = "http://xcglobe.com/olc/index.php/catalog/ajax_get_item_prop?prop=psites&id="+uid+"&tv=pilots&y=&ww=0";

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        //mTextView.setText(response);

                        Pattern p = Pattern.compile("var points_arr=([^;]+);");
                        Matcher m = p.matcher(response);
                        while (m.find()) { // Find each match in turn; String can't do this.
                            String pts = m.group(1); // Access a submatch group; String can't do this.
                            //mTextView.setText(pts);
                            saveSites(ctx, pts);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //mTextView.setText("That didn't work!");
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private static void saveSites(Context ctx, String pts) {
        try {
            JSONArray a = new JSONArray(pts);
            for(int i = 0; i < a.length(); i++) {
                JSONArray p = a.getJSONArray(i);
                Util.save(ctx, p.getString(0), 1);
            }
        } catch(Exception e) {
            Log.e("getSites", "save", e);
        }
    }

    public static JSONArray loadJSONFromAsset(Context ctx, String filename) {
        JSONArray json = new JSONArray();
        try {
            InputStream is = ctx.getAssets().open(filename);
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

    public static JSONArray loadJSONFromFile(Context ctx, File file) {
        JSONArray json = new JSONArray();
        try {
            FileInputStream is = new FileInputStream(file);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String str = new String(buffer, "UTF-8");
            json = new JSONArray(str);
        } catch (Exception ex) {
            Log.e("XCGlobe", "loadjson from file", ex);
        }
        return json;
    }

    public static File getSitesFile(Context context) {
        File cacheDir = context.getCacheDir();
        return new File(cacheDir, "sites_wind.json");
    }
}
