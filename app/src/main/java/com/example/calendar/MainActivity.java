package com.example.calendar;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import com.example.calendar.models.Record;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();

    private MenuItem searchItem;

    private boolean month = false;

    public static int NEW_NOTE = 556;
    public static int EDIT_NOTE = 557;
    public static int DELETE_NOTE = 558;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_records, new RecordsFragment(), "visible_fragment");
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        fragmentTransaction.commit();

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.main_fab);
        fab.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, RecordActivity.class);
            intent.putExtra(RecordActivity.ACTION, RecordActivity.CREATE_NOTE);
            Fragment fragment = getSupportFragmentManager().findFragmentByTag("visible_fragment");
            if (fragment instanceof CalendarFragment) {
                intent.putExtra(RecordActivity.CHOSE_DATE, ((CalendarFragment) fragment).calendarView.getFirstSelectedDate());
            }
            startActivityForResult(intent, NEW_NOTE);
        });
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
                record.setDate((Date) data.getSerializableExtra(RecordActivity.DATE));
                record.setPhotos(data.getStringArrayListExtra(RecordActivity.PHOTOS));

                newRecordRef.set(record).addOnCompleteListener(task -> {
                   if (task.isSuccessful()) {
                       Log.v(TAG, "Say me this shit");

                       Log.v(TAG, "Okay, cool.");
                       Fragment fragment = getSupportFragmentManager().findFragmentByTag("visible_fragment");

                       if (fragment instanceof RecordsFragment) {
                           Collections.reverse(record.getPhotos());
                            ((RecordsFragment) fragment).recordsAdapter.addRecord(record);
                       } else if (fragment instanceof CalendarFragment) {
                           Collections.reverse(record.getPhotos());
                           ((CalendarFragment) fragment).recordsAdapter.addRecord(record,
                                   ((CalendarFragment) fragment).calendarView.getFirstSelectedDate());
                           ((CalendarFragment) fragment).updateCalendarDots();
                       }

                   } else {
                        Log.v(TAG, "Some shit happened");
                   }
                });
            }
        } else if (requestCode == MainActivity.EDIT_NOTE) {
            String noteId;
            int position;
            ArrayList<String> photoURIs;
            if (resultCode == Activity.RESULT_OK) {
                noteId = data.getStringExtra(RecordActivity.NOTE_ID);
                position = data.getExtras().getInt(RecordActivity.NOTE_POSITION);
                String title = data.getStringExtra(RecordActivity.TITLE);
                String recordText = data.getStringExtra(RecordActivity.RECORD);
                Date date = (Date) data.getSerializableExtra(RecordActivity.DATE);
                photoURIs = new ArrayList<>(data.getStringArrayListExtra(RecordActivity.PHOTOS));

                FirebaseFirestore db = FirebaseFirestore.getInstance();

                DocumentReference dR = db.collection("records").document(noteId);
                dR.update("title", title, "text", recordText,
                        "date", date).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.v(MainActivity.TAG, "Note has been updated");
                        Record record = new Record();
                        record.setTitle(title);
                        record.setText(recordText);
                        record.setDate(date);
                        record.setPhotos(photoURIs);

                        Fragment fragment = getSupportFragmentManager().findFragmentByTag("visible_fragment");
                        if (fragment instanceof RecordsFragment) {
                            ((RecordsFragment) fragment).recordsAdapter.updateRecord(position, record);
                        } else if (fragment instanceof CalendarFragment) {
                            ((CalendarFragment) fragment).recordsAdapter.updateRecord
                                    (position, record, ((CalendarFragment) fragment)
                                            .calendarView.getFirstSelectedDate());
                            ((CalendarFragment) fragment).updateCalendarDots();
                        }
                    } else {
                        Log.v(MainActivity.TAG, "Note update has been failed");
                    }
                });
            } else if (resultCode == MainActivity.DELETE_NOTE) {
                noteId = data.getStringExtra(RecordActivity.NOTE_ID);
                position = data.getExtras().getInt(RecordActivity.NOTE_POSITION);
                photoURIs = data.getStringArrayListExtra(RecordActivity.PHOTOS);

                Log.v(MainActivity.TAG, "DAMN, this shit works!");

                FirebaseFirestore db = FirebaseFirestore.getInstance();

                DocumentReference docRef = db.collection("records").document(noteId);
                docRef.delete().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (photoURIs != null) {
                            for (String photoURI : photoURIs) {
                                StorageReference storageRef = FirebaseStorage.getInstance()
                                        .getReferenceFromUrl(photoURI);

                                storageRef.delete().addOnSuccessListener(aVoid -> {
                                    Log.v(MainActivity.TAG, "Photo has been deleted successfully");
                                }).addOnFailureListener(e -> {
                                    Log.v(MainActivity.TAG, "Photo has not been deleted, error: " + e);
                                });
                            }
                        }

                        Fragment fragment = getSupportFragmentManager().findFragmentByTag("visible_fragment");
                        if (fragment instanceof RecordsFragment) {
                            ((RecordsFragment) fragment).recordsAdapter.deleteRecord(position);
                        } else if (fragment instanceof CalendarFragment) {
                            ((CalendarFragment) fragment).recordsAdapter.deleteRecord(position);
                            ((CalendarFragment) fragment).updateCalendarDots();
                        }
                    }
                });
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

       searchItem = menu.findItem(R.id.action_search);

        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                RecordsFragment recordsFragment = (RecordsFragment)
                        getSupportFragmentManager().findFragmentById(R.id.fragment_records);

                if (recordsFragment != null && recordsFragment.isAdded()) {
                    recordsFragment.recordsAdapter.getFilter().filter(newText);
                }
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_month:
                if (month) {
                    Drawable drawable = ResourcesCompat.getDrawable(getResources(),
                            R.drawable.ic_apps_black_24dp, null);

                    item.setIcon(drawable);
                    searchItem.setVisible(true);
                } else {
                    Drawable drawable = ResourcesCompat.getDrawable(getResources(),
                            R.drawable.ic_view_list_black_24dp, null);
                    item.setIcon(drawable);
                    searchItem.setVisible(false);
                }
                changeFragments(month);
                break;
            case R.id.action_refresh:
                Fragment fragment = getSupportFragmentManager().findFragmentByTag("visible_fragment");
                if (fragment instanceof RecordsFragment) {
                    ((RecordsFragment) fragment).recordsAdapter.clearRecords();
                    ((RecordsFragment) fragment).getNotes();
                }
                else if (fragment instanceof CalendarFragment) {
                    ((CalendarFragment) fragment).recordsAdapter.clearRecords();
                    ((CalendarFragment) fragment).getNotes();
                    ((CalendarFragment) fragment).updateCalendarDots();
                }
                break;
            case R.id.action_sign_out:
                signOut();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void changeFragments(boolean isMonth) {
        if (isMonth) {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.fragment_records, new RecordsFragment(), "visible_fragment");
            fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            fragmentTransaction.commit();

            month = false;
        } else {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            CalendarFragment calendarFragment = new CalendarFragment();
            ft.replace(R.id.fragment_records, calendarFragment, "visible_fragment");
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();

            month = true;
        }
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
