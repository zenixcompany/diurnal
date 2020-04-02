package com.example.calendar;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class RecordActivity extends AppCompatActivity {
    public static final String ACTION = "ACTION";
    public static final String CREATE_NOTE = "CREATE_NOTE";
    public static final String EDIT_NOTE = "EDIT_NOTE";

    public static final String TITLE = "TITLE";
    public static final String RECORD = "RECORD";

    // false - CREATE_NOTE, true - EDIT_NOTE
    private boolean action = false;

    private Intent intent;

    private EditText titleView;
    private EditText recordView;

    private String title;
    private String record;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        titleView = findViewById(R.id.recordActivity_title);
        recordView = findViewById(R.id.recordActivity_text);

        title = titleView.getText().toString();
        record = recordView.getText().toString();

        intent = getIntent();
        if (intent.getExtras() != null) {
            String actionStr = intent.getExtras().getString(ACTION);

            if (actionStr.contentEquals(CREATE_NOTE)) {
                action = false;
            }
            else if (actionStr.contentEquals(EDIT_NOTE)) {
                action = true;
            }
        }


        Toolbar toolbar = findViewById(R.id.recordActivity_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

    }

    @Override
    public void onBackPressed() {
        if (action) {

        } else {
            if (checkChanges()) {
                intent.putExtra(TITLE, titleView.getText().toString());
                intent.putExtra(RECORD, recordView.getText().toString());

                setResult(RESULT_OK, intent);
            }
        }

        super.onBackPressed();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_record, menu);

        return true;
    }

    private boolean checkChanges() {
        // true if changes exist
        return !title.contentEquals(titleView.getText().toString()) ||
                !record.contentEquals(recordView.getText().toString());
    }
}
