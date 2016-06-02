package com.example.android.sunshine.app;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by matamora on 5/31/2016.
 */
public class WeatherDataParser {
    final static String LOG_TAG = WeatherDataParser.class.getSimpleName();
    public WeatherDataParser() {}

    public static double getMaxTemperatureForDay(String weatherJsonString, int dayIndex) {
        JSONObject weather, dayInfo, temperatureInfo;
        JSONArray days;
        try {
            weather = new JSONObject(weatherJsonString);
            days = weather.getJSONArray("list");
            dayInfo = days.getJSONObject(dayIndex);
            temperatureInfo = dayInfo.getJSONObject("temp");
            return temperatureInfo.getDouble("max");
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error Parsing JSON");
            return -1.0;
        }
    }
}
