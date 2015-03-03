package com.aware.plugin.mobile_sensor.activities;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.plugin.mobile_sensor.Plugin;
import com.aware.plugin.mobile_sensor.R;
import com.aware.providers.Aware_Provider;
import com.aware.providers.Aware_Provider.Aware_Plugins;
import com.aware.utils.DatabaseHelper;
import com.aware.utils.Https;

/**
 * The main user interface that lets you tweak the throttle and 
 * @author Nischal Shrestha
 * Sources: 
 * http://jhshi.me/2013/12/02/how-to-use-downloadmanager/
 * http://stackoverflow.com/questions/11392183/how-to-check-if-the-application-is-installed-or-not-in-android-programmatically
 */
public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener  {

	
	static Settings activity = null;
//	private final static long HOUR = 3600000;
	private boolean registered = false;
	private static boolean initialized = false;
	private long downloadID;
	private ProgressDialog waitingDialog;
	
	private static DatabaseHelper databaseHelper = null;
	private static SQLiteDatabase database = null;

	private static String filename;
	private static String url = "http://www.awareframework.com/";
	
	public static Settings getInstance(){
		return activity;
	}
	
	private String downloadCompleteIntentName = DownloadManager.ACTION_DOWNLOAD_COMPLETE;
	private IntentFilter downloadCompleteIntentFilter = new IntentFilter(downloadCompleteIntentName);
	private BroadcastReceiver downloadCompleteReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
//			Log.d("Download","Inside onReceive");
	    	//Ignore unrelated downloads
	    	long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L);
			if (id != downloadID) {
//			    Log.d("Download", "Ignoring unrelated download " + id);
			    return;
			}
			//Query the state of downloading
			DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
			DownloadManager.Query query = new DownloadManager.Query();
			query.setFilterById(id);
			Cursor cursor = downloadManager.query(query);
			// it shouldn't be empty, but just in case
			if (!cursor.moveToFirst()) {
//			    Log.d("Download", "Empty row");
			    return;
			}
			//Check the state and downloaded file information
			int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
			if (DownloadManager.STATUS_SUCCESSFUL != cursor.getInt(statusIndex)) {
//			    Log.d("Download", "Download Failed");
			    return;
			} else{
				waitingDialog.dismiss();
			    install(getApplicationContext().getExternalFilesDir(null)+"/"+filename);
			    ContentValues values = new ContentValues();
			    //Package name, plugin name, plugin status, plugin version
			    values.put(Aware_Plugins.PLUGIN_PACKAGE_NAME,"com.aware.plugin.mobile_sensor");
			    values.put(Aware_Plugins.PLUGIN_NAME, "Mobile Sensor");
			    values.put(Aware_Plugins.PLUGIN_AUTHOR, "Nischal Shrestha");
			    values.put(Aware_Plugins.PLUGIN_DESCRIPTION, "An experimental app to determine the factors that cause user stress");
			    values.put(Aware_Plugins.PLUGIN_STATUS, 1);
			    try {
					values.put(Aware_Plugins.PLUGIN_VERSION, getPackageManager().getPackageInfo(getPackageName(),0).versionCode);
				} catch (NameNotFoundException e) {
					e.printStackTrace();
				}
			    database.insert(Aware_Provider.DATABASE_TABLES[2], Aware_Plugins.PLUGIN_NAME, values);
			}
	    }
	};
	
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		 if (databaseHelper == null) {
	            databaseHelper = new DatabaseHelper( getApplicationContext(), Aware_Provider.DATABASE_NAME, null, Aware_Provider.DATABASE_VERSION, Aware_Provider.DATABASE_TABLES, Aware_Provider.TABLES_FIELDS );
        }
        if( databaseHelper != null && ( database == null || ! database.isOpen() )) {
            database = databaseHelper.getWritableDatabase();
        }
		addPreferencesFromResource(R.layout.activity_settings);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
		
	}
	
	private class Download_Client extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Void... arg0) {
			//Grab fresh url with the latest client
			HttpResponse response = new Https(getApplicationContext()).dataGET("https://api.awareframework.com/index.php/awaredev/framework_latest");
	        if( response != null && response.getStatusLine().getStatusCode() == 200 ) {
	        	try {
					JSONArray data = new JSONArray(EntityUtils.toString(response.getEntity()));
					JSONObject latest_framework = data.getJSONObject(0);
//					if( Aware.DEBUG ) Log.d(Aware.TAG, "Latest: " + latest_framework.toString());
					filename = latest_framework.getString("filename");
					return true;
				} catch (ParseException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (JSONException e) {
					e.printStackTrace();
				}
	        } else {
	        	if( Aware.DEBUG ) Log.d(Aware.TAG, "Unable to fetch latest framework from AWARE repository...");
	        }
			return false;
		}
		
		@Override
    	protected void onPostExecute(Boolean result) {
			if(result){
//				Log.d("Download",filename);
				url += filename;
				getApplicationContext().registerReceiver(downloadCompleteReceiver, downloadCompleteIntentFilter);
				registered = true;
				DownloadManager downloadManager = (DownloadManager) getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
//				Log.d("Download",url);
				// only download via WIFI
				DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
				request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
				request.setTitle("Aware Client");
				request.setDescription("Downloading the Aware client");
				// we just want to download silently
				request.setVisibleInDownloadsUi(false);
				request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
				request.setDestinationInExternalFilesDir(getApplicationContext(), null, filename);
				// enqueue this request
				downloadID = downloadManager.enqueue(request);
			}
//			else{
//				Log.d("Download","Result came out false..");
//			}
		}
		
	}
	
	private boolean appInstalledOrNot(String uri) {
        PackageManager pm = getPackageManager();
        boolean app_installed = false;
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            app_installed = true;
        }
        catch (PackageManager.NameNotFoundException e) {
            app_installed = false;
        }
        return app_installed ;
    }
	
	protected void install(String fileName) {
	    Intent install = new Intent(Intent.ACTION_VIEW);
	    install.setDataAndType(Uri.fromFile(new File(fileName)),
	            "application/vnd.android.package-archive");
	    startActivity(install);
	}
	
	@SuppressWarnings("deprecation")
	private void initialize(){
		if(registered)
			getApplicationContext().unregisterReceiver(downloadCompleteReceiver);
		//Enter the participant ID
		final EditText inputID = new EditText(Settings.this);
		final AlertDialog idDialog =  new AlertDialog.Builder(Settings.this).create();
//		idDialog.setTitle("Device Name");
		idDialog.setTitle("Participant ID");
		idDialog.setMessage("Please enter the participant ID");
		idDialog.setButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Plugin.participantID = inputID.getText().toString();
				String attributes = "Date, Timestamp, Participant ID, Multitasking, Mode of Transportation, Noise Level, Voice Call,"
						+ "Text Messaging, Email, Calendar Event, Installations, Stress Rating, Loudness Rating";
				try {
//					File exists = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+Plugin.outputFile);
					File exists = new File(Environment.getExternalStorageDirectory() + "/AWARE" + Plugin.outputFile);
					if(!exists.exists()){
//						FileWriter writer = new FileWriter(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+Plugin.outputFile,true);
						FileWriter writer = new FileWriter(Environment.getExternalStorageDirectory() + "/AWARE" + Plugin.outputFile,true);
						writer.append(attributes+"\n");
			    		writer.flush();
			    		writer.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				initialized = true;
			}
		});
	    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
	                        LinearLayout.LayoutParams.MATCH_PARENT,
	                        LinearLayout.LayoutParams.MATCH_PARENT);
	    inputID.setLayoutParams(lp);
	    idDialog.setView(inputID);
	    idDialog.setCancelable(false);

		//Make user turn on accessibility
	    if(activity == null){
	    	activity = this;
        	AlertDialog accessibility;
    	    AlertDialog.Builder builder = new AlertDialog.Builder(Settings.this);
    	    builder.setMessage("Please activate AWARE and Mobile Sensor on the Accessibility Services!");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    Intent accessibilitySettings = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    accessibilitySettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT );
                    startActivity(accessibilitySettings);
            	    idDialog.show();
                }
            });
            accessibility = builder.create();
        	accessibility.show();
        }
	}
	
	/**
	 * Returns whether or not the Plugin has been successfully initialized
	 * @return
	 */
	public static Boolean isInitialized(){
		return initialized;
	}
	
	@Override
    protected void onResume() {
        super.onResume();
        if(!appInstalledOrNot("com.aware")){
			waitingDialog = ProgressDialog.show(Settings.this, "Downloading AWARE client",
	    		    "Please wait a few seconds...", true);
			new Download_Client().execute();
	} else if(activity == null){
        	initialize();
        }
        //Checking if services are running
//        isMyServiceRunning(com.aware.ESM.class, this);
//        isMyServiceRunning(com.aware.ApplicationsJB.class, this);
//        isMyServiceRunning(com.aware.Screen.class, this);
//        isMyServiceRunning(com.aware.Communication.class, this);
//        isMyServiceRunning(com.aware.Installations.class, this);
    }
	
