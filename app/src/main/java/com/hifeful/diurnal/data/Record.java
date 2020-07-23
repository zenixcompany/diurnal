package com.hifeful.diurnal.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Date;

public class Record implements Parcelable {
    private String user_id;
    private String note_id;

    private String title;
    private String text;
    private Date date;

    private ArrayList<String> photos;

    public Record() {}

    public Record(Date date) {
        this.date = date;
    }

    public Record(String user_id, String note_id, String title, String text, Date date,
                  ArrayList<String> photos) {
        this.user_id = user_id;
        this.note_id = note_id;
        this.title = title;
        this.text = text;
        this.date = date;
        this.photos = photos;
    }

    protected Record(Parcel in) {
        user_id = in.readString();
        note_id = in.readString();
        title = in.readString();
        text = in.readString();
        photos = in.createStringArrayList();
    }

    public static final Creator<Record> CREATOR = new Creator<Record>() {
        @Override
        public Record createFromParcel(Parcel in) {
            return new Record(in);
        }

        @Override
        public Record[] newArray(int size) {
            return new Record[size];
        }
    };

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getNote_id() {
        return note_id;
    }

    public void setNote_id(String note_id) {
        this.note_id = note_id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public ArrayList<String> getPhotos() {
        return photos;
    }

    public void setPhotos(ArrayList<String> photos) {
        this.photos = photos;
    }

    @Override
    public String toString() {
        return "Record{" +
                "title='" + title + '\'' +
                ", text='" + text + '\'' +
                ", date=" + date +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(user_id);
        parcel.writeString(note_id);
        parcel.writeString(title);
        parcel.writeString(text);
        parcel.writeStringList(photos);
    }
}
