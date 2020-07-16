package com.hifeful.diurnal.data;

public class Photo {
    private String photoName;
    private String photoUrl;

    public Photo() {}

    public Photo(String photoName, String photoUrl) {
        this.photoName = photoName;
        this.photoUrl = photoUrl;
    }

    public String getPhotoName() {
        return photoName;
    }

    public void setPhotoName(String photoName) {
        this.photoName = photoName;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
