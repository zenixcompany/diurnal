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
import com.google.firebase.firestore.DocumentReference;
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

public class RecordActivity extends AppCompatActivity implements SelectPhotoDialog.OnPhotoSelectedListener , View.OnClickListener{

    private ImageView mIVpicture;//Зображення

    private Button mBTNaddPicture;//Кнопка додати фото

    private File mTempPhoto;

    private String mImageUri = "";

    private String mRereference = "";

    private StorageReference mStorageRef;

    private static final int REQUEST_CODE_PERMISSION_RECEIVE_CAMERA = 102;
    private static final int REQUEST_CODE_TAKE_PHOTO = 103;

    public static final int REQUEST_PERMISSIONS = 299;
    public static final int REQUEST_TAKE_PHOTO = 300;
    public static final int REQUEST_CHOOSE_PHOTO = 301;

    public Task<Uri> getStorageReference;

    public static final String ACTION = "ACTION";
    public static final String CREATE_NOTE = "CREATE_NOTE";
    public static final String EDIT_NOTE = "EDIT_NOTE";

    public static final String TITLE = "TITLE";
    public static final String RECORD = "RECORD";
    public static final String NOTE_ID = "NOTE_ID";
    public static final String NOTE_POSITION = "NOTE_POSITION";



    // false - CREATE_NOTE, true - EDIT_NOTE
    private boolean action = false;
    private boolean isEditing = false;

    private StorageReference mStorageReference;

    private Intent intent;

    private EditText titleView;
    private EditText recordView;
    private Menu menu;
    private ImageView imageView;//findViewById

    private String title;
    private String record;
    private String recordId;
    private int recordPosition;

    private PhotosAdapter photosAdapter;
    private SelectPhotoDialog selectPhotoDialog;

    public Task<Uri> getStorageReferenceFunction(){
        return getStorageReference = mStorageReference.getDownloadUrl();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        mStorageReference = FirebaseStorage.getInstance().getReference();

        titleView = findViewById(R.id.recordActivity_title);
        recordView = findViewById(R.id.recordActivity_text);
        RecyclerView photosRecycler = findViewById(R.id.recordActivity_photos_recycler);

        intent = getIntent();
        if (intent.getExtras() != null) {
            String actionStr = intent.getExtras().getString(ACTION);
            if (actionStr.contentEquals(CREATE_NOTE)) {
                action = false;
            }
            else if (actionStr.contentEquals(EDIT_NOTE)) {
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

        photos.add(new Photo("Dipa", "https://sitechecker.pro/wp-content/uploads/2017/12/URL-meaning.png"));
        photos.add(new Photo("Dipa", "https://sitechecker.pro/wp-content/uploads/2017/12/URL-meaning.png"));
        photos.add(new Photo("Dipa", "https://sitechecker.pro/wp-content/uploads/2017/12/URL-meaning.png"));
        photos.add(new Photo("Dipa", "https://sitechecker.pro/wp-content/uploads/2017/12/URL-meaning.png"));
        photos.add(new Photo("Dipa", "https://sitechecker.pro/wp-content/uploads/2017/12/URL-meaning.png"));
        photos.add(new Photo("Dipa", "https://sitechecker.pro/wp-content/uploads/2017/12/URL-meaning.png"));
        photos.add(new Photo("Dipa", "https://sitechecker.pro/wp-content/uploads/2017/12/URL-meaning.png"));
        photos.add(new Photo("Dipa", "https://sitechecker.pro/wp-content/uploads/2017/12/URL-meaning.png"));
        photos.add(new Photo("Dipa", "https://sitechecker.pro/wp-content/uploads/2017/12/URL-meaning.png"));
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
                    recordView.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
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

    @Override
    public void getImagePath(Uri imagePath) {
        Photo photo = new Photo(selectPhotoDialog.createImageName(), imagePath.toString());
        Log.v(MainActivity.TAG, imagePath.toString());
        photosAdapter.addPhoto(photo);
    }

    @Override
    public void getImageBitmap(Bitmap bitmap) {

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

        deleteDialog.setNegativeButton(R.string.cancel, (dialogInterface, i) -> { });

        deleteDialog.create().show();
    }
//    //додає фото в галерею
//    private void galleryAddPic() {
//        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//        File f = new File(currentPhotoPath);
//        Uri contentUri = Uri.fromFile(f);
//        mediaScanIntent.setData(contentUri);
//        this.sendBroadcast(mediaScanIntent);
//    }

    /*
    Метод для добавления интента в лист интентов
    */
    public static List<Intent> addIntentsToList(Context context, List<Intent> list, Intent intent) {
        List<ResolveInfo> resInfo = context.getPackageManager().queryIntentActivities(intent, 0);
        for (ResolveInfo resolveInfo : resInfo) {
            String packageName = resolveInfo.activityInfo.packageName;
            Intent targetedIntent = new Intent(intent);
            targetedIntent.setPackage(packageName);
            list.add(targetedIntent);
        }
        return list;
    }

    //R.id.btn_add_picture - id натиснотої кнопки додати зображення
    @Override
    public void onClick(View v) {
//        if(v.getId() == R.id.btn_add_picture){
//            addPhoto();
//        }
    }

    @Override
    public void onActivityResult(int requestCode,int resultCode, Intent data){
        super.onActivityResult(requestCode , resultCode , data);
        switch (requestCode){
            case REQUEST_CODE_TAKE_PHOTO:
                if(resultCode == RESULT_OK) {
                    if (data != null && data.getData() != null) {
                        mImageUri = getRealPathFromURI(data.getData());

                        Picasso.with(getBaseContext())
                                .load(data.getData())
                                .into(mIVpicture);
                        uploadFileInFireBaseStorage(data.getData());
                    } else if (mImageUri != null) {
                        mImageUri = Uri.fromFile(mTempPhoto).toString();

                        Picasso.with(this)
                                .load(mImageUri)
                                .into(mIVpicture);
                        uploadFileInFireBaseStorage(Uri.fromFile((mTempPhoto)));
                    }
                }
                break;
        }
    }
    //Получаем абсолютный путь файла из Uri
    private String getRealPathFromURI(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        @SuppressWarnings("deprecation")
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int columnIndex = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(columnIndex);
    }

    public void uploadFileInFireBaseStorage (Uri uri){
        UploadTask uploadTask = mStorageRef.child("images/" + mRereference).putFile(uri);
        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred());
                Log.i("Load","Upload is " + progress + "% done");
            }
        }).addOnSuccessListener(taskSnapshot -> {
            getStorageReference = mStorageReference.getDownloadUrl();
////            Uri donwoldUri = taskSnapshot.getMetadata().getDownloadUrl();
//            Log.i("Load" , "Uri donwlod" + donwoldUri);
        });
    }
}
/*
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

    //створює файл для фотографії
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.calendar",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

        /*
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            imageView.setImageBitmap(imageBitmap);
        }
    }

    //незнаю чи то треба то для великих зображень
    private void setPic() {
        // Get the dimensions of the View
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
        imageView.setImageBitmap(bitmap);
    }

 */