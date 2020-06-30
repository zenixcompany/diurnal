package com.example.calendar.record;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.calendar.R;
import com.example.calendar.mainscreen.MainScreenActivity;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;

public class RecordSelectPhotoDialog extends DialogFragment {
    // Constants
    private static final int REQUEST_STORAGE_PERMISSIONS = 298;
    private static final int REQUEST_CAMERA_PERMISSIONS = 299;
    private static final int REQUEST_TAKE_PHOTO = 300;
    private static final int REQUEST_CHOOSE_PHOTO = 301;

    // UI
    private TextView selectPhoto;
    private TextView takePhoto;

    // Variables
    private OnPhotoSelectedListener photoSelectedListener;

    private String currentPhotoPath;
    private File photoFile = null;

    public interface OnPhotoSelectedListener {
        void getChosenImage(Uri imagePath);
        void getTakenImage(File image);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_selectphoto, container, false);

        selectPhoto = view.findViewById(R.id.dialog_choosePhoto);
        selectPhoto.setOnClickListener(view1 -> {
            if (checkStoragePermissions()) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_CHOOSE_PHOTO);
            }
        });

        takePhoto = view.findViewById(R.id.dialog_takePhoto);
        takePhoto.setOnClickListener(view1 -> {
            if (checkCameraPermissions()) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (intent.resolveActivity(getContext().getPackageManager())  != null) {
                    photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException e) {
                        Log.e(MainScreenActivity.TAG, e.toString());
                    }

                    if (photoFile != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            Uri photoUri = FileProvider.getUriForFile(getContext(),
                                    "com.example.calendar.fileprovider",
                                    photoFile);
                            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                        }
                        else {
                            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                        }
                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivityForResult(intent, REQUEST_TAKE_PHOTO);
                    }
                }
            }
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CHOOSE_PHOTO) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                Uri selectedImageUri = data.getData();
                photoSelectedListener.getChosenImage(selectedImageUri);
            }
            dismiss();
        }
        else if (requestCode == REQUEST_TAKE_PHOTO) {
            if (resultCode == Activity.RESULT_OK) {
                if (photoFile != null) {
                    photoSelectedListener.getTakenImage(photoFile);
                }
            } else {
                if (photoFile != null) {
                    if (photoFile.delete())
                        Log.v(MainScreenActivity.TAG, "Canceled photo was deleted");
                }
            }
            dismiss();
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        try {
            photoSelectedListener = (OnPhotoSelectedListener) getActivity();
        } catch (ClassCastException e) {
            Log.e(MainScreenActivity.TAG, Objects.requireNonNull(e.getMessage()));
        }

        super.onAttach(context);
    }

    private boolean checkStoragePermissions() {
        String[] permissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
        };

        if (ContextCompat.checkSelfPermission(getContext(),
                permissions[0]) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getContext(),
                        permissions[1]) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
           requestPermissions(permissions, REQUEST_STORAGE_PERMISSIONS);
            return false;
        }
    }

    private boolean checkCameraPermissions() {
        String[] permissions = {
                Manifest.permission.CAMERA,
        };

        if (ContextCompat.checkSelfPermission(getContext(),
                permissions[0]) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            requestPermissions(permissions, REQUEST_CAMERA_PERMISSIONS);
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_STORAGE_PERMISSIONS:
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    selectPhoto.performClick();
                } else {
                    dismiss();
                    Snackbar.make(getActivity().findViewById(R.id.recordActivity_layout),
                            getResources().getString(R.string.permissionStorageDenied),
                            Snackbar.LENGTH_LONG)
                            .show();
                }
                break;
            case REQUEST_CAMERA_PERMISSIONS:
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePhoto.performClick();
                } else {
                    dismiss();
                    Snackbar.make(getActivity().findViewById(R.id.recordActivity_layout),
                            getResources().getString(R.string.permissionCameraDenied),
                            Snackbar.LENGTH_LONG)
                            .show();
                }
                break;
        }
    }

    public File createImageFile() throws IOException {

        File storageDir = getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile(((RecordActivity) getActivity()).createImageName(), ".jpg", storageDir);

        currentPhotoPath = image.getAbsolutePath();
        Log.v(MainScreenActivity.TAG, currentPhotoPath);
        return image;
    }
}
