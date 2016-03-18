package com.example.android.sunshine.app.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alberto on 10/3/16.
 */
public class SunshineDay {

    @SerializedName("city")
    SunshineCity mCity;

    @SerializedName("list")
    List<SunshineInfo> mList = new ArrayList<>();

    public SunshineCity getCity() {
        return mCity;
    }

    public List<SunshineInfo> getList() {
        return mList;
    }
}
