package com.example.calendar;

import android.content.Intent;
import android.os.Bundle;

import com.example.calendar.models.Record;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;

import java.util.Date;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();

    public static final String USER_EMAIL = "USER_EMAIL";
    public static final String USER_NAME = "USER_NAME";

    public static int NEW_NOTE = 556;
    public static int EDIT_NOTE = 557;
    public static int DELETE_NOTE = 558;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.main_fab);
        fab.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, RecordActivity.class);
            intent.putExtra(RecordActivity.ACTION, RecordActivity.CREATE_NOTE);
            startActivityForResult(intent, NEW_NOTE);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == NEW_NOTE) {
            if (resultCode == RESULT_OK) {
                Log.v(TAG, data.getStringExtra(RecordActivity.TITLE));
                Log.v(TAG, data.getStringExtra(RecordActivity.RECORD));

                FirebaseFirestore db = FirebaseFirestore.getInstance();
                String user_id = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
                DocumentReference newRecordRef = db.collection("records").document();

                Record record = new Record();
                record.setTitle(data.getStringExtra(RecordActivity.TITLE));
                record.setText(data.getStringExtra(RecordActivity.RECORD));
                record.setNote_id(newRecordRef.getId());
                record.setUser_id(user_id);

                newRecordRef.set(record).addOnCompleteListener(task -> {
                   if (task.isSuccessful()) {
                       Log.v(TAG, "Say me this shit");
                       newRecordRef.get().addOnCompleteListener(task1 -> {
                           Date date = task1.getResult().getDate("date");
                           Log.v(TAG, date.toString());
                           record.setDate(date);

                           Log.v(TAG, "Okay, cool.");
                           RecordsFragment recordsFragment = (RecordsFragment)
                                   getSupportFragmentManager().findFragmentById(R.id.fragment_records);

                           if (recordsFragment != null && recordsFragment.isAdded()) {
                               recordsFragment.recordsAdapter.addRecord(record);
                           }
                       });

                   } else {
                        Log.v(TAG, "Some shit happened");
                   }
                });
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);

        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_month:

                break;
            case R.id.action_sign_out:
                signIn();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void signIn() {
        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
