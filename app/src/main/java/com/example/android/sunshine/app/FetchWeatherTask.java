/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.sunshine.app;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.data.WeatherContract.WeatherEntry;
import com.example.android.sunshine.app.data.WeatherDbHelper;
import com.example.android.sunshine.app.model.SunshineCity;
import com.example.android.sunshine.app.model.SunshineDay;
import com.example.android.sunshine.app.model.SunshineInfo;
import com.example.android.sunshine.app.model.SunshineTemperature;
import com.example.android.sunshine.app.model.SunshineWeather;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FetchWeatherTask implements Callback<SunshineDay> {

    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

    private final Context mContext;

    public FetchWeatherTask(Context context) {
        mContext = context;
    }

//    private boolean DEBUG = true;

    /**
     * Helper method to handle insertion of a new location in the weather database.
     *
     * @param locationSetting The location string used to request updates from the server.
     * @param cityName        A human-readable city name, e.g "Mountain View"
     * @param lat             the latitude of the city
     * @param lon             the longitude of the city
     * @return the row ID of the added location.
     */
    long addLocation(String locationSetting, String cityName, double lat, double lon) {
        long locationId;

        Cursor locationCursor = mContext.getContentResolver().query(
                WeatherContract.LocationEntry.CONTENT_URI,
                new String[]{WeatherContract.LocationEntry._ID},
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                new String[]{locationSetting},
                null
        );

        if (locationCursor.moveToFirst()) {
            int locationIdIndex = locationCursor.getColumnIndex(WeatherContract.LocationEntry._ID);
            locationId = locationCursor.getLong(locationIdIndex);
        } else {
            ContentValues locationValues = new ContentValues();

            locationValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);

            Uri insertedUri = mContext.getContentResolver().insert(
                    WeatherContract.LocationEntry.CONTENT_URI,
                    locationValues);

            locationId = ContentUris.parseId(insertedUri);
        }

        return locationId;
    }

    public void reload() {
        final String FORMAT_PARAM = "mode";
        final String UNITS_PARAM = "units";
        final String DAYS_PARAM = "cnt";
        final String LANG_PARAM = "lang";
        final String APPID_PARAM = "APPID";

        final int NUM_DAYS = 7;
        final double VERSION = 2.5;

        Interceptor interceptor = new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                HttpUrl.Builder urlBuilder = request.url().newBuilder();

                urlBuilder = urlBuilder.addQueryParameter(FORMAT_PARAM, "json")
                        .addQueryParameter(UNITS_PARAM, "metric")
                        .addQueryParameter(APPID_PARAM, BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                        .addQueryParameter(LANG_PARAM, Locale.getDefault().getLanguage())
                        .addQueryParameter(DAYS_PARAM, Integer.toString(NUM_DAYS));

                HttpUrl url = urlBuilder.build();

                request = request.newBuilder().url(url).build();
                return chain.proceed(request);
            }
        };

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        clientBuilder.interceptors().add(interceptor);
        OkHttpClient client = clientBuilder.build();

        String baseUrl = "http://api.openweathermap.org/";
        Retrofit retrofit = new Retrofit.Builder()
                .client(client)
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        SunshineEndpointInterface sunshineAPI = retrofit.create(
                SunshineEndpointInterface.class);

        String locationValue = getLocationValueFromPreferences();

        Call<SunshineDay> call = sunshineAPI.getDays(VERSION, locationValue);
        call.enqueue(this);
    }

    private String getLocationValueFromPreferences() {
        String locationKey = mContext.getString(R.string.pref_location_key);
        String locationDefaultValue = mContext.getString(R.string.pref_location_default);
        return PreferenceManager
                .getDefaultSharedPreferences(mContext)
                .getString(locationKey, locationDefaultValue);
    }

    @Override
    public void onResponse(Call<SunshineDay> call, Response<SunshineDay> response) {

        SunshineDay sunshineDay = response.body();

        Vector<ContentValues> cVVector = new Vector<>(sunshineDay.getList().size());

        Time dayTime = new Time();
        dayTime.setToNow();
        int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);
        dayTime = new Time();

        SunshineCity city = sunshineDay.getCity();

        String locationSetting = getLocationValueFromPreferences();

        long locationId = addLocation(locationSetting, city.getName(),
                city.getCityCoords().getLat(), city.getCityCoords().getLong());

        int i = 0;
        for (SunshineInfo sunshineInfo : sunshineDay.getList()) {
            SunshineTemperature temperature = sunshineInfo.getTemperature();
            SunshineWeather weather = sunshineInfo.getWeather();
            if (weather != null) {
                String description = weather.getDescription();
                long dateTime;
                dateTime = dayTime.setJulianDay(julianStartDay + (i++));

                ContentValues weatherValues = new ContentValues();

                weatherValues.put(WeatherEntry.COLUMN_LOC_KEY, locationId);
                weatherValues.put(WeatherEntry.COLUMN_DATE, dateTime);
                weatherValues.put(WeatherEntry.COLUMN_HUMIDITY, sunshineInfo.getHumidity());
                weatherValues.put(WeatherEntry.COLUMN_PRESSURE, sunshineInfo.getPressure());
                weatherValues.put(WeatherEntry.COLUMN_WIND_SPEED, sunshineInfo.getSpeed());
                weatherValues.put(WeatherEntry.COLUMN_DEGREES, sunshineInfo.getDegrees());
                weatherValues.put(WeatherEntry.COLUMN_MAX_TEMP, temperature.getMax());
                weatherValues.put(WeatherEntry.COLUMN_MIN_TEMP, temperature.getMin());
                weatherValues.put(WeatherEntry.COLUMN_SHORT_DESC, description);
                weatherValues.put(WeatherEntry.COLUMN_WEATHER_ID, weather.getId());

                cVVector.add(weatherValues);
            }

        }

        // add to database
        if (cVVector.size() > 0) {
            ContentValues[] contentValues = cVVector.toArray(
                    new ContentValues[cVVector.size()]);
            int inserts = mContext.getContentResolver().bulkInsert(
                    WeatherEntry.CONTENT_URI,
                    contentValues);
            Log.d(LOG_TAG, "BuilkInsert Complete. " + inserts + " Inserted");
        }
    }

    @Override
    public void onFailure(Call<SunshineDay> call, Throwable t) {
        Toast.makeText(mContext, t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
    }
}