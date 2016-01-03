package com.xcglobe.xcglobesites;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

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

                Util.getSites(getApplicationContext(), uid);
                finish();
            }
        });
    }


}
