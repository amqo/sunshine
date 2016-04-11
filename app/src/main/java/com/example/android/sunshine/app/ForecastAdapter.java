package com.example.android.sunshine.app;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * {@link ForecastAdapter} exposes a list of weather forecasts
 * from a {@link android.database.Cursor} to a {@link android.widget.ListView}.
 */
public class ForecastAdapter extends CursorAdapter {

    private static final int VIEW_TYPE_TODAY = 0;
    private static final int VIEW_TYPE_FUTURE_DAY = 1;
    private static final int VIEW_TYPE_COUNT = 2;

    private boolean mUseTodayLayout;

    public ForecastAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0 && mUseTodayLayout) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    /*
        Remember that these views are reused as needed.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        int viewType = getItemViewType(cursor.getPosition());

        int layout = viewType ==  VIEW_TYPE_TODAY ?
                R.layout.list_item_forecast_today :
                R.layout.list_item_forecast;

        View view = LayoutInflater.from(context).inflate(layout, parent, false);
        ViewHolder holder = new ViewHolder(view);
        view.setTag(holder);

        return view;
    }

    /*
        This is where we fill-in the views with the contents of the cursor.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ViewHolder holder = (ViewHolder) view.getTag();

        double highTemp = cursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP);
        holder.highView.setText(Utility.formatTemperature(context, highTemp));

        double lowTemp = cursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP);
        holder.lowView.setText(Utility.formatTemperature(context, lowTemp));

        holder.dateView.setText(Utility.getFriendlyDayString(
                context, cursor.getLong(ForecastFragment.COL_WEATHER_DATE)));

        String description = cursor.getString(ForecastFragment.COL_WEATHER_DESC);
        holder.forecastView.setText(description);

        int weatherIcon = getWeatherIcon(cursor);
        holder.iconView.setImageResource(weatherIcon);
        holder.iconView.setContentDescription(description);
    }

    private int getWeatherIcon(Cursor cursor) {
        int weatherId = cursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);
        int viewType = getItemViewType(cursor.getPosition());
        boolean isToday = viewType == VIEW_TYPE_TODAY;
        int weatherIcon = Utility.getArtResourceForWeatherCondition(weatherId);
        if (!isToday) {
            weatherIcon = Utility.getIconResourceForWeatherCondition(weatherId);
        }
        return weatherIcon;
    }

    static class ViewHolder {
        @Bind(R.id.list_item_icon) ImageView iconView;
        @Bind(R.id.list_item_high_textview) TextView highView;
        @Bind(R.id.list_item_low_textview) TextView lowView;
        @Bind(R.id.list_item_date_textview) TextView dateView;
        @Bind(R.id.list_item_forecast_textview) TextView forecastView;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}