//	private boolean isMyServiceRunning(Class<?> serviceClass,Context context) {
//        ActivityManager manager = (ActivityManager)context. getSystemService(Context.ACTIVITY_SERVICE);
//        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
//            if (serviceClass.getName().equals(service.service.getClassName())) {
////                Log.i("Service already","running");
//                Log.d("Service",serviceClass.getName()+" is running");
//                return true;
//            }
//        }
//        Log.d("Service",serviceClass.getName()+" isn't running");
////        Log.i("Service not","running");
//        return false;
//    }
	
	@SuppressWarnings("deprecation")
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		
		Preference preference = (Preference) findPreference(key);
//		if( preference.getKey().equals("prefGlobalThrottle")){
//			String num = sharedPreferences.getString("prefGlobalThrottle", "NULL");
//			if(!num.equals("NULL") && !num.equals("")){
////				Log.d("Settings","Throttle b4: "+Plugin.throttle);
//				Plugin.throttle = Integer.parseInt(num)*HOUR;
//				runOnUiThread(new Runnable() {
//		    	  @Override
//		    	  public void run() {
//		    	    Toast.makeText(getApplicationContext(), "Wrong password!", Toast.LENGTH_LONG).show();
//		    	  }
//		        });
////				Log.d("Settings","Throttle after: "+Plugin.throttle);
//			}else{
//				EditTextPreference edit = (EditTextPreference) findPreference("prefGlobalThrottle");
//				edit.setText(""+Plugin.throttle/HOUR);
//			}
//		} 
//		else 
		if( preference.getKey().equals("prefNoiseCalibration")) {
//			Log.d("Settings",sharedPreferences.getString("prefNoiseCalibration", "NULL"));
			if(sharedPreferences.getString("prefNoiseCalibration","NULL").equals("pass")){
				Intent intent = new Intent(this, NoiseCalibrationActivity.class);
				intent.putExtra("Class",Plugin.class);
				startActivity(intent);
			} else{
				Toast.makeText(getApplicationContext(), "Wrong password!", Toast.LENGTH_LONG).show();
			}
		}
//		else if( preference.getKey().equals("prefTimeElapsed")) {
////			Log.d("Settings",sharedPreferences.getString("prefTimeElapsed", "NULL"));
////			if(sharedPreferences.getString("prefNoiseCalibration","NULL").equals("pass")){
////				Intent intent = new Intent(this, NoiseCalibrationActivity.class);
////				intent.putExtra("Class",Plugin.class);
////				startActivity(intent);
////			} else{
////				Toast.makeText(getApplicationContext(), "Wrong password!", Toast.LENGTH_LONG).show();
////			}
//			Intent i = new Intent(this, TimeElapsedActivity.class);
//			startActivity(i);
//		}
	}

}
