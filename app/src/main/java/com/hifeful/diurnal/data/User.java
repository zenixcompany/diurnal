package com.hifeful.diurnal.data;

import android.os.Parcel;
import android.os.Parcelable;

public class User implements Parcelable {
    private String userId;
    private String username;
    private String email;
    private String photoUrl;

    public User() {
    }

    public User(String userId, String username, String email, String photoUrl) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.photoUrl = photoUrl;
    }

    protected User(Parcel in) {
        userId = in.readString();
        username = in.readString();
        email = in.readString();
        photoUrl = in.readString();
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(userId);
        parcel.writeString(username);
        parcel.writeString(email);
        parcel.writeString(photoUrl);
    }
}
