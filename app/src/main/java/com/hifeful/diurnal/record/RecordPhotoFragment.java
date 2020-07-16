package com.hifeful.diurnal.record;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.hifeful.diurnal.R;
import com.squareup.picasso.Picasso;

public class RecordPhotoFragment extends Fragment {
    // UI
    private ImageView mImageView;

    // Variables
    private boolean mIsRestoring;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_photo, container, false);
        mImageView = view.findViewById(R.id.photo_full_size);

        mIsRestoring = savedInstanceState != null;

        Bundle bundle = this.getArguments();
        assert bundle != null;
        String photoUrl = bundle.getString("photoUrl");

        Picasso.with(getContext()).load(photoUrl).into(mImageView);

        return view;
    }

    @Nullable
    @Override
    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
        if (mIsRestoring) {
            mIsRestoring = false;
            return null;
        }

        if (enter) {
            return AnimatorInflater.loadAnimator(getActivity(), android.R.animator.fade_in);
        } else {
            return AnimatorInflater.loadAnimator(getActivity(), android.R.animator.fade_out);
        }
    }
}