package com.example.user.translator.models;

import android.support.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class HistoryItemModel implements Comparable<HistoryItemModel> {
    @SerializedName("lang")
    @Expose
    private String lang;

    @SerializedName("textTo")
    @Expose
    private String to;

    @SerializedName("textFrom")
    @Expose
    private String from;

    @SerializedName("date")
    @Expose
    private long date;
    @SerializedName("fav")
    @Expose
    private boolean isMarkedFav;

    public HistoryItemModel(String lang, String to, String from, long date, boolean isMarkedFav) {
        this.lang = lang;
        this.to = to;
        this.from = from;
        this.date = date;
        this.isMarkedFav = isMarkedFav;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getTextFrom() {
        return from;
    }

    public String getTextTo() {
        return to;
    }

    public long getDate() {
        return date;
    }

    public boolean isMarkedFav() {
        return isMarkedFav;
    }

    public void setMarkedFav(boolean markedFav) {
        isMarkedFav = markedFav;
    }

    @Override
    public int compareTo(@NonNull HistoryItemModel o) {
        if (date > o.getDate()) {
            return -1;
        } else if (date < o.getDate()) {
            return 1;
        } else {
            return 0;
        }
    }
}
