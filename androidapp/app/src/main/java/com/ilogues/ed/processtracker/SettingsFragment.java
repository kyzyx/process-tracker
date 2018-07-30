package com.ilogues.ed.processtracker;

import android.content.SharedPreferences;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.os.Bundle;

/**
 * Created by edzhang on 7/24/18.
 * Ref: https://stackoverflow.com/questions/34288925/display-the-value-of-the-edittextpreference-in-summary
 */

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    SharedPreferences sharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onResume() {
        super.onResume();

        sharedPreferences = getPreferenceManager().getSharedPreferences();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        updateSummary((EditTextPreference) this.findPreference("sheets_url"));
        updateSummary((ListPreference) this.findPreference("refresh_every"));
    }

    @Override
    public void onPause() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        Preference urlpref = this.findPreference("sheets_url");
        updateSummary((EditTextPreference) urlpref);
        Preference refpref = this.findPreference("refresh_every");
        updateSummary((ListPreference) refpref);
    }
    private void updateSummary(EditTextPreference preference) {
        preference.setSummary(preference.getText());
    }
    private void updateSummary(ListPreference preference) {
        preference.setSummary(preference.getEntry());
    }
}
