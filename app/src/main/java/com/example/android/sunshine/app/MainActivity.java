package com.example.android.sunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new ForecastFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_view_on_map:
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                showMap(preferences.getString(getString(R.string.pref_location_key),
                        getString(R.string.pref_location_default)));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void showMap(String zipCode) {
        final String BASE_URI = "geo:0,0?";
        final String QUERY = "q";

        Uri builtUri = Uri.parse(BASE_URI).buildUpon()
                .appendQueryParameter(QUERY, zipCode)
                .build();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(builtUri);

        if(intent.resolveActivity(this.getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast toast = Toast.makeText(this, "No map app available.", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

}
