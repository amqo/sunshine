package com.example.android.sunshine.app;

import com.example.android.sunshine.app.model.SunshineDay;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by alberto on 10/3/16.
 */
public interface SunshineEndpointInterface {
    @GET("data/{version}/forecast/daily")
    Call<SunshineDay> getDays(@Path("version") double versionCode, @Query("q") String query);
}
