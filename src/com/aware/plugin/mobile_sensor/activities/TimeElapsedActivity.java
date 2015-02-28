package com.aware.plugin.mobile_sensor.activities;

import java.util.ArrayList;
import java.util.Arrays;

import com.aware.plugin.mobile_sensor.Plugin;
import com.aware.plugin.mobile_sensor.R;
import com.aware.plugin.mobile_sensor.observers.MessageObserver;
import com.aware.plugin.mobile_sensor.observers.MultitaskingObserver;
import com.aware.plugin.mobile_sensor.observers.NoiseObserver;
import com.aware.plugin.mobile_sensor.observers.VoiceCallObserver;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class TimeElapsedActivity extends ActionBarActivity {
	
	private static ListView mainListView;
	private static ArrayAdapter<String> listAdapter;
	private static String[] sensors;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_time_elapsed);

//		Log.d("Elapsed","Inside onCreate()");
		
		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
		
	  // Find the ListView resource.   
	    mainListView = (ListView) findViewById( R.id.list );  
	  
	    long currentTime = System.currentTimeMillis();
	    
	    String lastMulti = (currentTime - MultitaskingObserver.lastMultiESM) < 60000 ? 
	    		((currentTime - MultitaskingObserver.lastMultiESM)/1000)+"ms" 
	    		: ((currentTime - MultitaskingObserver.lastMultiESM)/60000)+"min";
	    String lastText = (currentTime - MessageObserver.lastMessageESM) < 60000 ? 
	    		((currentTime - MessageObserver.lastMessageESM)/1000)+"ms" 
	    		: ((currentTime - MessageObserver.lastMessageESM)/60000)+"min";
	    String lastEmail = (currentTime - MultitaskingObserver.lastEmailESM) < 60000 ?
	    		((currentTime - MultitaskingObserver.lastEmailESM)/1000)+"ms"
	    		: ((currentTime - MultitaskingObserver.lastEmailESM)/60000)+"min";
	    String lastMoT = (currentTime - Plugin.lastNegativeESM) < 60000 ?
	    		((currentTime - Plugin.lastNegativeESM)/1000)+"ms"
	    		: ((currentTime - Plugin.lastNegativeESM)/60000)+"min";
	    String lastAbs = (currentTime - NoiseObserver.lastNoisyESM) < 60000 ?
	    		 ((currentTime - NoiseObserver.lastNoisyESM)/1000)+"ms"
	    		 : ((currentTime - NoiseObserver.lastNoisyESM)/60000)+"min";
	    String lastVoice = (currentTime - VoiceCallObserver.lastVoiceESM) < 60000 ?
	    		((currentTime - VoiceCallObserver.lastVoiceESM)/1000)+"ms" 
	    		: ((currentTime - VoiceCallObserver.lastVoiceESM)/60000)+"min";
	    String lastCal = (currentTime - Plugin.lastCalendarESM) < 60000 ?
	    		((currentTime - Plugin.lastCalendarESM)/1000)+"ms"
	    		: ((currentTime - Plugin.lastCalendarESM)/60000)+"min";
	    
	    // Create and populate a List of the recent ESMs
	    sensors = new String[] { 
	    		"Multitasking: "+lastMulti, 
	    		"Text Messaging: "+lastText, 
	    		"Email: "+lastEmail, 
	    		"Mode of Transportation: "+lastMoT,
	    		"Absolute Noise: "+lastAbs, 
	    		"Voice Call: "+lastVoice, 
	    		"Calendar: "+lastCal};
	    
	    ArrayList<String> sensorList = new ArrayList<String>();  
	    sensorList.addAll( Arrays.asList(sensors) );  
	      
	    // Create ArrayAdapter using the planet list.  
	    listAdapter = new ArrayAdapter<String>(this, R.layout.simplerow, sensorList);  

	    // Set the ArrayAdapter as the ListView's adapter.  
	    mainListView.setAdapter( listAdapter );  
	}
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.time_elapsed, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_time_elapsed,
					container, false);
			return rootView;
		}
	}

}
