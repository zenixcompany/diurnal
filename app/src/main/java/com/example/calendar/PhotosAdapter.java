package com.example.calendar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.example.calendar.models.Photo;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

public class PhotosAdapter extends RecyclerView.Adapter<PhotosAdapter.ViewHolder> {
    private Context context;
    private ArrayList<Photo> photos;

    private Listener listener;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private CardView cardView;

        public ViewHolder(CardView photo) {
            super(photo);
            this.cardView = photo;
        }
    }

    public static interface Listener {
        public void onClick(int position);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public PhotosAdapter(Context context, ArrayList<Photo> photos) {
        this.context = context;
        this.photos = photos;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardView cv = (CardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_photo, parent, false);

        return new ViewHolder(cv);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CardView cardView = holder.cardView;

        ImageView image = cardView.findViewById(R.id.photo);

        if (position == 0) {
            image.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.border, null));
            image.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_add_a_photo_black_24dp, null));
        }
        else{
            Picasso.with(context).load(photos.get(position).getPhotoUrl())
                    .resize(400, 400).centerInside().into(image);
        }

        cardView.setOnClickListener(view -> {
            listener.onClick(position);
        });
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    public void addPhoto(Photo photo) {
        photos.add(1, photo);
        notifyDataSetChanged();
    }
}
