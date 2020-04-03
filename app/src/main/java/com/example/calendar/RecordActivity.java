package com.example.calendar;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.KeyEvent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.example.calendar.models.Photo;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecordActivity extends AppCompatActivity implements SelectPhotoDialog.OnPhotoSelectedListener {
    public static final int REQUEST_PERMISSIONS = 299;
    public static final int REQUEST_TAKE_PHOTO = 300;
    public static final int REQUEST_CHOOSE_PHOTO = 301;

    public static final String ACTION = "ACTION";
    public static final String CREATE_NOTE = "CREATE_NOTE";
    public static final String EDIT_NOTE = "EDIT_NOTE";

    public static final String TITLE = "TITLE";
    public static final String RECORD = "RECORD";
    public static final String NOTE_ID = "NOTE_ID";
    public static final String NOTE_POSITION = "NOTE_POSITION";
    public static final String PHOTOS = "PHOTOS";

    // false - CREATE_NOTE, true - EDIT_NOTE
    private boolean action = false;
    private boolean isEditing = false;

    private Intent intent;

    private EditText titleView;
    private EditText recordView;
    private Menu menu;

    private String title;
    private String record;
    private String recordId;
    private int recordPosition;

    private PhotosAdapter photosAdapter;

    private FirebaseFirestore db;
    private CollectionReference recordsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        titleView = findViewById(R.id.recordActivity_title);
        recordView = findViewById(R.id.recordActivity_text);
        RecyclerView photosRecycler = findViewById(R.id.recordActivity_photos_recycler);

        intent = getIntent();
        if (intent.getExtras() != null) {
            String actionStr = intent.getExtras().getString(ACTION);
            if (actionStr.contentEquals(CREATE_NOTE)) {
                action = false;
            } else if (actionStr.contentEquals(EDIT_NOTE)) {
                action = true;
                recordId = getIntent().getStringExtra(NOTE_ID);
                recordPosition = getIntent().getExtras().getInt(NOTE_POSITION);

                titleView.setText(intent.getStringExtra(TITLE));
                recordView.setText(intent.getStringExtra(RECORD));

                titleView.setInputType(InputType.TYPE_NULL);
                titleView.setOnKeyListener((view, i, keyEvent) -> true);
                recordView.setInputType(InputType.TYPE_NULL);
                recordView.setOnKeyListener((view, i, keyEvent) -> true);
                recordView.setSingleLine(false);
                isEditing = false;
            }
        }

        title = titleView.getText().toString();
        record = recordView.getText().toString();
        title = titleView.getText().toString();
        record = recordView.getText().toString();

        ArrayList<Photo> photos = new ArrayList<>();

        photos.add(new Photo("Add a photo", "photo"));
        photos.add(new Photo("Dipa", "https://sitechecker.pro/wp-content/uploads/2017/12/URL-meaning.png"));


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

        Toolbar toolbar = findViewById(R.id.recordActivity_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        db = FirebaseFirestore.getInstance();
        recordsRef = db.collection("records");

    }

    @Override
    public void onBackPressed() {
        if (checkChanges()) {
            intent.putExtra(TITLE, titleView.getText().toString());
            intent.putExtra(RECORD, recordView.getText().toString());

            setResult(RESULT_OK, intent);
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
        this.menu = menu;
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_mode_edit_black_24dp);
        menu.getItem(1).setIcon(drawable);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_record_edit_done:
                if (isEditing) {
                    titleView.setInputType(InputType.TYPE_NULL);
                    titleView.setOnKeyListener((view, i, keyEvent) -> true);
                    recordView.setInputType(InputType.TYPE_NULL);
                    recordView.setOnKeyListener((view, i, keyEvent) -> true);
                    recordView.setSingleLine(false);

                    Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_mode_edit_black_24dp);
                    item.setIcon(drawable);

                    isEditing = false;
                } else {
                    titleView.setInputType(InputType.TYPE_CLASS_TEXT);
                    titleView.setOnKeyListener(null);
                    recordView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                    recordView.setOnKeyListener(null);

                    Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_done_black_24dp);
                    item.setIcon(drawable);

                    isEditing = true;
                }
                break;
            case R.id.action_record_delete:
                confirmDeleteDialog();
                break;
        }

        return super.onOptionsItemSelected(item);
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

        saveImageToFirebase(imagePath, imageName);

        photosAdapter.addPhoto(photo);
    }

    @Override
    public void getTakenImage(File image) {
        Log.v(MainActivity.TAG, Uri.fromFile(image).toString());

        String imageName = image.getName().substring(0, image.getName().lastIndexOf('.'));
        Photo photo = new Photo(imageName, Uri.fromFile(image).toString());

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
                !record.contentEquals(recordView.getText().toString());
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
}


    //незнаю чи то треба то для великих зображень
//    private void setPic() {
//        // Get the dimensions of the View
//        int targetW = imageView.getWidth();
//        int targetH = imageView.getHeight();
//
//        // Get the dimensions of the bitmap
//        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
//        bmOptions.inJustDecodeBounds = true;
//
//        int photoW = bmOptions.outWidth;
//        int photoH = bmOptions.outHeight;
//
//        // Determine how much to scale down the image
//        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
//
//        // Decode the image file into a Bitmap sized to fill the View
//        bmOptions.inJustDecodeBounds = false;
//        bmOptions.inSampleSize = scaleFactor;
//        bmOptions.inPurgeable = true;
//
//        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
//        imageView.setImageBitmap(bitmap);
//    }


