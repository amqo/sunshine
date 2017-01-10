package com.example.android.sunshine.app.model;

import com.google.gson.annotations.SerializedName;

public class SunshineCity {

    @SerializedName("name")
    private String mName;

    @SerializedName("coord")
    private SunshineCityCoords mCityCoords;

    public String getName() {
        return mName;
    }

    public SunshineCityCoords getCityCoords() {
        return mCityCoords;
    }
}
