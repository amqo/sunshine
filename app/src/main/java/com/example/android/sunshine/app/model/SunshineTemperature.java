package com.example.android.sunshine.app.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by alberto on 10/3/16.
 */
public class SunshineTemperature {

    @SerializedName("day")
    private double mDay;

    @SerializedName("min")
    private double mMinTemp;

    @SerializedName("max")
    private double mMaxTemp;

    public double getDay() {
        return mDay;
    }

    public double getMin() {
        return mMinTemp;
    }

    public double getMax() {
        return mMaxTemp;
    }
}
