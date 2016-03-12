package com.example.android.sunshine.app.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alberto on 11/3/16.
 */
public class SunshineInfo {

    @SerializedName("temp")
    SunshineTemperature mTemperature;

    @SerializedName("weather")
    List<SunshineWeather> mWeather = new ArrayList<>();

    public SunshineWeather getWeather() {
        return mWeather.isEmpty() ? null : mWeather.get(0);
    }

    public SunshineTemperature getTemperature() {
        return mTemperature;
    }
}
