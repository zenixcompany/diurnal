package com.example.calendar;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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

public class RecordActivity extends AppCompatActivity implements View.OnClickListener{

    private ImageView mIVpicture;//Зображення

    private Button mBTNaddPicture;//Кнопка додати фото

    private File mTempPhoto;//файл для збереження фото з камери

    private String mImageUri = "";// Uri файлу

    private String mRereference = "";//Ключ з списку

    private Uri ImageUri;

    private StorageReference mStorageRef;

    private String downloadUrl;

    private static String myTempFileName;

    private static final int REQUEST_CODE_PERMISSION_RECEIVE_CAMERA = 102;
    private static final int REQUEST_CODE_TAKE_PHOTO = 103;
    static final int REQUEST_TAKE_PHOTO = 1;
    static final int REQUEST_IMAGE_CAPTURE = 1;

    public Task<Uri> getStorageReference;

    public static final String ACTION = "ACTION";
    public static final String CREATE_NOTE = "CREATE_NOTE";
    public static final String EDIT_NOTE = "EDIT_NOTE";

    public static final String TITLE = "TITLE";
    public static final String RECORD = "RECORD";


    // false - CREATE_NOTE, true - EDIT_NOTE
    private boolean action = false;

    //private StorageReference mStorageReference;// my func

    private Intent intent;

    private EditText titleView;
    private EditText recordView;
    private ImageView imageView;//findViewById

    private String title;
    private String record;

    String currentPhotoPath;

    /* my func
    public Task<Uri> getStorageReferenceFunction(){
        return getStorageReference = mStorageReference.getDownloadUrl();
    }


     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        //mStorageReference = FirebaseStorage.getInstance().getReference();//my func

        titleView = findViewById(R.id.recordActivity_title);
        recordView = findViewById(R.id.recordActivity_text);

        title = titleView.getText().toString();
        record = recordView.getText().toString();

        //Зображення і кнопка
        mIVpicture = (ImageView) findViewById(R.id.iv_piture);
        mBTNaddPicture = (Button) findViewById(R.id.btn_add_picture);
        //Для кнопки
        mBTNaddPicture.setOnClickListener(this);

        File localFile = null;

        mRereference = getIntent().getStringExtra("Reference");//ключ
        mStorageRef = FirebaseStorage.getInstance().getReference();//силка на файлове сховище Firebase

        try {
            localFile = createTempImageFile(getExternalCacheDir());
            final File finalLocalFile = localFile;

            mStorageRef.child("images/" + mRereference).getFile(localFile)
                    .addOnSuccessListener((OnSuccessListener<FileDownloadTask.TaskSnapshot>) taskSnapshot -> Picasso.with(getBaseContext())
                            .load(Uri.fromFile(finalLocalFile))
                            .into(mIVpicture)).addOnFailureListener((OnFailureListener) e -> Log.i("Load","" + e));

        } catch (IOException e) {
            e.printStackTrace();
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

    //додає фото в галерею
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    //Метод для добавления фото
    private void addPhoto() {

        //Проверяем разрешение на работу с камерой
        boolean isCameraPermissionGranted = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        //Проверяем разрешение на работу с внешнем хранилещем телефона
        boolean isWritePermissionGranted = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        //Если разрешения != true
        if(!isCameraPermissionGranted || !isWritePermissionGranted) {

            String[] permissions;//Разрешения которые хотим запросить у пользователя

            if (!isCameraPermissionGranted && !isWritePermissionGranted) {
                permissions = new String[] {android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
            } else if (!isCameraPermissionGranted) {
                permissions = new String[] {android.Manifest.permission.CAMERA};
            } else {
                permissions = new String[] {android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
            }
            //Запрашиваем разрешения у пользователя
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSION_RECEIVE_CAMERA);
        } else {
            //Если все разрешения получены
            try {
                mTempPhoto = createTempImageFile(getExternalCacheDir());
                mImageUri = mTempPhoto.getAbsolutePath();

                //Создаём лист с интентами для работы с изображениями
                List<Intent> intentList = new ArrayList<>();
                Intent chooserIntent = null;


                Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                takePhotoIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mTempPhoto));

                intentList = addIntentsToList(this, intentList, pickIntent);
                intentList = addIntentsToList(this, intentList, takePhotoIntent);

                if (!intentList.isEmpty()) {
                    chooserIntent = Intent.createChooser(intentList.remove(intentList.size() - 1),"Choose your image source");
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentList.toArray(new Parcelable[]{}));
                }

                /*После того как пользователь закончит работу с приложеним(которое работает с изображениями)
                 будет вызван метод onActivityResult
                */
                startActivityForResult(chooserIntent, REQUEST_CODE_TAKE_PHOTO);
            } catch (IOException e) {
                Log.e("ERROR", e.getMessage(), e);
            }
        }
    }

    public static File createTempImageFile(File storageDir) throws IOException {

        // Генерируем имя файла
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());//получаем время
        String imageFileName = "photo_" + timeStamp;//состовляем имя файла
        myTempFileName =  "photo_" + timeStamp;
        //Создаём файл
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }

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
        if(v.getId() == R.id.btn_add_picture){
            addPhoto();
        }
    }

    @Override
    public void onActivityResult(int requestCode,int resultCode, Intent data){
        super.onActivityResult(requestCode , resultCode , data);
        switch (requestCode){
            case REQUEST_CODE_TAKE_PHOTO:
                if(resultCode == RESULT_OK) {
                    if (data != null && data.getData() != null) {
                        mImageUri = getRealPathFromURI(data.getData());
                        ImageUri = data.getData();
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

    final StorageReference filePath = mStorageRef.child(ImageUri.getLastPathSegment() + myTempFileName + ".jpg");

    public void uploadFileInFireBaseStorage (Uri uri){
        UploadTask uploadTask = mStorageRef.child("images/" + mRereference).putFile(uri);
        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred());
            Log.i("Load","Upload is " + progress + "% done");
        }).addOnSuccessListener(taskSnapshot -> {
//            getStorageReference = mStorageReference.getDownloadUrl();
           downloadUrl = filePath.getDownloadUrl().toString();
            /*
            Uri downloadUrl = taskSnapshot.getMetadata().getDownloadUrl();
            Log.i("Load" , "Uri download" + downloadUrl);
             */
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


