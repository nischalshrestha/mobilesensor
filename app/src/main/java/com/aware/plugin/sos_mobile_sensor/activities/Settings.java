package com.aware.plugin.sos_mobile_sensor.activities;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.support.v4.view.accessibility.AccessibilityManagerCompat;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;

import com.aware.Aware;
import com.aware.plugin.sos_mobile_sensor.R;

import java.util.List;

/**
 * The main user interface that lets you activate/deactivate 
 * @author Nischal Shrestha
 * 
 * Sources: 
 * http://jhshi.me/2013/12/02/how-to-use-downloadmanager/
 * http://stackoverflow.com/questions/11392183/how-to-check-if-the-application-is-installed-or-not-in-android-programmatically
 */
public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener  {

	/**
	* Broadcasted event: the user has turned on his phone
	*/
	public static final String STATUS_PLUGIN_SOS_MOBILE_SENSOR = "status_plugin_sos_mobile_sensor";

	public static boolean initialized = false;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.activity_settings);

	}
	
	@SuppressWarnings("deprecation")
	private void initialize(){
		//Make user turn on accessibility
	    if(!isAccessibilityServiceActive(getApplicationContext())){
        	AlertDialog accessibility;
    	    AlertDialog.Builder builder = new AlertDialog.Builder(Settings.this);
    	    builder.setMessage("Please activate AWARE on the Accessibility Services!");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    Intent accessibilitySettings = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    accessibilitySettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT );
                    startActivity(accessibilitySettings);
                    initialized = true;
                }
            });
            accessibility = builder.create();
        	accessibility.show();
        }
	}

    public static boolean isAccessibilityServiceActive(Context c) {
        AccessibilityManager accessibilityManager = (AccessibilityManager) c.getSystemService(ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> runningServices = AccessibilityManagerCompat.getEnabledAccessibilityServiceList(accessibilityManager, AccessibilityEventCompat.TYPES_ALL_MASK);
        for (AccessibilityServiceInfo service : runningServices) {
            Log.d(Aware.TAG, service.toString());
            if (service.getId().contains("com.aware")) {
                return true;
            }
        }
        return false;
    }

	@Override
	protected void onPause(){
		super.onPause();
	}
	
	@Override
    protected void onResume() {
        super.onResume();
        if(!initialized){
        	initialize();
        }
    }
	
	@SuppressWarnings("deprecation")
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		
		Preference preference = (Preference) findPreference(key);
        if( preference.getKey().toString().equals(STATUS_PLUGIN_SOS_MOBILE_SENSOR) ) {
            boolean is_active = sharedPreferences.getBoolean(key, false);
            Aware.setSetting(this, key, is_active);
            if( is_active ) {
                Aware.startPlugin(getApplicationContext(), getPackageName());
                Aware.startPlugin(getApplicationContext(), "com.aware.plugin.google.activity_recognition");
                Aware.startPlugin(getApplicationContext(), "com.aware.plugin.ambient_noise");
            } else {
                Aware.stopPlugin(getApplicationContext(), getPackageName());
       			Aware.stopPlugin(getApplicationContext(), "com.aware.plugin.google.activity_recognition");
			    Aware.stopPlugin(getApplicationContext(), "com.aware.plugin.ambient_noise");
            }
        }
	}

}