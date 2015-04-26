package com.aware.plugin.sos_mobile_sensor.observers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.aware.ESM;
import com.aware.Screen;
import com.aware.plugin.sos_mobile_sensor.Plugin;

import java.util.Calendar;

//import android.os.Handler;
//import android.os.HandlerThread;
//import android.os.Looper;
//import android.util.Log;
//import android.util.Log;

/**
 * This broadcast receiver is meant to detect screen events, more specifically
 * when the screen turns on and off. It will also attempt to make this app power efficient
 * by turning off the noise sensor when the screen is off. 
 * @author Nischal Shrestha
 */
public class ScreenObserver extends BroadcastReceiver {
	
	//Plugin for context
	private Plugin plugin;
	public static boolean noStress = false;
	public static boolean alreadyAsked = false;
	private static final int[] hours = {10,12,14,16,18};
	
	/**
	 * Initiate ScreenObserver with the handler, and plugin for context
	 * @param plugin
	 */
	public ScreenObserver(Plugin plugin){
		this.plugin = plugin;
	}
	
	@SuppressWarnings("unused")
	@Override
	public void onReceive(Context context, Intent intent) {
		if(intent.getAction().equalsIgnoreCase(Screen.ACTION_AWARE_SCREEN_ON)) {
//			if( Aware.DEBUG )
//				Log.d("Screen","User has turned on the screen");
			Plugin.screenOnTime = System.currentTimeMillis();
			Plugin.screenIsOn = true;
            //No stressors
			for(int i = 0; i < hours.length; i++){
				if(Calendar.HOUR_OF_DAY == hours[i]){
					if(Plugin.ambient_noise.equals("silent") 	&&
					   Plugin.movement.equals("still")			&&
					   Plugin.multitasking == 0					&&
					   Plugin.text_messaging == 0				&&
					   Plugin.calendar_event != ""				&&
					   Plugin.email == 0						&&
					   Plugin.voice_messaging == 0              &&
					   Calendar.DAY_OF_WEEK > 1 				&&
					   Calendar.DAY_OF_WEEK < 7
					   ){
						noStress = true;
						plugin.CONTEXT_PRODUCER.onContext();
						final Handler handler = new Handler();
		            	handler.postDelayed(new Runnable() {
		            	    @Override
		            	    public void run() {
		            	    	Intent esm = new Intent();
				                esm.setAction(ESM.ACTION_AWARE_QUEUE_ESM);
				                String esmStr = "[" +
				                        "{'esm': {" +
				                        "'esm_type': 1, " +
				                        "'esm_title': 'Stress Rating', " +
				                        "'esm_instructions': 'Rate your stress level from 0-5', " +
				                        "'esm_submit':'Done', " +
				                        "'esm_expiration_threashold': 30, " +
				                        "'esm_trigger': 'No Stress Question' }}]";
				                esm.putExtra(ESM.EXTRA_ESM,esmStr);
				                if(Plugin.screenIsOn)
				                    plugin.sendBroadcast(esm);
		            	    }
		            	}, 5000);
		                break;
					} else{
						plugin.CONTEXT_PRODUCER.onContext(); //record no stressors event anyways
					}
				}
			}
            //It's been more than 1 min since last stressor fired
            if(Plugin.stressInit && System.currentTimeMillis() - Plugin.initStressorTime > 60000){
//                Log.d("Stress", "Running stress rating!");
                ESMObserver.lock = true;
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent rating = new Intent();
                        rating.setAction(ESM.ACTION_AWARE_QUEUE_ESM);
                        String esmStr =
                                "[" +
                                "{'esm': {" +
                                "'esm_type': 4, " +
                                "'esm_title': 'Stress Rating', " +
                                "'esm_instructions': 'Rate your stress level from 0-5', " +
                                "'esm_likert_max':5, "+
                                "'esm_likert_max_label':'Very Stressed', "+
                                "'esm_likert_min_label':'Not Stressed', "+
                                "'esm_likert_step':1, "+
                                "'esm_submit':'OK', "+
                                "'esm_expiration_threashold': 120, " +
                                "'esm_trigger': '"+Plugin.initStressor+"'}}]";
                        rating.putExtra(ESM.EXTRA_ESM, esmStr);
                        if (Plugin.screenIsOn)
                            plugin.sendBroadcast(rating);
                    }
                },5000);
            }
		}
		if(intent.getAction().equals(Screen.ACTION_AWARE_SCREEN_OFF)) {
//			if( Aware.DEBUG )
//				Log.d("Screen","User has turned off the screen");
            ESMObserver.handler.removeCallbacksAndMessages(null);
			Plugin.screenIsOn = false;
		}
	}
	
}
