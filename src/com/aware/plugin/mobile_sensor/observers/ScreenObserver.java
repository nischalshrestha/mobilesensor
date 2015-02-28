package com.aware.plugin.mobile_sensor.observers;

//import android.app.ActivityManager;
//import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
//import android.os.Handler;
//import android.os.HandlerThread;
//import android.os.Looper;
//import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.Screen;
//import com.aware.plugin.ambient_noise.Provider.AmbientNoise_Data;
import com.aware.plugin.mobile_sensor.Plugin;
//import com.aware.plugin.modeoftransportation.MoT_Provider.MoT;
//import com.aware.plugin.noise_level.NoiseLevel_Provider.NoiseLevel;
import com.aware.plugin.noise_level.NoiseLevel_Provider.NoiseLevel;
//import com.aware.providers.Applications_Provider.Applications_Foreground;

/**
 * This broadcast receiver is meant to detect screen events, more specifically
 * when the screen turns on and off. It will also attempt to make this app power efficient
 * by turning off the noise sensor when the screen is off. 
 * @author Nischal Shrestha
 */
public class ScreenObserver extends BroadcastReceiver {
	
	//Plugin for context
	private Plugin plugin;
	
	/**
	 * Initiate ScreenObserver with the handler, and plugin for context
	 * @param plugin
	 */
	public ScreenObserver(Plugin plugin){
		this.plugin = plugin;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
//		Log.d("Thread","Screen: Is this the main thread?"+(Looper.myLooper() == Looper.getMainLooper()));
//		Log.d("Thread","Screen running?: "+Plugin.thread_screen.getName()+" -> "+Plugin.thread_screen.getState());
		if(intent.getAction().equalsIgnoreCase(Screen.ACTION_AWARE_SCREEN_ON)) {
//			if( Aware.DEBUG )
//				Log.d("Screen","User has turned on the screen");
			Plugin.screenOnTime = System.currentTimeMillis();
			Plugin.screenIsOn = true;
			
			Aware.setSetting(plugin.getApplicationContext(), Aware_Preferences.STATUS_APPLICATIONS, true);
			Aware.setSetting(plugin.getApplicationContext(), Aware_Preferences.STATUS_INSTALLATIONS, true);
			Aware.setSetting(plugin.getApplicationContext(), Aware_Preferences.STATUS_CALLS, true);
			Aware.setSetting(plugin.getApplicationContext(), Aware_Preferences.STATUS_MESSAGES, true);
			Aware.setSetting(plugin.getApplicationContext(), Aware_Preferences.STATUS_ESM, true);
			Aware.setSetting(plugin, Aware_Preferences.DEBUG_FLAG, true);
			Intent apply = new Intent(Aware.ACTION_AWARE_REFRESH);
			plugin.sendBroadcast(apply);
			
//			Intent mot = new Intent(plugin.getApplicationContext(), com.aware.plugin.modeoftransportation.Plugin.class);
//	  	    plugin.startService(mot);
			
			//Restart noise and mot plugins
//			Log.d("Screen","Starting noise and mot sensors");
			
			//Initialize the context observers with the sensor thread for performance
			if(Plugin.noise_observer != null){
//				Log.d("Screen","Starting noise sensor observer thread");
				Intent noise = new Intent(plugin, com.aware.plugin.noise_level.Plugin.class);
			    plugin.startService(noise);
				plugin.getContentResolver().registerContentObserver(NoiseLevel.CONTENT_URI, true, Plugin.noise_observer);
			}
			
//			Log.d("Service","---------------------------");
//	        isMyServiceRunning(com.aware.ESM.class, plugin.getApplicationContext());
//	        isMyServiceRunning(com.aware.Applications.class, plugin.getApplicationContext());
//	        isMyServiceRunning(com.aware.ApplicationsJB.class, plugin.getApplicationContext());
//	        isMyServiceRunning(com.aware.Screen.class, plugin.getApplicationContext());
//	        isMyServiceRunning(com.aware.Communication.class, plugin.getApplicationContext());
//	        isMyServiceRunning(com.aware.Installations.class, plugin.getApplicationContext());
//	        Log.d("Service","---------------------------");
		}
		if(intent.getAction().equals(Screen.ACTION_AWARE_SCREEN_OFF)) {
//			if( Aware.DEBUG )
//				Log.d("Screen","User has turned off the screen");
			Plugin.screenIsOn = false;
//			Log.d("Screen","Stoping noise and mot sensors");
			
			Aware.setSetting(plugin.getApplicationContext(), Aware_Preferences.STATUS_APPLICATIONS, false);
			Aware.setSetting(plugin.getApplicationContext(), Aware_Preferences.STATUS_INSTALLATIONS, false);
			Aware.setSetting(plugin.getApplicationContext(), Aware_Preferences.STATUS_CALLS, false);
			Aware.setSetting(plugin.getApplicationContext(), Aware_Preferences.STATUS_MESSAGES, false);
			Aware.setSetting(plugin.getApplicationContext(), Aware_Preferences.STATUS_ESM, false);
			Intent refresh = new Intent(Aware.ACTION_AWARE_REFRESH);
			plugin.sendBroadcast(refresh);
			
			Intent applicationsSrv = new Intent(plugin.getApplicationContext(), com.aware.ApplicationsJB.class);
	  	    plugin.stopService(applicationsSrv);
			
			 //Checking if services are running
//			Log.d("Service","---------------------------");
//	        isMyServiceRunning(com.aware.ESM.class, plugin.getApplicationContext());
//	        isMyServiceRunning(com.aware.Applications.class, plugin.getApplicationContext());
//	        isMyServiceRunning(com.aware.ApplicationsJB.class, plugin.getApplicationContext());
//	        isMyServiceRunning(com.aware.Screen.class, plugin.getApplicationContext());
//	        isMyServiceRunning(com.aware.Communication.class, plugin.getApplicationContext());
//	        isMyServiceRunning(com.aware.Installations.class, plugin.getApplicationContext());
//	        Log.d("Service","---------------------------");
			
			//Deactive the plugins
//			Intent noise = new Intent(plugin, com.aware.plugin.noise_level.Plugin.class);
//		    plugin.stopService(noise);
//	  		Intent mot = new Intent(plugin.getApplicationContext(), com.aware.plugin.modeoftransportation.Plugin.class);
//	  	    plugin.stopService(mot);
	  	    
			//turn off noise observer
			if(NoiseObserver.s != null){
//				Log.d("Screen","Shutting down noise sensor observer thread");
				Plugin.thread_sensor_noise.removeCallbacksAndMessages(null);
				plugin.getContentResolver().unregisterContentObserver(Plugin.noise_observer);
//				Log.d("Thread","Screen: Is the noise thread alive?"+Plugin.thread_sensor_noise.getLooper().getThread().isAlive());
			} 
//			else{
//				Log.d("Screen","Noise sensor not yet calibrating so cannot shut down!");
//			}
		}
	}
	
//	private boolean isMyServiceRunning(Class<?> serviceClass,Context context) {
//		private void isMyServiceRunning(Class<?> serviceClass,Context context) {
//        ActivityManager manager = (ActivityManager)context. getSystemService(Context.ACTIVITY_SERVICE);
//        int i = 0;
//        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
//        	if(service.service.getClassName().contains("aware")){
//        		Log.d("Service",service.service.getClassName());
//        		i++;
//        	}
////            if (serviceClass.getName().equals(service.service.getClassName())) {
//////                Log.i("Service already","running");
////                Log.d("Service",serviceClass.getName()+" is running "+i);
////                return true;
////            }
//        }	
//        Log.d("Service",serviceClass.getName()+" isn't running "+i);
////        Log.i("Service not","running");
////        return false;
//    }
}
