package com.example.android.sunshine.app.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by alberto on 18/3/16.
 */
public class SunshineCityCoords {

    @SerializedName("lat")
    private double mLat;

    @SerializedName("lon")
    private double mLong;

    public double getLat() {
        return mLat;
    }

    public double getLong() {
        return mLong;
    }
}
