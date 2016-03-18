package com.example.android.sunshine.app.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by alberto on 10/3/16.
 */
public class SunshineWeather {

    @SerializedName("id")
    private long mId;

    @SerializedName("main")
    private String mMainDescription;

    @SerializedName("description")
    private String mDescription;

    public long getId() {
        return mId;
    }

    public String getMain() {
        return mMainDescription;
    }

    public String getDescription() {
        return mDescription;
    }
}
