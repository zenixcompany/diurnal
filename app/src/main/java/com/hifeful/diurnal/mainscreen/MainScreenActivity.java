package com.hifeful.diurnal.mainscreen;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.hifeful.diurnal.application.MyApplication;
import com.hifeful.diurnal.R;
import com.hifeful.diurnal.data.Record;
import com.hifeful.diurnal.data.User;
import com.hifeful.diurnal.features.PreferenceActivity;
import com.hifeful.diurnal.mainscreen.calendar.CalendarFragment;
import com.hifeful.diurnal.mainscreen.records.RecordsFragment;
import com.hifeful.diurnal.record.RecordActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MainScreenActivity extends AppCompatActivity {
    // Constants
    public static final String TAG = MainScreenActivity.class.getSimpleName();

    public static int NEW_NOTE = 556;
    public static int EDIT_NOTE = 557;
    public static int DELETE_NOTE = 558;

    // UI
    private MenuItem searchItem;
    private SearchView searchView;

    // Variables
    private User mUser;

    private boolean month = false;
    private boolean isSearchViewOpened = false;
    private boolean isSearchViewFocused = true;
    private String mSearchQuery;
    private int mSearchViewStartSelection;
    private int mSearchViewEndSelection;

    private int defaultNightMode = AppCompatDelegate.MODE_NIGHT_NO;

    private FirebaseUser mCurrentUser;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate: ");
        if (MyApplication.getInstance().isNightModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            defaultNightMode = AppCompatDelegate.MODE_NIGHT_YES;
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            defaultNightMode = AppCompatDelegate.MODE_NIGHT_NO;
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        db = FirebaseFirestore.getInstance();
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        checkDocumentAvailability(mCurrentUser);
        mUser = getUserInfo(mCurrentUser);

        if (savedInstanceState == null) {
            Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("visible_fragment");
            if (currentFragment != null) {
                if (currentFragment instanceof RecordsFragment) {
                    attachRecordsFragment((RecordsFragment) currentFragment);
                }
                else if (currentFragment instanceof CalendarFragment) {
                    attachCalendarFragment((CalendarFragment) currentFragment);
                }
            } else {
                attachRecordsFragment(new RecordsFragment());
            }
        } else {
            month = savedInstanceState.getBoolean("isCalendar");
            mSearchQuery = savedInstanceState.getString("searchKey");
            isSearchViewOpened = savedInstanceState.getBoolean("isSearchViewOpened");
            isSearchViewFocused = savedInstanceState.getBoolean("isSearchViewFocused");
            mSearchViewStartSelection = savedInstanceState.getInt("searchViewSelectionStart");
            mSearchViewEndSelection = savedInstanceState.getInt("searchViewSelectionEnd");

            Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("visible_fragment");

            if (currentFragment instanceof RecordsFragment) {
                attachRecordsFragment((RecordsFragment) currentFragment);
            }
            else if (currentFragment instanceof CalendarFragment) {
                attachCalendarFragment((CalendarFragment) currentFragment);
            }
        }

        FloatingActionButton fab = findViewById(R.id.main_fab);
        fab.setOnClickListener(view -> {
            Intent intent = new Intent(MainScreenActivity.this, RecordActivity.class);
            intent.putExtra(RecordActivity.ACTION, RecordActivity.CREATE_NOTE);
            Fragment fragment = getSupportFragmentManager().findFragmentByTag("visible_fragment");
            if (fragment instanceof CalendarFragment) {
                intent.putExtra(RecordActivity.CHOSE_DATE, ((CalendarFragment) fragment).calendarView
                        .getSelectedDate().getCalendar());
            }
            startActivityForResult(intent, NEW_NOTE);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: ");

        if (AppCompatDelegate.getDefaultNightMode() != defaultNightMode) {
            Handler handler = new Handler();
            handler.postDelayed(this::recreate, 1);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "onActivityResult: ");

        if (requestCode == NEW_NOTE) {
            if (resultCode == RESULT_OK) {
                Log.i(TAG, "onActivityResult: New note");
                Log.v(TAG, data.getStringExtra(RecordActivity.TITLE));
                Log.v(TAG, data.getStringExtra(RecordActivity.RECORD));

                String user_id = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

                Record record = new Record();
                record.setTitle(data.getStringExtra(RecordActivity.TITLE));
                record.setText(data.getStringExtra(RecordActivity.RECORD));
                record.setUser_id(user_id);
                record.setDate((Date) data.getSerializableExtra(RecordActivity.DATE));
                record.setPhotos(data.getStringArrayListExtra(RecordActivity.PHOTOS));

                addRecordToDatabase(record);
            }
        } else if (requestCode == MainScreenActivity.EDIT_NOTE) {
            String noteId;
            int position;
            ArrayList<String> photoURIs;
            if (resultCode == Activity.RESULT_OK) {
                String user_id = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
                noteId = data.getStringExtra(RecordActivity.NOTE_ID);
                position = data.getExtras().getInt(RecordActivity.NOTE_POSITION);
                String title = data.getStringExtra(RecordActivity.TITLE);
                String recordText = data.getStringExtra(RecordActivity.RECORD);
                Date date = (Date) data.getSerializableExtra(RecordActivity.DATE);
                photoURIs = new ArrayList<>(data.getStringArrayListExtra(RecordActivity.PHOTOS));

                Record updatedRecord = new Record(user_id, noteId, title, recordText, date, photoURIs);

                updateRecordToDatabase(updatedRecord, position);
            } else if (resultCode == MainScreenActivity.DELETE_NOTE) {
                noteId = data.getStringExtra(RecordActivity.NOTE_ID);
                position = data.getExtras().getInt(RecordActivity.NOTE_POSITION);
                photoURIs = data.getStringArrayListExtra(RecordActivity.PHOTOS);

                Log.v(MainScreenActivity.TAG, "DAMN, this shit works!");

                deleteRecordFromDatabase(noteId, photoURIs, position);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        searchItem = menu.findItem(R.id.action_search);

        searchView = (SearchView) searchItem.getActionView();
        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        assert searchManager != null;
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

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
        changeIcons(menu.findItem(R.id.action_month), !month);

        if (isSearchViewOpened || !TextUtils.isEmpty(mSearchQuery)) {
            searchItem.expandActionView();
            searchView.setQuery(mSearchQuery, true);
            searchView.setFocusable(true);
            EditText searchText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
            searchText.setSelection(mSearchViewStartSelection, mSearchViewEndSelection);

            if (!isSearchViewFocused) {
                searchView.clearFocus();
                searchText.setOnFocusChangeListener((view, b) -> {
                    if (searchText.hasFocus()) {
                        searchText.setCursorVisible(true);
                    }
                });
                searchText.setCursorVisible(false);
                searchText.clearFocus();
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_month:
                mSearchQuery = null;
                if (searchItem.isVisible()) {
                    searchItem.collapseActionView();
                }
                changeFragments(item, month);
                return true;
            case R.id.action_refresh:
                mSearchQuery = searchView.isIconified() ? null : searchView.getQuery().toString();
                refreshRecords();
                return true;
            case R.id.action_settings:
                Intent intent = new Intent(this, PreferenceActivity.class);
                intent.putExtra("user", mUser);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i(TAG, "onSaveInstanceState: ");
        mSearchQuery = searchView.getQuery().toString();
        outState.putString("searchKey", mSearchQuery);
        outState.putBoolean("isSearchViewOpened", !searchView.isIconified());
        outState.putBoolean("isCalendar", month);
        outState.putBoolean("isSearchViewFocused", getCurrentFocus() instanceof SearchView.SearchAutoComplete);

        if (!searchView.isIconified() && getCurrentFocus() instanceof SearchView.SearchAutoComplete) {
            TextView searchText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
            outState.putInt("searchViewSelectionStart", searchText.getSelectionStart());
            outState.putInt("searchViewSelectionEnd", searchText.getSelectionEnd());
        }

        getSupportFragmentManager().putFragment(outState, "currentFragment",
                Objects.requireNonNull(getSupportFragmentManager().findFragmentByTag("visible_fragment")));
    }

    public String getSearchViewQuery() {
        return (isSearchViewOpened || !TextUtils.isEmpty(mSearchQuery)) ? mSearchQuery : null;
    }

    private void checkDocumentAvailability(FirebaseUser firebaseUser) {
        DocumentReference documentReference = db.collection("users")
                .document(firebaseUser.getUid());

        documentReference.get().addOnCompleteListener((OnCompleteListener<DocumentSnapshot>) task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot documentSnapshot = task.getResult();
                if (documentSnapshot != null && documentSnapshot.exists()) {
                } else {
                    createUserDocument(firebaseUser);
                }
            }
        });
    }

    private void createUserDocument(FirebaseUser firebaseUser) {
        Map<String, Object> user = new HashMap<>();
        user.put("user_id", firebaseUser.getUid());
        user.put("photo_count", 0);

        db.collection("users").document(firebaseUser.getUid())
                .set(user);
    }

    private User getUserInfo(FirebaseUser firebaseUser) {
        User user = new User();
        user.setUserId(firebaseUser.getUid());
        user.setUsername(firebaseUser.getDisplayName());
        user.setEmail(firebaseUser.getEmail());
        user.setPhotoUrl(firebaseUser.getPhotoUrl().toString());

        return user;
    }

    private void addRecordToDatabase(Record record) {
        DocumentReference newRecordRef = db.collection("records").document();
        record.setNote_id(newRecordRef.getId());
        newRecordRef.set(record).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.v(TAG, "Say me this shit");
                Log.v(TAG, "Okay, cool.");
                Fragment fragment = getSupportFragmentManager().findFragmentByTag("visible_fragment");

                if (fragment instanceof RecordsFragment) {
                    Collections.reverse(record.getPhotos());
                    ((RecordsFragment) fragment).recordsAdapter.addRecord(record);
                    ((RecordsFragment) fragment).setUpRecyclerVisibility();
                } else if (fragment instanceof CalendarFragment) {
                    Collections.reverse(record.getPhotos());
                    ((CalendarFragment) fragment).recordsAdapter.addRecord(record,
                            ((CalendarFragment) fragment).calendarView.getSelectedDate().getCalendar());
                    ((CalendarFragment) fragment).updateCalendarDots();
                }

            } else {
                Log.v(TAG, "Some shit happened");
            }
        });
    }

    private void updateRecordToDatabase(Record updatedRecord, int position) {
        DocumentReference dR = db.collection("records").document(updatedRecord.getNote_id());
        dR.update("title", updatedRecord.getTitle(), "text", updatedRecord.getText(),
                "date", updatedRecord.getDate()).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.v(MainScreenActivity.TAG, "Note has been updated");

                Fragment fragment = getSupportFragmentManager().findFragmentByTag("visible_fragment");
                if (fragment instanceof RecordsFragment) {
                    ((RecordsFragment) fragment).recordsAdapter.updateRecord(position, updatedRecord);
                } else if (fragment instanceof CalendarFragment) {
                    ((CalendarFragment) fragment).recordsAdapter.updateRecord
                            (position, updatedRecord, ((CalendarFragment) fragment)
                                    .calendarView.getSelectedDate().getCalendar());
                    ((CalendarFragment) fragment).updateCalendarDots();
                }
            } else {
                Log.v(MainScreenActivity.TAG, "Note update has been failed");
            }
        });
    }

    private void deleteRecordFromDatabase(String noteId, ArrayList<String> photoURIs, int position) {
        DocumentReference docRef = db.collection("records").document(noteId);
        docRef.delete().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (photoURIs != null) {
                    for (String photoURI : photoURIs) {
                        StorageReference storageRef = FirebaseStorage.getInstance()
                                .getReferenceFromUrl(photoURI);

                        storageRef.delete().addOnSuccessListener(aVoid -> {
                            decrementPhotoCount();
                        });
                    }
                }

                Fragment fragment = getSupportFragmentManager().findFragmentByTag("visible_fragment");
                if (fragment instanceof RecordsFragment) {
                    ((RecordsFragment) fragment).recordsAdapter.deleteRecord(position);
                    ((RecordsFragment) fragment).setUpRecyclerVisibility();
                } else if (fragment instanceof CalendarFragment) {
                    ((CalendarFragment) fragment).recordsAdapter.deleteRecord(position);
                    ((CalendarFragment) fragment).updateCalendarDots();
                }
            }
        });
    }
    private void decrementPhotoCount() {
        db.collection("users").document(mCurrentUser.getUid())
                .update("photo_count", FieldValue.increment(-1));
    }

    private void attachRecordsFragment(RecordsFragment recordsFragment) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_records, recordsFragment, "visible_fragment");
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        fragmentTransaction.commit();
    }

    private void attachCalendarFragment(CalendarFragment calendarFragment) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_records, calendarFragment, "visible_fragment");
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        fragmentTransaction.commit();
    }

    private void changeFragments(MenuItem item, boolean isMonth) {
        changeIcons(item, isMonth);
        if (isMonth) {
            attachRecordsFragment(new RecordsFragment());

            month = false;
        } else {
            attachCalendarFragment(new CalendarFragment());

            month = true;
        }
    }

    private void changeIcons(MenuItem item, boolean isMonth) {
        if (isMonth) {
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
    }

    private void refreshRecords() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("visible_fragment");
        if (fragment instanceof RecordsFragment) {
            ((RecordsFragment) fragment).recordsAdapter.clearRecords();
            ((RecordsFragment) fragment).getRecordsFromDatabase();
        }
        else if (fragment instanceof CalendarFragment) {
            ((CalendarFragment) fragment).recordsAdapter.clearRecords();
            ((CalendarFragment) fragment).getRecordsFromDatabase();
        }
    }
}
