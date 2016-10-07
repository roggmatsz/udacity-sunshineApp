package com.example.android.sunshine.app;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class DetailActivity extends ActionBarActivity {
    static final String LOG_TAG = DetailFragment.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        if(savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new DetailFragment()).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //add items to action bar if present
        getMenuInflater().inflate(R.menu.detail, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //handle action bar taps here
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static class DetailFragment extends Fragment {

        public DetailFragment() {
            //Tells fragment to call onCreateOptionsMenu()
            setHasOptionsMenu(true);
        }

        private static final String LOG_TAG = DetailFragment.class.getSimpleName();
        private static final String SHARE_HASHTAG = "#sunshineapp";
        private String forecastString;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

            Intent intent = getActivity().getIntent();

            //check that an Intent exists and contains EXTRA_TEXT before attempting
            //to assign its value to testTextView
            if(intent != null && intent.hasExtra(Intent.EXTRA_TEXT)) {
                ((TextView) rootView.findViewById(R.id.testTextView))
                        .setText(intent.getStringExtra(Intent.EXTRA_TEXT));
                forecastString = intent.getStringExtra(Intent.EXTRA_TEXT);
            }

            return rootView;
        }
        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

            //get share item
            MenuItem shareItem = menu.findItem(R.id.action_share);

            //get share provider
            ShareActionProvider shareActionProvider =
                    (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);

            //add an intent to the share provider.
            if(shareActionProvider != null) {
                shareActionProvider.setShareIntent(makeShareForecastIntent());
            } else {
                Log.d(LOG_TAG, "Share Provider is null.");
            }
        }

        private Intent makeShareForecastIntent() {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);

            //keeps from shared to application to be added into Sunshine's
            //activity stack. IE: tapping back returns user to Sunshine and not
            //the shared app main activity.
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    forecastString + " " + SHARE_HASHTAG);
            return shareIntent;
        }
    }
}
