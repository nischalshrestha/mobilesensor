package com.aware.plugin.sos_mobile_sensor.activities;

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
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceActivity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.aware.Aware;
import com.aware.plugin.sos_mobile_sensor.Plugin;
import com.aware.plugin.sos_mobile_sensor.R;
import com.aware.providers.Aware_Provider;
import com.aware.providers.Aware_Provider.Aware_Plugins;
import com.aware.utils.DatabaseHelper;
import com.aware.utils.Https;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class Settings extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static Settings activity;
    private boolean registered = false;
    public static boolean initialized = false;
    private boolean installed = false;
    private long downloadID;
    private ProgressDialog waitingDialog;

    private static DatabaseHelper databaseHelper = null;
    private static SQLiteDatabase database = null;

    private static String filename;
    private static String url = "http://www.awareframework.com/";

    public static Settings getInstance(){
        return activity;
    }

    private SharedPreferences prefs;
    private String downloadCompleteIntentName = DownloadManager.ACTION_DOWNLOAD_COMPLETE;
    private IntentFilter downloadCompleteIntentFilter = new IntentFilter(downloadCompleteIntentName);
    private BroadcastReceiver downloadCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("Download","Inside onReceive");
            //Ignore unrelated downloads
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L);
            if (id != downloadID) {
                Log.d("Download", "Ignoring unrelated download " + id);
                return;
            }
            //Query the state of downloading
            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(id);
            Cursor cursor = downloadManager.query(query);
            // it shouldn't be empty, but just in case
            if (!cursor.moveToFirst()) {
                Log.d("Download", "Empty row");
                return;
            }
            //Check the state and downloaded file information
            int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            if (DownloadManager.STATUS_SUCCESSFUL != cursor.getInt(statusIndex)) {
                Log.d("Download", "Download Failed");
                return;
            } else{
                waitingDialog.dismiss();
                install(getApplicationContext().getExternalFilesDir(null)+"/"+filename);
                ContentValues values = new ContentValues();
                //Package name, plugin name, plugin status, plugin version
                values.put(Aware_Plugins.PLUGIN_PACKAGE_NAME,"com.aware.plugin.sos_mobile_sensor");
                values.put(Aware_Plugins.PLUGIN_NAME, "Mobile Sensor");
                values.put(Aware_Plugins.PLUGIN_AUTHOR, "Nischal Shrestha");
                values.put(Aware_Plugins.PLUGIN_DESCRIPTION, "An experimental app to determine the factors that cause user stress");
                values.put(Aware_Plugins.PLUGIN_STATUS, 1);
                try {
                    values.put(Aware_Plugins.PLUGIN_VERSION, getPackageManager().getPackageInfo(getPackageName(),0).versionCode);
                    installed = true;
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                database.insert(Aware_Provider.DATABASE_TABLES[2], Aware_Plugins.PLUGIN_NAME, values);
            }
        }
    };


    protected void install(String fileName) {
        Intent install = new Intent(Intent.ACTION_VIEW);
        install.setDataAndType(Uri.fromFile(new File(fileName)),
                "application/vnd.android.package-archive");
        startActivity(install);
    }

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
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if(!appInstalledOrNot("com.aware")){
            waitingDialog = ProgressDialog.show(Settings.this, "Downloading AWARE client",
                    "Please wait a few seconds...", true);
            new Download_Client().execute();
        }

    }

    private class Download_Client extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... arg0) {
            //Grab fresh url with the latest client
            HttpResponse response = new Https(getApplicationContext()).dataGET("https://api.awareframework.com/index.php/awaredev/framework_latest",true);
            if( response != null && response.getStatusLine().getStatusCode() == 200 ) {
                try {
                    JSONArray data = new JSONArray(EntityUtils.toString(response.getEntity()));
                    JSONObject latest_framework = data.getJSONObject(0);
                    if( Aware.DEBUG ) Log.d(Aware.TAG, "Latest: " + latest_framework.toString());
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
                Log.d("Download",filename);
                url += filename;

                DownloadManager downloadManager = (DownloadManager) getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
                Log.d("Download",url);
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
            else{
                Log.d("Download","Result came out false..");
            }
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

    @SuppressWarnings("deprecation")
    private void initialize(){
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
                    File exists = new File(Environment.getExternalStorageDirectory() + "/AWARE" + Plugin.outputFile);
                    if(!exists.exists()){
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

    @Override
    protected void onPause(){
        super.onPause();
        getApplicationContext().unregisterReceiver(downloadCompleteReceiver);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getApplicationContext().registerReceiver(downloadCompleteReceiver, downloadCompleteIntentFilter);
        registered = true;
        prefs.registerOnSharedPreferenceChangeListener(this);
        if(!initialized && appInstalledOrNot("com.aware")){
            initialize();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

    }
}
