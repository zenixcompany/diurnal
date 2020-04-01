package com.example.calendar;

import java.util.Date;

public class Record {
    private long id;
    private long listPosition;

    private String title;
    private String text;
    private Date date;

    public Record(Date date) {
        this.date = date;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getListPosition() {
        return listPosition;
    }

    public void setListPosition(long listPosition) {
        this.listPosition = listPosition;
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
}
