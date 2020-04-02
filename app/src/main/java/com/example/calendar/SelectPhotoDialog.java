package com.example.calendar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class SelectPhotoDialog extends DialogFragment {

    OnPhotoSelectedListener photoSelectedListener;

    public interface OnPhotoSelectedListener {
        void getImagePath(Uri imagePath);
        void getImageBitmap(Bitmap bitmap);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_selectphoto, container, false);

        TextView selectPhoto = view.findViewById(R.id.dialog_choosePhoto);
        selectPhoto.setOnClickListener(view1 -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, RecordActivity.REQUEST_CHOOSE_PHOTO);
        });

        TextView takePhoto = view.findViewById(R.id.dialog_takePhoto);
        takePhoto.setOnClickListener(view1 -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.setType("image/*");
            startActivityForResult(intent, RecordActivity.REQUEST_TAKE_PHOTO);
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RecordActivity.REQUEST_CHOOSE_PHOTO && resultCode == Activity.RESULT_OK &&
                data != null) {
            Uri selectedImageUri = data.getData();
            photoSelectedListener.getImagePath(selectedImageUri);

            getDialog().dismiss();
        }
        else if (requestCode == RecordActivity.REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK &&
                data != null) {
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            photoSelectedListener.getImageBitmap(bitmap);

            getDialog().dismiss();
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        try {
            photoSelectedListener = (OnPhotoSelectedListener) getActivity();
        } catch (ClassCastException e) {
            Log.e(MainActivity.TAG, Objects.requireNonNull(e.getMessage()));
        }

        super.onAttach(context);
    }
}
