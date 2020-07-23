package com.hifeful.diurnal.record;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.transition.Fade;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.net.Uri;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.google.firebase.firestore.DocumentSnapshot;
import com.hifeful.diurnal.application.ConnectivityReceiver;
import com.hifeful.diurnal.mainscreen.MainScreenActivity;
import com.hifeful.diurnal.R;
import com.hifeful.diurnal.data.Photo;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

public class RecordActivity extends AppCompatActivity implements RecordSelectPhotoDialog.OnPhotoSelectedListener,
                                                                    DatePickerDialog.OnDateSetListener,
                                                            ViewTreeObserver.OnGlobalLayoutListener {
    // Constants
    public static final String ACTION = "ACTION";
    public static final String CREATE_NOTE = "CREATE_NOTE";
    public static final String EDIT_NOTE = "EDIT_NOTE";

    public static final String TITLE = "TITLE";
    public static final String RECORD = "RECORD";
    public static final String DATE = "DATE";
    public static final String CHOSE_DATE = "CHOSE_DATE";
    public static final String NOTE_ID = "NOTE_ID";
    public static final String NOTE_POSITION = "NOTE_POSITION";
    public static final String PHOTOS = "PHOTOS";

    // UI
    private RelativeLayout mRootLayout;
    private RecyclerView photosRecycler;
    private ProgressBar progressBar;
    private ImageButton deleteButton;
    private ImageButton editDoneButton;
    private Button calendarPicker;
    private EditText titleView;
    private EditText recordView;

    private AlertDialog deleteDialog;
    private AlertDialog deletePhotoDialog;
    private AlertDialog uploadPhotoCancelDialog;
    private DatePickerDialog datePickerDialog;
    private AlertDialog photoLimitDialog;

    // Variables
    // false - CREATE_NOTE, true - EDIT_NOTE
    private boolean action = false;
    private boolean isEditing = false;
    private boolean isKeyboardShowing = false;
    private boolean isRecordField = true;
    private boolean isPhotoChanged = false;

    private boolean isSaving = false;
    private boolean isDeleteDialogShowing = false;
    private boolean isDeletePhotoDialogShowing = false;
    private int toDeletePhotoPosition;
    private boolean isUploadPhotoCancelDialogShowing = false;
    private boolean isDatePickerShowing = false;
    private boolean isPhotoLimitDialogShowing = false;

    private int startSelection = 0;
    private int endSelection = 0;

    private Intent intent;

    private String title;
    private String record;
    private String recordId;
    private Date noteDate;
    private Date newDate;
    private ArrayList<String> photosUri;

    private RecordPhotosAdapter photosAdapter;

    private FirebaseUser mCurrentUser;
    private CollectionReference recordsRef;
    private CollectionReference usersRef;
    private StorageReference mStorageRef;
    private UploadTask uploadPhotoTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        if (savedInstanceState != null) {
            photosUri = savedInstanceState.getStringArrayList("photosUri");
        }

        mRootLayout = findViewById(R.id.recordActivity_layout);
        progressBar = findViewById(R.id.recordActivity_progress_bar);
        titleView = findViewById(R.id.recordActivity_title);
        recordView = findViewById(R.id.recordActivity_text);

        editDoneButton = findViewById(R.id.recordActivity_edit_done);
        deleteButton = findViewById(R.id.recordActivity_delete);
        calendarPicker = findViewById(R.id.recordActivity_date);

        photosRecycler = findViewById(R.id.recordActivity_photos_recycler);

        intent = getIntent();
        if (intent.getExtras() != null) {
            String actionStr = intent.getExtras().getString(ACTION);
            noteDate = (Date) intent.getExtras().getSerializable(DATE);
            Calendar choseCalendar = (Calendar) intent.getExtras().getSerializable(CHOSE_DATE);
            Calendar calendar = Calendar.getInstance();

            calendarPicker.setOnClickListener(view -> showDatePickerDialog(noteDate));

            if (actionStr.contentEquals(CREATE_NOTE)) {
                if (savedInstanceState != null) {
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED |
                            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                } else {
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE |
                            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                }
                setUpNoteCreating(choseCalendar, calendar);
            } else if (actionStr.contentEquals(EDIT_NOTE)) {
                setUpNoteEditing(calendar);
            }
        }

        mRootLayout.getViewTreeObserver().addOnGlobalLayoutListener(this);

        title = titleView.getText().toString();
        record = recordView.getText().toString();

        setUpPhotoRecycler();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        recordsRef = db.collection("records");
        usersRef = db.collection("users");
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();

        Toolbar toolbar = findViewById(R.id.recordActivity_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        isSaving = true;

        outState.putBoolean("mode", isEditing);

        if (getCurrentFocus() != null) {
            saveCursorState();
            outState.putBoolean("isKeyboardShowing", isKeyboardShowing);
            outState.putBoolean("isRecord", isRecordField);
            outState.putInt("startSelection", startSelection);
            outState.putInt("endSelection", endSelection);
        }

        if (newDate != null) {
            outState.putSerializable("newDate", newDate);
        }
        outState.putSerializable("noteDate", noteDate);
        outState.putBoolean("isPhotoChanged", isPhotoChanged);
        outState.putStringArrayList("photosUri", photosUri);

        saveDialogStates(outState);

        if (mStorageRef != null && progressBar.getVisibility() == View.VISIBLE) {
            outState.putString("storageReference", mStorageRef.toString());
        }
    }

    private void saveCursorState() {
        switch (getCurrentFocus().getId()) {
            case R.id.recordActivity_title:
                isRecordField = false;
                startSelection = ((EditText) getCurrentFocus()).getSelectionStart();
                endSelection = ((EditText) getCurrentFocus()).getSelectionEnd();
                break;
            case R.id.recordActivity_text:
                isRecordField = true;
                startSelection = ((EditText) getCurrentFocus()).getSelectionStart();
                endSelection = ((EditText) getCurrentFocus()).getSelectionEnd();
                break;
        }
    }

    private void saveDialogStates(@NonNull Bundle outState) {
        if (deleteDialog != null) {
            if (deleteDialog.isShowing()) {
                isDeleteDialogShowing = true;
                deleteDialog.dismiss();
            } else {
                isDeleteDialogShowing = false;
            }
            outState.putBoolean("isDeleteDialogShowing", isDeleteDialogShowing);
        } else if (deletePhotoDialog != null) {
            if (deletePhotoDialog.isShowing()) {
                isDeletePhotoDialogShowing = true;
                outState.putInt("toDeletePhotoPosition", toDeletePhotoPosition);
                deletePhotoDialog.dismiss();
            } else {
                isDeletePhotoDialogShowing = false;
            }
            outState.putBoolean("isDeletePhotoDialogShowing", isDeletePhotoDialogShowing);
        } else if (datePickerDialog != null) {
            if (datePickerDialog.isShowing()) {
                isDatePickerShowing = true;

                Calendar selectedCalendar = Calendar.getInstance();
                selectedCalendar.set(Calendar.MONTH, datePickerDialog.getDatePicker().getMonth());
                selectedCalendar.set(Calendar.DAY_OF_MONTH, datePickerDialog.getDatePicker().getDayOfMonth());
                selectedCalendar.set(Calendar.YEAR, datePickerDialog.getDatePicker().getYear());

                Date selectedDate = selectedCalendar.getTime();
                outState.putSerializable("datePickerSelectedDate", selectedDate);

                datePickerDialog.dismiss();
            } else {
                isDatePickerShowing = false;
            }
            outState.putBoolean("isDatePickerShowing", isDatePickerShowing);
        } else if (uploadPhotoCancelDialog != null) {
            if (uploadPhotoCancelDialog.isShowing()) {
                isUploadPhotoCancelDialogShowing = true;
                outState.putBoolean("isPhotoPaused", uploadPhotoTask.isPaused());
                uploadPhotoCancelDialog.dismiss();
            } else {
                isUploadPhotoCancelDialogShowing = false;
            }
            outState.putBoolean("isUploadPhotoCancelDialogShowing", isUploadPhotoCancelDialogShowing);
        } else if (photoLimitDialog != null) {
            if (photoLimitDialog.isShowing()) {
                isPhotoLimitDialogShowing = true;

                photoLimitDialog.dismiss();
            } else {
                isPhotoLimitDialogShowing = true;
            }
            outState.putBoolean("isPhotoLimitDialogShowing", isPhotoLimitDialogShowing);
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        isEditing = savedInstanceState.getBoolean("mode");

        isRecordField = savedInstanceState.getBoolean("isRecord", true);
        startSelection = savedInstanceState.getInt("startSelection", recordView.length());
        endSelection = savedInstanceState.getInt("endSelection", recordView.length());
        isKeyboardShowing = savedInstanceState.getBoolean("isKeyboardShowing");

        if (isEditing) {
            enableEditing(isRecordField ? recordView : titleView, startSelection, endSelection,
                    true);
        }

        newDate = (Date) savedInstanceState.getSerializable("newDate");
        if (newDate != null) {
            Calendar newCalendar = new GregorianCalendar();
            newCalendar.setTime(newDate);
            changeDate(newCalendar);
        }
        noteDate = (Date) savedInstanceState.getSerializable("noteDate");
        isPhotoChanged = savedInstanceState.getBoolean("isPhotoChanged");

        isDeleteDialogShowing = savedInstanceState.getBoolean("isDeleteDialogShowing");
        isDeletePhotoDialogShowing = savedInstanceState.getBoolean("isDeletePhotoDialogShowing");
        isUploadPhotoCancelDialogShowing = savedInstanceState.getBoolean("isUploadPhotoCancelDialogShowing");
        isDatePickerShowing = savedInstanceState.getBoolean("isDatePickerShowing");
        isPhotoLimitDialogShowing = savedInstanceState.getBoolean("isPhotoLimitDialogShowing");
        if (isDeleteDialogShowing) {
            showConfirmDeleteDialog();
        } else if (isDeletePhotoDialogShowing) {
            int position = savedInstanceState.getInt("toDeletePhotoPosition");
            showConfirmDeletePhotoDialog(position);
        } else if (isDatePickerShowing) {
            Date selectedDate = (Date) savedInstanceState.getSerializable("datePickerSelectedDate");
            showDatePickerDialog(selectedDate);
        } else if (isUploadPhotoCancelDialogShowing) {
            showConfirmPhotoUploadCancelDialog();
        } else if (isPhotoLimitDialogShowing) {
            showPhotoLimitDialog();
        }

        String strStorageRef = savedInstanceState.getString("storageReference");
        if (strStorageRef != null) {
            mStorageRef = FirebaseStorage.getInstance().getReferenceFromUrl(strStorageRef);
            List<UploadTask> uploadTaskList = mStorageRef.getActiveUploadTasks();
            if (uploadTaskList.size() > 0) {
                boolean isPhotoPaused = savedInstanceState.getBoolean("isPhotoPaused");
                UploadTask uploadTask = uploadTaskList.get(0);
                if (isPhotoPaused) {
                    if (uploadTask.isInProgress()) {
                        uploadTask.pause();
                    }
                }
                setPhotoUploadListeners(uploadTask, mStorageRef.getName());

                uploadPhotoTask = uploadTask;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (progressBar.getVisibility() == View.VISIBLE) {
            if (uploadPhotoTask.isInProgress()) {
                uploadPhotoTask.pause();
                showConfirmPhotoUploadCancelDialog();
            }
            return;
        }

        if (action && isEditing) {
            editDoneButton.performClick();
            return;
        }
        if (!action && isKeyboardShowing) {
            hideSoftKeyboard(getCurrentFocus());
            return;
        }
        if (checkChanges()) {
            intent.putExtra(TITLE, titleView.getText().toString());
            intent.putExtra(RECORD, recordView.getText().toString());
            if (newDate != null)
                intent.putExtra(DATE, newDate);
            else
                intent.putExtra(DATE, noteDate);
            intent.putExtra(PHOTOS, photosUri);
            setResult(RESULT_OK, intent);
        }
        super.onBackPressed();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void setUpNoteCreating(Calendar choseCalendar, Calendar calendar) {
        action = false;

        if (choseCalendar != null) {
            calendar.set(Calendar.YEAR, choseCalendar.get(Calendar.YEAR));
            calendar.set(Calendar.MONTH, choseCalendar.get(Calendar.MONTH));
            calendar.set(Calendar.DAY_OF_MONTH, choseCalendar.get(Calendar.DAY_OF_MONTH));
        }

        String date = calendar.get(Calendar.MONTH)+1 + "/" +
                calendar.get(Calendar.DAY_OF_MONTH) + "/" +
                calendar.get(Calendar.YEAR);
        calendarPicker.setText(date);
        noteDate = calendar.getTime();

        editDoneButton.setVisibility(View.GONE);
        deleteButton.setVisibility(View.GONE);

        // It needs API 28
        titleView.requestFocus();

        isEditing = true;
    }

    private void setUpNoteEditing(Calendar calendar) {
        action = true;
        calendar.setTime(noteDate);

        String date = calendar.get(Calendar.MONTH)+1 + "/" +
                calendar.get(Calendar.DAY_OF_MONTH) + "/" +
                calendar.get(Calendar.YEAR);
        calendarPicker.setText(date);

        recordId = getIntent().getStringExtra(NOTE_ID);

        titleView.setText(intent.getStringExtra(TITLE));
        recordView.setText(intent.getStringExtra(RECORD));

        disableEditing();

        Drawable editDrawable = ContextCompat.getDrawable(this, R.drawable.ic_mode_edit_black_24dp);
        editDoneButton.setImageDrawable(editDrawable);

        startSelection = recordView.length();
        endSelection = recordView.length();
        editDoneButton.setOnClickListener(view -> {
            if (isEditing) {
                if (getCurrentFocus() != null) {
                    saveCursorState();
                }
                disableEditing();
            } else {
                enableEditing(isRecordField ? recordView : titleView, startSelection, endSelection,
                        false);
            }
        });

        deleteButton.setOnClickListener(view -> showConfirmDeleteDialog());

        photosUri = getIntent().getStringArrayListExtra(PHOTOS);
    }

    private void enableEditing(EditText view, int startSelection, int endSelection, boolean isRestored) {
        enableContentInteractions();

        if (!isRestored && !isKeyboardShowing) {
            showSoftKeyboard(view);
        } else if (isRestored && isKeyboardShowing) {
            showSoftKeyboard(view);
        }

        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_done_black_24dp);
        editDoneButton.setImageDrawable(drawable);

        view.requestFocus();
        if (isRestored) {
            view.setSelection(startSelection, endSelection);
        } else {
            view.setSelection(endSelection);
        }

        isEditing = true;
    }

    private void disableEditing() {
        if (getCurrentFocus() != null) {
            hideSoftKeyboard(getCurrentFocus());
        }
        disableContentInteractions();

        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_mode_edit_black_24dp);
        editDoneButton.setImageDrawable(drawable);

        isEditing = false;
    }

    private void enableContentInteractions() {
        titleView.setKeyListener(new EditText(this).getKeyListener());
        titleView.setFocusable(true);
        titleView.setFocusableInTouchMode(true);
        titleView.setCursorVisible(true);

        recordView.setKeyListener(new EditText(this).getKeyListener());
        recordView.setFocusable(true);
        recordView.setFocusableInTouchMode(true);
        recordView.setCursorVisible(true);
    }

    private void disableContentInteractions() {
        titleView.setKeyListener(null);
        titleView.setFocusable(false);
        titleView.setFocusableInTouchMode(false);
        titleView.setCursorVisible(false);

        recordView.setKeyListener(null);
        recordView.setFocusable(false);
        recordView.setFocusableInTouchMode(false);
        recordView.setCursorVisible(false);
    }

    private void showSoftKeyboard(View view) {
        if(view.requestFocus()) {
            Log.i(MainScreenActivity.TAG, getCurrentFocus().toString());
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideSoftKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void getPhotosCount() {

        usersRef.document(mCurrentUser.getUid()).get()
                .addOnCompleteListener(task -> {
                    Long photosCount;
                    if (task.isSuccessful()) {
                        DocumentSnapshot snapshot = task.getResult();
                        if (snapshot != null && snapshot.exists()) {
                            photosCount = snapshot.getLong("photo_count");
                        } else {
                            photosCount = 0L;
                        }
                    } else {
                        photosCount = 0L;
                    }
                    if (photosCount < 50L) {
                        RecordSelectPhotoDialog selectPhotoDialog = new RecordSelectPhotoDialog();
                        selectPhotoDialog.show(getSupportFragmentManager(), getString(R.string.choose_take_photo));
                    } else {
                        showPhotoLimitDialog();
                    }
                });
    }

    private void setUpPhotoRecycler() {
        ArrayList<Photo> photos = new ArrayList<>();

        photos.add(new Photo("Add a photo", "photo"));
        if (photosUri != null) {
            for (String photoUri : photosUri) {
                photos.add(new Photo("dipa", photoUri));
            }
        } else {
            photosUri = new ArrayList<>();
        }

        photosAdapter = new RecordPhotosAdapter(this, photos);
        photosAdapter.setListener(new RecordPhotosAdapter.Listener() {
            @Override
            public void onClick(int position) {
                if (position == 0) {
                        if (ConnectivityReceiver.isConnected()) {
                            // Get photos count from Firestore and show appropriate dialog
                            getPhotosCount();
                        } else {
                            Snackbar.make(findViewById(R.id.recordActivity_layout),
                                    getString(R.string.internetConnectionFailedPhoto), Snackbar.LENGTH_LONG)
                                    .show();
                        }
                } else {
                    Bundle bundle = new Bundle();
                    bundle.putString("photoUrl", photos.get(position).getPhotoUrl());
                    RecordPhotoFragment photoFragment = new RecordPhotoFragment();
                    photoFragment.setArguments(bundle);

                    photoFragment.setEnterTransition(new Fade());
                    photoFragment.setExitTransition(new Fade());

                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(android.R.id.content, photoFragment)
                            .addToBackStack(null)
                            .commit();
                }
            }

            @Override
            public void onDeleteImageClick(int position) {
                showConfirmDeletePhotoDialog(position);
            }
        });
        photosRecycler.setAdapter(photosAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        photosRecycler.setLayoutManager(layoutManager);
        photosRecycler.post(() -> {
            View first = photosRecycler.getChildAt(0);
            int[] originalPos = new int[2];
            first.getLocationInWindow(originalPos);
            float progressBarWidthPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    25,
                    getResources().getDisplayMetrics());
            int viewCenter = (int) ((first.getWidth() / 2) - progressBarWidthPx);
            progressBar.setX((int)originalPos[0] + viewCenter + 4);
            progressBar.setY((int)originalPos[1] + 4);
        });
    }

    public String createImageName() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());//получаем время

        return "photo_" + timeStamp;
    }

    @Override
    public void getChosenImage(Uri imagePath) {
        String imageName = createImageName();
        String strImagePath = getRealImagePath(imagePath);
        File imageFile = new File(strImagePath);
        try {
            Bitmap imageBitmap = RecordSelectPhotoDialog.handleSamplingAndRotationBitmap(this, Uri.fromFile(imageFile));
            savePhotoToStorage(imageBitmap, imageName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void getTakenImage(File image) {
        String imageName = image.getName().substring(0, image.getName().lastIndexOf('.'));
        Uri imageUri = Uri.fromFile(image);
        Log.i(MainScreenActivity.TAG, "getTakenImage: "  + imageUri.toString());
        try {
            Bitmap imageBitmap = RecordSelectPhotoDialog.handleSamplingAndRotationBitmap(this,
                    imageUri);
            savePhotoToStorage(imageBitmap, imageName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getRealImagePath(Uri uri){
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        String document_id = cursor.getString(0);
        document_id = document_id.substring(document_id.lastIndexOf(":")+1);
        cursor.close();

        cursor = getContentResolver().query(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null, MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);
        cursor.moveToFirst();
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        cursor.close();

        return path;
    }

    private boolean checkChanges() {
        // true if changes exist
        return !title.contentEquals(titleView.getText().toString()) ||
                !record.contentEquals(recordView.getText().toString()) ||
                isPhotoChanged || newDate != null;
    }

    private void showConfirmDeleteDialog() {
        AlertDialog.Builder deleteDialogBuilder = new AlertDialog.Builder(this, R.style.AlertDialogCustom);
        deleteDialogBuilder.setMessage(R.string.deleteConfirmation);

        deleteDialogBuilder.setPositiveButton(R.string.delete, (dialogInterface, i) -> {
            setResult(MainScreenActivity.DELETE_NOTE, intent);
            finish();
        });

        deleteDialogBuilder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
        });

        deleteDialogBuilder.setOnDismissListener(dialogInterface -> deleteDialog = null);

        deleteDialog = deleteDialogBuilder.create();
        deleteDialog.show();
    }

    private void showConfirmDeletePhotoDialog(int position) {
        AlertDialog.Builder deletePhotoDialogBuilder = new AlertDialog.Builder(this, R.style.AlertDialogCustom);
        deletePhotoDialogBuilder.setMessage(R.string.deletePhotoConfirmation);

        deletePhotoDialogBuilder.setPositiveButton(R.string.delete, (dialogInterface, i) -> {
            if (action) {
                recordsRef.document(recordId).update("photos", FieldValue.arrayRemove(photosAdapter
                        .photos.get(position).getPhotoUrl()))
                        .addOnCompleteListener(task -> {
                            deletePhotoFromStorage(position);
                        }).addOnFailureListener(e -> {
                    Log.v(MainScreenActivity.TAG, "Delete from array has been failed");
                });
            } else {
                deletePhotoFromStorage(position);
            }
        });

        deletePhotoDialogBuilder.setOnDismissListener(dialogInterface -> deletePhotoDialog = null);

        deletePhotoDialogBuilder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
        });

        toDeletePhotoPosition = position;
        deletePhotoDialog = deletePhotoDialogBuilder.create();
        deletePhotoDialog.show();
    }

    private void deletePhotoFromStorage(int position) {
        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReferenceFromUrl(photosAdapter.photos.get(position).getPhotoUrl());
        storageRef.delete().addOnSuccessListener(aVoid -> {
            Log.v(MainScreenActivity.TAG, "Photo has been deleted successfully");
            changePhotoCount(-1);
            photosUri.remove(photosAdapter.photos.get(position).getPhotoUrl());
            photosAdapter.photos.remove(position);
            photosAdapter.notifyDataSetChanged();
            if (action) {
                isPhotoChanged = true;
            } else {
                isPhotoChanged = photosUri.size() > 0;
            }
        }).addOnFailureListener(e -> {
            Log.v(MainScreenActivity.TAG, "Photo has not been deleted, error: " + e);
        });
    }

    private void addPhotoUriToDatabase(String photoName, String photoUri) {
        recordsRef.document(recordId).update("photos", FieldValue.arrayUnion(photoUri))
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        changePhotoCount(1);
                        photosUri.add(0, photoUri);
                        Log.v(MainScreenActivity.TAG, "Photo URL was bound to record");
                        hideProgressBar();

                        Photo photoObj = new Photo(photoName, photoUri);
                        photosAdapter.addPhoto(photoObj);
                        isPhotoChanged = true;
                    } else {
                        Log.v(MainScreenActivity.TAG, "Photo URL binding error");
                    }
                });
    }

    private void showConfirmPhotoUploadCancelDialog() {
        AlertDialog.Builder photoUploadCancelDialogBuilder = new AlertDialog.Builder(this, R.style.AlertDialogCustom);
        photoUploadCancelDialogBuilder.setMessage(R.string.cancelUploadConfirmation);

        photoUploadCancelDialogBuilder.setPositiveButton(R.string.continueUploading, (dialogInterface, i) -> {
            if (uploadPhotoTask != null && !uploadPhotoTask.isComplete()) {
                uploadPhotoTask.resume();
            }
        });

        photoUploadCancelDialogBuilder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
            if (uploadPhotoTask != null) {
                uploadPhotoTask.cancel();
            }
            hideProgressBar();
        });
        photoUploadCancelDialogBuilder.setOnDismissListener(dialogInterface -> {
            uploadPhotoCancelDialog = null;
            if (!isSaving) {
                if (uploadPhotoTask != null && !uploadPhotoTask.isComplete()) {
                    uploadPhotoTask.resume();
                }
            }
        });

        uploadPhotoCancelDialog = photoUploadCancelDialogBuilder.create();
        uploadPhotoCancelDialog.show();
    }

    private void showPhotoLimitDialog() {
        AlertDialog.Builder photoLimitDialogBuilder = new AlertDialog.Builder(this,
                R.style.AlertDialogCustom);
        photoLimitDialogBuilder.setMessage(getResources().getString(R.string.photoLimitMessageDialog));

        photoLimitDialogBuilder.setCancelable(false).
                setPositiveButton("OK", (dialogInterface, i) -> {
            dialogInterface.dismiss();
        });

        photoLimitDialog = photoLimitDialogBuilder.create();
        photoLimitDialog.show();
    }

    private void savePhotoToStorage(Bitmap photo, String photoName) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        photo.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();

        String userID = mCurrentUser.getUid();

       mStorageRef = FirebaseStorage.getInstance().getReference()
                .child("images/users/" + userID + "/" + photoName);
        uploadPhotoTask = mStorageRef.putBytes(imageBytes);
        setPhotoUploadListeners(uploadPhotoTask, photoName);
    }

    private void changePhotoCount(long count) {
        usersRef.document(mCurrentUser.getUid()).update("photo_count", FieldValue.increment(count));
    }

    private void setPhotoUploadListeners(UploadTask uploadTask, String photoName) {
        showProgressBar();
        uploadTask.addOnSuccessListener(this, taskSnapshot -> {
            Log.v(MainScreenActivity.TAG, "Dopy");
            Task<Uri> result = taskSnapshot.getMetadata().getReference().getDownloadUrl();
            result.addOnSuccessListener(this, uri -> {
                Log.v(MainScreenActivity.TAG, "Tell me this shit - " + uri.toString());
                String photoUri = uri.toString();
                if (action) {
                    addPhotoUriToDatabase(photoName, photoUri);
                } else {
                    changePhotoCount(1);
                    photosUri.add(photoUri);

                    hideProgressBar();

                    Photo photoObj = new Photo(photoName, photoUri);
                    photosAdapter.addPhoto(photoObj);

                    isPhotoChanged = true;
                }
            });

        }).addOnFailureListener(this, e -> {
            Log.v(MainScreenActivity.TAG, "Shitty");
        }).addOnProgressListener(this, taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            Log.v(MainScreenActivity.TAG, "Progress" + progress);
            progressBar.setProgress((int)progress);
        });
    }

    private void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private void showDatePickerDialog(Date selectedDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(selectedDate);
        datePickerDialog = new DatePickerDialog(this,
                                                R.style.DatePickerDialogCustom,
                                                this,
                                                calendar.get(Calendar.YEAR),
                                                calendar.get(Calendar.MONTH),
                                                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    @Override
    public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, i);
        calendar.set(Calendar.MONTH, i1);
        calendar.set(Calendar.DAY_OF_MONTH, i2);
        noteDate = calendar.getTime();
        newDate = calendar.getTime();

        changeDate(calendar);
    }

    private void changeDate(Calendar date) {
        String dateStr = date.get(Calendar.MONTH)+1 + "/" + date.get(Calendar.DAY_OF_MONTH) +
                "/" + date.get(Calendar.YEAR);
        calendarPicker.setText(dateStr);
    }

    @Override
    public void onGlobalLayout() {
        Rect r = new Rect();
        mRootLayout.getWindowVisibleDisplayFrame(r);
        int screenHeight = mRootLayout.getRootView().getHeight();

        // r.bottom is the position above soft keypad or device button.
        // if keypad is shown, the r.bottom is smaller than that before.
        int keypadHeight = screenHeight - r.bottom;

        if (keypadHeight > screenHeight * 0.15) { // 0.15 ratio is perhaps enough to determine keypad height.
            // keyboard is opened
            if (!isKeyboardShowing) {
                isKeyboardShowing = true;
            }
        }
        else {
            // keyboard is closed
            if (isKeyboardShowing) {
                isKeyboardShowing = false;
            }
        }
    }
}
