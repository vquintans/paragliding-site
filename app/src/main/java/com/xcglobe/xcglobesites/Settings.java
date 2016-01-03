package com.xcglobe.xcglobesites;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Settings extends AppCompatActivity {

    protected ProgressBar bar;
    protected TextView modified;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_settings);

        bar = (ProgressBar) findViewById(R.id.progressBar);
        modified = (TextView) findViewById(R.id.modified);

        CheckBox airspace = (CheckBox) findViewById(R.id.showairspace);
        boolean showairs = Util.getBoolean(this, "showairspace");
        airspace.setChecked(showairs);

        airspace.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Util.save(getApplicationContext(), "showairspace", isChecked);
            }
        });

        CheckBox n = (CheckBox) findViewById(R.id.n);
        n.setChecked(Util.getBoolean(this, "n", true));
        n.setTag("n");

        CheckBox e = (CheckBox) findViewById(R.id.e);
        e.setChecked(Util.getBoolean(this, "e", true));
        e.setTag("e");

        CheckBox w = (CheckBox) findViewById(R.id.w);
        w.setChecked(Util.getBoolean(this, "w", true));
        w.setTag("w");

        CheckBox s = (CheckBox) findViewById(R.id.s);
        s.setChecked(Util.getBoolean(this, "s", true));
        s.setTag("s");

        CheckBox ne = (CheckBox) findViewById(R.id.ne);
        ne.setChecked(Util.getBoolean(this, "ne", true));
        ne.setTag("ne");

        CheckBox nw = (CheckBox) findViewById(R.id.nw);
        nw.setChecked(Util.getBoolean(this, "nw", true));
        nw.setTag("nw");

        CheckBox se = (CheckBox) findViewById(R.id.se);
        se.setChecked(Util.getBoolean(this, "se", true));
        se.setTag("se");

        CheckBox sw = (CheckBox) findViewById(R.id.sw);
        sw.setChecked(Util.getBoolean(this, "sw", true));
        sw.setTag("sw");

        CheckBox nowind = (CheckBox) findViewById(R.id.wind);
        nowind.setChecked(Util.getBoolean(this, "nowind", true));
        nowind.setTag("nowind");


        CheckBox.OnCheckedChangeListener cb_wind = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Util.save(getApplicationContext(), buttonView.getTag().toString(), isChecked);
            }
        };
        n.setOnCheckedChangeListener(cb_wind);
        e.setOnCheckedChangeListener(cb_wind);
        w.setOnCheckedChangeListener(cb_wind);
        s.setOnCheckedChangeListener(cb_wind);
        ne.setOnCheckedChangeListener(cb_wind);
        nw.setOnCheckedChangeListener(cb_wind);
        se.setOnCheckedChangeListener(cb_wind);
        sw.setOnCheckedChangeListener(cb_wind);
        nowind.setOnCheckedChangeListener(cb_wind);

        EditText numpopular = (EditText) findViewById(R.id.numpopular);
        numpopular.setText(Util.getInt(this, "popular", 50) + "");
        numpopular.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    Util.save(getApplicationContext(), "popular", new Integer(s.toString()));
                } catch(Exception e) {
                    Log.e("Settings","edit num popular",e);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        EditText minflights = (EditText) findViewById(R.id.minflghts);
        minflights.setText(Util.getInt(this, "minflights", 1) + "");
        minflights.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    Util.save(getApplicationContext(), "minflights", new Integer(s.toString()));
                } catch (Exception e) {
                    Log.e("Settings", "edit min flights", e);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        findViewById(R.id.download).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                download();
                v.setEnabled(false);
            }
        });

        updateModifiedDate();
    }

    public void download() {
        new DownloadSitesTask().execute();
    }

    public void updateModifiedDate() {
        File f = Util.getSitesFile(getApplicationContext());
        if(f.exists()) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd. MMM yyyy HH:mm");
            modified.setText(""+sdf.format(new Date(f.lastModified())));
        } else {
            modified.setText("Not yet downloaded");
        }
    }

    class DownloadSitesTask extends AsyncTask<Void,Integer,Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                URLConnection cn = new URL(Util.SITES_URL).openConnection();
                cn.connect();
                InputStream stream = cn.getInputStream();

                FileOutputStream out = new FileOutputStream(Util.getSitesFile(getApplicationContext()));
                byte buf[] = new byte[16384];
                int total = 0;
                do {
                    int numread = stream.read(buf);
                    total += numread;
                    if (numread <= 0) break;
                    out.write(buf, 0, numread);

                    publishProgress(total, cn.getContentLength());
                } while (true);
            } catch (Exception e) {
                Log.e("Util","download sites",e);
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            bar.setMax(values[1]);
            bar.setProgress(values[0]);
            Log.d("xcg download",values[0]+"/"+values[1]+" done");
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            findViewById(R.id.download).setEnabled(true);
            updateModifiedDate();

            bar.setProgress(0);
        }
    }

}
