package com.example.calendar.record;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.example.calendar.application.ConnectivityReceiver;
import com.example.calendar.mainscreen.MainScreenActivity;
import com.example.calendar.R;
import com.example.calendar.data.Photo;
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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class RecordActivity extends AppCompatActivity implements RecordSelectPhotoDialog.OnPhotoSelectedListener,
                                                                    DatePickerDialog.OnDateSetListener{
    public static final int REQUEST_PERMISSIONS = 299;
    public static final int REQUEST_TAKE_PHOTO = 300;
    public static final int REQUEST_CHOOSE_PHOTO = 301;

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

    // false - CREATE_NOTE, true - EDIT_NOTE
    private boolean action = false;
    private boolean isEditing = false;
    private boolean isPhotoChanged = false;

    private Intent intent;

    private ProgressBar progressBar;
    private ImageButton deleteButton;
    private ImageButton editDoneButton;
    private Button calendarPicker;
    private EditText titleView;
    private EditText recordView;

    private String title;
    private String record;
    private String recordId;
    private Date noteDate;
    private Date newDate;
    private ArrayList<String> photosUri;

    private RecordPhotosAdapter photosAdapter;

    private FirebaseFirestore db;
    private CollectionReference recordsRef;
    private UploadTask uploadPhotoTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        progressBar = findViewById(R.id.recordActivity_progress_bar);
        titleView = findViewById(R.id.recordActivity_title);
        recordView = findViewById(R.id.recordActivity_text);

        editDoneButton = findViewById(R.id.recordActivity_edit_done);
        deleteButton = findViewById(R.id.recordActivity_delete);
        calendarPicker = findViewById(R.id.recordActivity_date);

        RecyclerView photosRecycler = findViewById(R.id.recordActivity_photos_recycler);

        intent = getIntent();
        if (intent.getExtras() != null) {
            String actionStr = intent.getExtras().getString(ACTION);
            noteDate = (Date) intent.getExtras().getSerializable(DATE);
            Calendar choseCalendar = (Calendar) intent.getExtras().getSerializable(CHOSE_DATE);
            Calendar calendar = Calendar.getInstance();

            calendarPicker.setOnClickListener(view -> showDatePickerDialog());

            if (actionStr.contentEquals(CREATE_NOTE)) {
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

            } else if (actionStr.contentEquals(EDIT_NOTE)) {
                action = true;
                calendar.setTime(noteDate);

                String date = calendar.get(Calendar.MONTH)+1 + "/" +
                        calendar.get(Calendar.DAY_OF_MONTH) + "/" +
                        calendar.get(Calendar.YEAR);
                calendarPicker.setText(date);

                recordId = getIntent().getStringExtra(NOTE_ID);

                titleView.setText(intent.getStringExtra(TITLE));
                recordView.setText(intent.getStringExtra(RECORD));


                titleView.setInputType(InputType.TYPE_NULL);
                titleView.setEnabled(false);
                recordView.setInputType(InputType.TYPE_NULL);
                recordView.setEnabled(false);
                recordView.setSingleLine(false);

                Drawable editDrawable = ContextCompat.getDrawable(this, R.drawable.ic_mode_edit_black_24dp);
                editDoneButton.setImageDrawable(editDrawable);

                isEditing = false;

                editDoneButton.setOnClickListener(view -> {
                    if (isEditing) {
                        titleView.setInputType(InputType.TYPE_NULL);
                        titleView.setEnabled(false);
                        recordView.setInputType(InputType.TYPE_NULL);
                        recordView.setEnabled(false);
                        recordView.setSingleLine(false);

                        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_mode_edit_black_24dp);
                        editDoneButton.setImageDrawable(drawable);

                        isEditing = false;
                    } else {
                        titleView.setInputType(InputType.TYPE_CLASS_TEXT);
                        titleView.setEnabled(true);
                        recordView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                        recordView.setEnabled(true);

                        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_done_black_24dp);
                        editDoneButton.setImageDrawable(drawable);

                        isEditing = true;
                    }
                });

                deleteButton.setOnClickListener(view -> confirmDeleteDialog());

                photosUri = getIntent().getStringArrayListExtra(PHOTOS);
            }
        }

        title = titleView.getText().toString();
        record = recordView.getText().toString();
        title = titleView.getText().toString();
        record = recordView.getText().toString();

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
                    verifyPermissions();
                    if (ConnectivityReceiver.isConnected()) {
                        RecordSelectPhotoDialog selectPhotoDialog = new RecordSelectPhotoDialog();
                        selectPhotoDialog.show(getSupportFragmentManager(), getString(R.string.choose_take_photo));
                    } else {
                        Snackbar.make(findViewById(R.id.recordActivity_layout),
                                getString(R.string.internetConnectionFailedPhoto), Snackbar.LENGTH_LONG)
                                .show();
                    }
                }
            }

            @Override
            public void onDeleteImageClick(int position) {
                confirmDeletePhotoDialog(position);
            }
        });
                photosRecycler.setAdapter(photosAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        photosRecycler.setLayoutManager(layoutManager);

        db = FirebaseFirestore.getInstance();
        recordsRef = db.collection("records");

        Toolbar toolbar = findViewById(R.id.recordActivity_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    @Override
    public void onBackPressed() {
        if (progressBar.getVisibility() == View.VISIBLE) {
            if (uploadPhotoTask.isInProgress()) {
                uploadPhotoTask.pause();
                confirmPhotoUploadCancel();
            }
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

    public String createImageName() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());//получаем время

        return "photo_" + timeStamp;
    }

    @Override
    public void getChosenImage(Uri imagePath) {
        String imageName = createImageName();

        saveImageToFirebase(imagePath, imageName);
    }

    @Override
    public void getTakenImage(File image) {
        String imageName = image.getName().substring(0, image.getName().lastIndexOf('.'));

        saveImageToFirebase(Uri.fromFile(image), imageName);
    }

    private void verifyPermissions() {
        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA};

        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                permissions[0]) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getApplicationContext(),
                        permissions[1]) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getApplicationContext(),
                        permissions[2]) == PackageManager.PERMISSION_GRANTED) {

        } else {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        verifyPermissions();
    }

    private boolean checkChanges() {
        // true if changes exist
        return !title.contentEquals(titleView.getText().toString()) ||
                !record.contentEquals(recordView.getText().toString()) ||
                isPhotoChanged || newDate != null;
    }

    private void confirmDeleteDialog() {
        AlertDialog.Builder deleteDialog = new AlertDialog.Builder(this);
        deleteDialog.setMessage(R.string.deleteConfirmation);

        deleteDialog.setPositiveButton(R.string.delete, (dialogInterface, i) -> {
            setResult(MainScreenActivity.DELETE_NOTE, intent);
            finish();
        });

        deleteDialog.setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
        });

        deleteDialog.create().show();
    }

    private void confirmDeletePhotoDialog(int position) {
        AlertDialog.Builder deleteDialog = new AlertDialog.Builder(this);
        deleteDialog.setMessage(R.string.deletePhotoConfirmation);

        deleteDialog.setPositiveButton(R.string.delete, (dialogInterface, i) -> {
            if (action) {
                recordsRef.document(recordId).update("photos", FieldValue.arrayRemove(photosAdapter
                        .photos.get(position).getPhotoUrl()))
                        .addOnCompleteListener(task -> {
                            StorageReference storageRef = FirebaseStorage.getInstance()
                                    .getReferenceFromUrl(photosAdapter.photos.get(position).getPhotoUrl());
                            storageRef.delete().addOnSuccessListener(aVoid -> {
                                Log.v(MainScreenActivity.TAG, "Photo has been deleted successfully");
                                photosUri.remove(photosAdapter.photos.get(position).getPhotoUrl());
                                photosAdapter.photos.remove(position);
                                photosAdapter.notifyDataSetChanged();

                                isPhotoChanged = true;
                            }).addOnFailureListener(e -> {
                                Log.v(MainScreenActivity.TAG, "Photo has not been deleted, error: " + e);
                            });
                        }).addOnFailureListener(e -> {
                    Log.v(MainScreenActivity.TAG, "Delete from array has been failed");
                });
            } else {
                StorageReference storageRef = FirebaseStorage.getInstance()
                        .getReferenceFromUrl(photosAdapter.photos.get(position).getPhotoUrl());
                storageRef.delete().addOnSuccessListener(aVoid -> {
                    Log.v(MainScreenActivity.TAG, "Photo has been deleted successfully");
                    photosUri.remove(photosAdapter.photos.get(position).getPhotoUrl());
                    photosAdapter.photos.remove(position);
                    photosAdapter.notifyDataSetChanged();

                    isPhotoChanged = photosUri.size() > 0;
                }).addOnFailureListener(e -> {
                    Log.v(MainScreenActivity.TAG, "Photo has not been deleted, error: " + e);
                });
            }
        });

        deleteDialog.setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
        });

        deleteDialog.create().show();
    }

    private void confirmPhotoUploadCancel() {
        AlertDialog.Builder confirmDialog = new AlertDialog.Builder(this);
        confirmDialog.setMessage(R.string.cancelUploadConfirmation);

        confirmDialog.setPositiveButton(R.string.continueUploading, (dialogInterface, i) -> {
            uploadPhotoTask.resume();
        });

        confirmDialog.setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
            uploadPhotoTask.cancel();
            progressBar.setVisibility(View.GONE);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        });

        confirmDialog.create().show();
    }

    private void saveImageToFirebase(Uri photo, String photoName) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        String userID = user.getUid();

        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("images/users/" + userID + "/" + photoName);
        progressBar.setVisibility(View.VISIBLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        uploadPhotoTask = storageReference.putFile(photo);
        uploadPhotoTask.addOnSuccessListener(taskSnapshot -> {
            Log.v(MainScreenActivity.TAG, "Dopy");

            Task<Uri> result = taskSnapshot.getMetadata().getReference().getDownloadUrl();
            result.addOnSuccessListener(uri -> {
                Log.v(MainScreenActivity.TAG, "Tell me this shit - " + uri.toString());
                String photoUri = uri.toString();
                if (action) {
                    recordsRef.document(recordId).update("photos", FieldValue.arrayUnion(photoUri))
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    photosUri.add(0, photoUri);
                                    Log.v(MainScreenActivity.TAG, "Photo URL was bound to record");
                                    progressBar.setVisibility(View.GONE);
                                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                                    Photo photoObj = new Photo(photoName, photoUri);
                                    photosAdapter.addPhoto(photoObj);
                                    isPhotoChanged = true;
                                } else {
                                    Log.v(MainScreenActivity.TAG, "Photo URL binding error");
                                }
                            });
                } else {
                    photosUri.add(photoUri);
                    progressBar.setVisibility(View.GONE);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                    Photo photoObj = new Photo(photoName, photoUri);
                    photosAdapter.addPhoto(photoObj);
                    isPhotoChanged = true;
                }
            });

        }).addOnFailureListener(e -> {
            Log.v(MainScreenActivity.TAG, "Shitty");
        }).addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            Log.v(MainScreenActivity.TAG, "Progress" + progress);
            progressBar.setProgress((int)progress);
        });
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                                                this,
                                                Calendar.getInstance().get(Calendar.YEAR),
                                                Calendar.getInstance().get(Calendar.MONTH),
                                                Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    @Override
    public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, i);
        calendar.set(Calendar.MONTH, i1);
        calendar.set(Calendar.DAY_OF_MONTH, i2);
        newDate = calendar.getTime();

        String date = i1+1 + "/" + i2 + "/" + i;
        calendarPicker.setText(date);
    }
}
