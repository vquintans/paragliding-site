package com.xcglobe.xcglobesites;

import android.content.Context;
import android.content.SharedPreferences;

public class Util {

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

    public static int get(Context c, String key) {
        SharedPreferences sharedPref = c.getSharedPreferences(prefkey, Context.MODE_PRIVATE);
        return sharedPref.getInt(key, -1);
    }

    public static String getString(Context c, String key) {
        SharedPreferences sharedPref = c.getSharedPreferences(prefkey, Context.MODE_PRIVATE);
        return sharedPref.getString(key, "");
    }
}
