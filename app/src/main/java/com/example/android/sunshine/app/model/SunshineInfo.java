package com.example.android.sunshine.app.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class SunshineInfo {

    @SerializedName("temp")
    SunshineTemperature mTemperature;

    @SerializedName("weather")
    List<SunshineWeather> mWeather = new ArrayList<>();

    @SerializedName("pressure")
    private double mPressure;

    @SerializedName("humidity")
    private double mHumidity;

    @SerializedName("speed")
    private double mSpeed;

    @SerializedName("deg")
    private double mDegrees;

    public SunshineWeather getWeather() {
        return mWeather.isEmpty() ? null : mWeather.get(0);
    }

    public SunshineTemperature getTemperature() {
        return mTemperature;
    }

    public double getPressure() {
        return mPressure;
    }

    public double getHumidity() {
        return mHumidity;
    }

    public double getSpeed() {
        return mSpeed;
    }

    public double getDegrees() {
        return mDegrees;
    }
}
