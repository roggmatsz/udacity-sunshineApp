package com.example.android.sunshine.app;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.os.AsyncTaskCompat;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.StringTokenizer;

public class ForecastFragment extends Fragment {
    final String LOG_TAG = ForecastFragment.class.getSimpleName();

    ArrayAdapter<String> adapter;
    ListView listView;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        //fetch weather information on app start.
        updateWeather();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        adapter = new ArrayAdapter<>(
                getActivity(),
                R.layout.list_item_forecast,
                R.id.list_item_forecast_textview,
                new ArrayList<String>()
        );

        //get a reference to the View's ListView
        listView = (ListView) rootView.findViewById(R.id.list_view_forecast);

        //Assign the ArrayAdapter to the ListView
        listView.setAdapter(adapter);

        //Creates a tap listener that shows a Toast when a ListView item is clicked.
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent detailIntent = new Intent(getActivity(), DetailActivity.class);
                detailIntent.putExtra(Intent.EXTRA_TEXT, adapter.getItem(position));
                startActivity(detailIntent);
            }
        });
        return rootView;
    }

    private void updateWeather() {
        FetchWeatherTask task = new FetchWeatherTask();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String location = preferences.getString(getString(R.string.pref_location_key),
                getString(R.string.pref_location_default));
        task.execute(location);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                updateWeather();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //converts the UNIX timestamp into milliseconds
    private String getReadableDateString(long time) {
        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd", Locale.US);
        return shortenedDateFormat.format(time);
    }

    //prettifies weather values returned from JSON call
    private String formatHighLows(double high, double low, String unitType) {
        if(unitType.equals(getString(R.string.pref_unit_value_imperial))) {
            high = (high * 1.8) + 32;
            low = (low * 1.8) + 32;
        } else if(!unitType.equals(getString(R.string.pref_unit_value_metric))) {
            Log.d(LOG_TAG, "Unit type not found:" + unitType);
        }

        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);
        return roundedHigh + "/" + roundedLow;
    }

    private String[] getWeatherDataFromJson(String forecastJsonString, int numDays)
        throws JSONException {

        //constants representing the JSON objects used.
        final String OWM_LIST = "list";
        final String OWM_MAIN = "main";
        final String OWM_MAX = "temp_max";
        final String OWM_MIN = "temp_min";
        final String OWM_DESCRIPTION = "main";
        final String OWM_WEATHER = "weather";

        String[] resultStrs = new String[numDays];

        JSONObject forecastJson = new JSONObject(forecastJsonString);
        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);


        Time dayTime = new Time();
        dayTime.setToNow();

        int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);
        dayTime = new Time();

        for(int i = 0; i < weatherArray.length(); i++) {
            String day;
            String description;
            String highLow;

            //get JSON object corresponding to the ith day
            JSONObject dayForecast = weatherArray.getJSONObject(i);

            //convert the long date/time json returns into a more conventional date/time
            day = getReadableDateString(dayTime.setJulianDay(julianStartDay + i));

            //get weather description, contained in an array called "weather" of size 1
            JSONArray weatherObject = dayForecast.getJSONArray(OWM_WEATHER);
            description = weatherObject.getJSONObject(0).getString(OWM_DESCRIPTION);

            //get temperature values, contained in object called "temp"
            JSONObject mainObject = dayForecast.getJSONObject(OWM_MAIN);
            double high = mainObject.getDouble(OWM_MAX);
            double low = mainObject.getDouble(OWM_MIN);

            //format the high and low temps according to unit type from settings; concatenate day,
            // desc, and formatted highLow
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String unitType = preferences.getString(getString(R.string.pref_units_key),
                    getString(R.string.pref_unit_defaultValue));
            highLow = formatHighLows(high, low, unitType);
            resultStrs[i] = day + " - " + description + " - " + highLow;
        }

        return resultStrs;
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {
        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        @Override
        protected String[] doInBackground(String... params) {
            HttpURLConnection urlConnection = null;
            BufferedReader bufferedReader = null;
            String forecastJsonStr = null;
            String format = "json";
            String units = "metric";
            int numDays = 7;

            if(params.length == 0) {
                return null;
            }

            try {
                final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast?";
                final String QUERY_PARAM = "zip";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "cnt";
                final String APPID_PARAM = "APPID";

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                        .appendQueryParameter(APPID_PARAM, BuildConfig.OWM_API_KEY)
                        .build();

                URL url = new URL(builtUri.toString());
                Log.v(LOG_TAG, "URI: " + builtUri.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if(inputStream == null) {
                    forecastJsonStr = null;
                    return null;
                }
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while((line = bufferedReader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                if(buffer.length() == 0) {
                    forecastJsonStr = null;
                    return null;
                }
                forecastJsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                forecastJsonStr = null;
                return null;
            } finally {
                if(urlConnection != null) {
                    urlConnection.disconnect();
                }
                if(bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream ", e);
                    }
                }
            }
            Log.i(LOG_TAG, forecastJsonStr);
            try {
                return getWeatherDataFromJson(forecastJsonStr, numDays);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String[] strings) {
            if(strings != null) {
                adapter = new ArrayAdapter<>(
                        getActivity(),
                        R.layout.list_item_forecast,
                        R.id.list_item_forecast_textview,
                        new ArrayList<String>(Arrays.asList(strings))
                );

                listView.setAdapter(adapter);
            }

        }
    }
}