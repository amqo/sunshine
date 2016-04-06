package com.example.android.sunshine.app;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.model.SunshineCity;
import com.example.android.sunshine.app.model.SunshineDay;
import com.example.android.sunshine.app.model.SunshineInfo;
import com.example.android.sunshine.app.model.SunshineTemperature;
import com.example.android.sunshine.app.model.SunshineWeather;

import java.io.IOException;
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


public class SunshineService extends IntentService implements Callback<SunshineDay> {

    private static final String LOG_TAG = SunshineService.class.getSimpleName();

    public SunshineService() {
        super("SunshineService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
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
    }

    private String getLocationValueFromPreferences() {
        String locationKey = getString(R.string.pref_location_key);
        String locationDefaultValue = getString(R.string.pref_location_default);
        return PreferenceManager
                .getDefaultSharedPreferences(this)
                .getString(locationKey, locationDefaultValue);
    }

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

        Cursor locationCursor = getContentResolver().query(
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

            Uri insertedUri = getContentResolver().insert(
                    WeatherContract.LocationEntry.CONTENT_URI,
                    locationValues);

            locationId = ContentUris.parseId(insertedUri);
        }

        return locationId;
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

                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationId);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, dateTime);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, sunshineInfo.getHumidity());
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, sunshineInfo.getPressure());
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, sunshineInfo.getSpeed());
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, sunshineInfo.getDegrees());
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, temperature.getMax());
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, temperature.getMin());
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, description);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weather.getId());

                cVVector.add(weatherValues);
            }

        }

        // add to database
        if (cVVector.size() > 0) {
            ContentValues[] contentValues = cVVector.toArray(
                    new ContentValues[cVVector.size()]);
            int inserts = getContentResolver().bulkInsert(
                    WeatherContract.WeatherEntry.CONTENT_URI,
                    contentValues);
            Log.d(LOG_TAG, "BuilkInsert Complete. " + inserts + " Inserted");
        }
    }

    @Override
    public void onFailure(Call<SunshineDay> call, Throwable t) {
        Toast.makeText(this, t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
    }

    static public class AlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            context.startService(new Intent(context, SunshineService.class));
        }
    }
}
