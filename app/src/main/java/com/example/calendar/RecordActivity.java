package com.example.calendar;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
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

import com.example.calendar.models.Photo;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class RecordActivity extends AppCompatActivity implements SelectPhotoDialog.OnPhotoSelectedListener,
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
    public static final String NOTE_ID = "NOTE_ID";
    public static final String NOTE_POSITION = "NOTE_POSITION";
    public static final String PHOTOS = "PHOTOS";

    // false - CREATE_NOTE, true - EDIT_NOTE
    private boolean action = false;
    private boolean isEditing = false;
    private boolean isPhotoAdded = false;

    private Intent intent;

    private ImageButton deleteButton;
    private ImageButton editDoneButton;
    private Button calendarPicker;
    private EditText titleView;
    private EditText recordView;

    private String title;
    private String record;
    private String recordId;
    private Date noteDate;
    private ArrayList<String> photosUri;

    private PhotosAdapter photosAdapter;

    private FirebaseFirestore db;
    private CollectionReference recordsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

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
            Calendar calendar = Calendar.getInstance();

            calendarPicker.setOnClickListener(view -> showDatePickerDialog());

            if (actionStr.contentEquals(CREATE_NOTE)) {
                action = false;

                String date = calendar.get(Calendar.MONTH)+1 + "/" +
                        calendar.get(Calendar.DAY_OF_MONTH) + "/" +
                        calendar.get(Calendar.YEAR);
                calendarPicker.setText(date);

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
                titleView.setOnKeyListener((view, i, keyEvent) -> true);
                recordView.setInputType(InputType.TYPE_NULL);
                recordView.setOnKeyListener((view, i, keyEvent) -> true);
                recordView.setSingleLine(false);

                Drawable editDrawable = ContextCompat.getDrawable(this, R.drawable.ic_mode_edit_black_24dp);
                editDoneButton.setImageDrawable(editDrawable);

                isEditing = false;

                editDoneButton.setOnClickListener(view -> {
                    if (isEditing) {
                        titleView.setInputType(InputType.TYPE_NULL);
                        titleView.setOnKeyListener((view1, i, keyEvent) -> true);
                        recordView.setInputType(InputType.TYPE_NULL);
                        recordView.setOnKeyListener((view1, i, keyEvent) -> true);
                        recordView.setSingleLine(false);

                        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_mode_edit_black_24dp);
                        editDoneButton.setImageDrawable(drawable);

                        isEditing = false;
                    } else {
                        titleView.setInputType(InputType.TYPE_CLASS_TEXT);
                        titleView.setOnKeyListener(null);
                        recordView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                        recordView.setOnKeyListener(null);

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

        if (action) {
            photos.add(new Photo("Add a photo", "photo"));
            if (photosUri != null) {
                for (String photoUri : photosUri) {
                    photos.add(new Photo("dipa", photoUri));
                }
            } else {
                photosUri = new ArrayList<>();
            }


            photosAdapter = new PhotosAdapter(this, photos);
            photosAdapter.setListener(position -> {
                if (position == 0) {
                    verifyPermissions();
                    SelectPhotoDialog selectPhotoDialog = new SelectPhotoDialog();
                    selectPhotoDialog.show(getSupportFragmentManager(), getString(R.string.choose_take_photo));
                }
            });
            photosRecycler.setAdapter(photosAdapter);

            LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
            photosRecycler.setLayoutManager(layoutManager);

            db = FirebaseFirestore.getInstance();
            recordsRef = db.collection("records");
        }


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
        if (checkChanges()) {
            intent.putExtra(TITLE, titleView.getText().toString());
            intent.putExtra(RECORD, recordView.getText().toString());
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

        Photo photo = new Photo(imageName, imagePath.toString());
        Log.v(MainActivity.TAG, imagePath.toString());

        isPhotoAdded = true;
        saveImageToFirebase(imagePath, imageName);

        photosAdapter.addPhoto(photo);
    }

    @Override
    public void getTakenImage(File image) {
        Log.v(MainActivity.TAG, Uri.fromFile(image).toString());

        String imageName = image.getName().substring(0, image.getName().lastIndexOf('.'));
        Photo photo = new Photo(imageName, Uri.fromFile(image).toString());

        isPhotoAdded = true;
        saveImageToFirebase(Uri.fromFile(image), imageName);
        photosAdapter.addPhoto(photo);
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
                isPhotoAdded;
    }

    private void confirmDeleteDialog() {
        AlertDialog.Builder deleteDialog = new AlertDialog.Builder(this);
        deleteDialog.setMessage(R.string.deleteConfirmation);
        deleteDialog.setCancelable(false);

        deleteDialog.setPositiveButton(R.string.delete, (dialogInterface, i) -> {
            setResult(MainActivity.DELETE_NOTE, intent);
            finish();
        });

        deleteDialog.setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
        });

        deleteDialog.create().show();
    }

    private void saveImageToFirebase(Uri photo, String photoName) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        String userID = user.getUid();

        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("images/users/" + userID + "/" + photoName);
        storageReference.putFile(photo).addOnSuccessListener(taskSnapshot -> {
            Log.v(MainActivity.TAG, "Dopy");

            Task<Uri> result = taskSnapshot.getMetadata().getReference().getDownloadUrl();
            result.addOnSuccessListener(uri -> {
                Log.v(MainActivity.TAG, "Tell me this shit - " + uri.toString());
                String photoUri = uri.toString();
                recordsRef.document(recordId).update("photos", FieldValue.arrayUnion(photoUri))
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                photosUri.add(0, photoUri);
                                Log.v(MainActivity.TAG, "Photo URL was bound to record");
                            } else {
                                Log.v(MainActivity.TAG, "Photo URL binding error");
                            }
                        });

            });

        }).addOnFailureListener(e -> {
            Log.v(MainActivity.TAG, "Shitty");
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
        String date = i1+1 + "/" + i2 + "/" + i;
        calendarPicker.setText(date);
    }
}
