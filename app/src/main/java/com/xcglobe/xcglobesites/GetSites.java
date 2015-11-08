package com.xcglobe.xcglobesites;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetSites extends AppCompatActivity {

    private TextView mTextView;
    private EditText euid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_get_sites);

        mTextView = (TextView) findViewById(R.id.textView2);
        euid = (EditText) findViewById(R.id.uid);

        euid.setText(Util.getString(this, "uid"));
        euid.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Util.save(getApplicationContext(), "uid", s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String uid = euid.getText().toString();

                RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
                String url = "http://xcglobe.com/olc/index.php/catalog/ajax_get_item_prop?prop=psites&id="+uid+"&tv=pilots&y=&ww=0";

                // Request a string response from the provided URL.
                StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                // Display the first 500 characters of the response string.
                                mTextView.setText(response);

                                Pattern p = Pattern.compile("var points_arr=([^;]+);");
                                Matcher m = p.matcher(response);
                                while (m.find()) { // Find each match in turn; String can't do this.
                                    String pts = m.group(1); // Access a submatch group; String can't do this.
                                    mTextView.setText(pts);
                                    saveSites(pts);
                                }
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        mTextView.setText("That didn't work!");
                    }
                });
                // Add the request to the RequestQueue.
                queue.add(stringRequest);
            }
        });
    }

    private void saveSites(String pts) {
        try {
            JSONArray a = new JSONArray(pts);
            for(int i = 0; i < a.length(); i++) {
                JSONArray p = a.getJSONArray(i);
                Util.save(this, p.getString(0), 1);
            }
            finish();
        } catch(Exception e) {
            Log.e("getSites", "save", e);
        }
    }

}
