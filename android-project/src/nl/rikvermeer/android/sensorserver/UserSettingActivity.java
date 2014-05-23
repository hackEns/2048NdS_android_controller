package nl.rikvermeer.android.sensorserver;

import nl.rikvermeer.android.sensorserver.R;

import android.os.Bundle;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;

public class UserSettingActivity extends PreferenceActivity implements
        OnSharedPreferenceChangeListener
{
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.user_settings);
        PreferenceManager.setDefaultValues(UserSettingActivity.this, R.xml.user_settings, false);
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            setSummary(getPreferenceScreen().getPreference(i));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);
        setSummary(pref);
    }

    public void setSummary(Preference pref) {
        if (pref instanceof PreferenceCategory) {
            PreferenceCategory pCat = (PreferenceCategory) pref;
            for (int i = 0; i < pCat.getPreferenceCount(); i++) {
                setSummary(pCat.getPreference(i));
            }
        } else {
            if (pref instanceof EditTextPreference) {
                EditTextPreference editTextPref = (EditTextPreference) pref;
                pref.setSummary(editTextPref.getText());
            }
        }
    }
}
