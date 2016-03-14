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

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.example.android.sunshine.app.model.SunshineDay;
import com.example.android.sunshine.app.model.SunshineInfo;
import com.example.android.sunshine.app.model.SunshineTemperature;
import com.example.android.sunshine.app.model.SunshineWeather;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Encapsulates fetching the forecast and displaying it as a {@link ListView} layout.
 */
public class ForecastFragment extends Fragment implements Callback<SunshineDay> {

    private final String LOG_TAG = ForecastFragment.class.getSimpleName();

    private ArrayAdapter<String> mForecastAdapter;

    @Bind(R.id.listview_forecast)ListView mListView;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_refresh) {

            //FetchWeatherTask weatherTask = new FetchWeatherTask();
            //weatherTask.execute("08026");

            reloadListWithRetrofit();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        reloadListWithRetrofit();
    }

    private void reloadListWithRetrofit() {

        final String FORMAT_PARAM = "mode";
        final String UNITS_PARAM = "units";
        final String DAYS_PARAM = "cnt";
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
                    .addQueryParameter(APPID_PARAM, "f3372a01184a640f574219c51aa346e1")
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

        Call<SunshineDay>  call = sunshineAPI.getDays(VERSION, locationValue);
        call.enqueue(this);
    }

    private String getLocationValueFromPreferences() {
        String locationKey = getString(R.string.pref_location_key);
        String locationDefaultValue = getString(R.string.pref_location_default);
        return PreferenceManager
                .getDefaultSharedPreferences(getActivity())
                .getString(locationKey, locationDefaultValue);
    }

    @Override
    public void onResponse(Call<SunshineDay> call, Response<SunshineDay> response) {
        getActivity().setProgressBarIndeterminateVisibility(false);
        SunshineDay sunshineDay = response.body();

        String day = "";
        String description = "";
        String highAndLow;

        String[] resultStrs = new String[sunshineDay.getList().size()];

        Time dayTime = new Time();
        dayTime.setToNow();
        int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);
        dayTime = new Time();

        int i = 0;
        for (SunshineInfo sunshineInfo : sunshineDay.getList()) {
            SunshineTemperature temperature = sunshineInfo.getTemperature();
            highAndLow = formatHighLows(temperature.getMax(), temperature.getMin());
            SunshineWeather weather = sunshineInfo.getWeather();
            if (weather != null) {
                description = weather.getDescription();
                long dateTime;
                dateTime = dayTime.setJulianDay(julianStartDay+i);
                day = getReadableDateString(dateTime);
            }
            resultStrs[i++] = day + " - " + description + " - " + highAndLow;
        }

        if (resultStrs != null) {
            mForecastAdapter.clear();
            mForecastAdapter.addAll(resultStrs);
        }

    }

    @Override
    public void onFailure(Call<SunshineDay> call, Throwable t) {
        Toast.makeText(getActivity(), t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Create some dummy data for the ListView.  Here's a sample weekly forecast
        String[] data = {
                "Mon 6/23 - Sunny - 31/17",
                "Tue 6/24 - Foggy - 21/8",
                "Wed 6/25 - Cloudy - 22/17",
                "Thurs 6/26 - Rainy - 18/11",
                "Fri 6/27 - Foggy - 21/10",
                "Sat 6/28 - TRAPPED IN WEATHERSTATION - 23/18",
                "Sun 6/29 - Sunny - 20/7"
        };
        List<String> weekForecast = new ArrayList<>(Arrays.asList(data));

        // Now that we have some dummy forecast data, create an ArrayAdapter.
        // The ArrayAdapter will take data from a source (like our dummy forecast) and
        // use it to populate the ListView it's attached to.
        mForecastAdapter =
                new ArrayAdapter<>(
                        getActivity(), // The current context (this activity)
                        R.layout.list_item_forecast, // The name of the layout ID.
                        R.id.list_item_forecast_textview, // The ID of the textview to populate.
                        weekForecast);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        ButterKnife.bind(this, rootView);
        mListView.setAdapter(mForecastAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String forecast = mForecastAdapter.getItem(position);
//                Snackbar.make(view, forecast, Snackbar.LENGTH_LONG).show();
                Intent intent = new Intent(getActivity(), DetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, forecast);
                startActivity(intent);
            }
        });

        return rootView;
    }

    private String getReadableDateString(long time){
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
        return shortenedDateFormat.format(time);
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    private String formatHighLows(double high, double low) {
        // For presentation, assume the user doesn't care about tenths of a degree.
        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        String highLowStr = roundedHigh + "/" + roundedLow;
        return highLowStr;
    }
}
