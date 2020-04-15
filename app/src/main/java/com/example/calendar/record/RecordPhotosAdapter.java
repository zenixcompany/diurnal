package com.example.calendar.record;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.example.calendar.R;
import com.example.calendar.mainscreen.MainScreenActivity;
import com.example.calendar.data.Photo;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

public class RecordPhotosAdapter extends RecyclerView.Adapter<RecordPhotosAdapter.ViewHolder> {
    private Context context;
    public ArrayList<Photo> photos;

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
        public void onDeleteImageClick(int position);
    }



    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public RecordPhotosAdapter(Context context, ArrayList<Photo> photos) {
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
        ImageButton deleteImage = cardView.findViewById(R.id.photo_delete);

        if (holder.getAdapterPosition() == 0) {
            Log.v(MainScreenActivity.TAG, "Photo position - " + holder.getAdapterPosition());
            holder.setIsRecyclable(false);
            image.setBackgroundResource(R.drawable.border);
            image.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_add_a_photo_black_24dp, null));
            deleteImage.setVisibility(View.GONE);
        }
        else{
            Picasso.with(context).load(photos.get(position).getPhotoUrl())
                    .resize(400, 400).centerInside().into(image);
        }

        deleteImage.setOnClickListener(view -> listener.onDeleteImageClick(position));

        cardView.setOnClickListener(view -> listener.onClick(position));
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
