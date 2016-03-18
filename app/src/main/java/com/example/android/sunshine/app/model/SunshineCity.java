package com.example.android.sunshine.app.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by alberto on 18/3/16.
 */
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
