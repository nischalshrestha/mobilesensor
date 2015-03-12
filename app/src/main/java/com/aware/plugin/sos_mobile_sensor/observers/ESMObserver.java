package com.aware.plugin.sos_mobile_sensor.observers;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;

import com.aware.plugin.sos_mobile_sensor.Plugin;
import com.aware.plugin.sos_mobile_sensor.activities.StressActivity;
import com.aware.providers.ESM_Provider.ESM_Data;

import java.util.Calendar;

/**
 * This class follows up on false positives for the stressors
 * @author Nischal Shrestha
 */
public class ESMObserver extends ContentObserver {
	
	//Instance of our plugin
	private Plugin plugin;
	//If the noon/evening MoT False Negative prompts have been answered, set to 1 
	public static int noonCheck = 0;
	public static int eveningCheck = 0;
//	public static String title;
//	public static String status;
//	public static String trigger;
//	public static String answer;
	
	/**
	 * Initiate ESMObserver ContentObserver with the handler, and plugin for context
	 * @param handler
	 */
	public ESMObserver(Handler handler, Plugin plugin) {
		super(handler);
		this.plugin = plugin;
	}
	
	/**
	 * 
	 */
	@SuppressLint("InlinedApi")
	public void onChange(boolean selfChange){
		super.onChange(selfChange);
//		Log.d("Stress","ESM db changed");
		Cursor esm = plugin.getContentResolver().query(ESM_Data.CONTENT_URI, null, null, null, ESM_Data.TIMESTAMP + " DESC LIMIT 1");
		if( esm != null && esm.moveToFirst()) {
			String title = esm.getString(esm.getColumnIndex(ESM_Data.TITLE));
			String status = esm.getString(esm.getColumnIndex(ESM_Data.STATUS));
			String trigger = esm.getString(esm.getColumnIndex(ESM_Data.TRIGGER));
			String answer = esm.getString(esm.getColumnIndex(ESM_Data.ANSWER));
			if(title.equals("You are multitasking") //multitasking  
					|| title.equals("You were just on the phone") //voice call
					|| title.equals("You are using a chat app") //text messaging
					|| title.equals("You have just sent a text message") //text messaging
					|| title.equals("You have just received a text message") //""
					|| title.equals("You are using an email app") //email
					|| trigger.equals("Calendar Reminder") //calendar 
					|| trigger.equals("Calendar Feedback") //calendar
					|| trigger.equals("Mode of Transportation")
					&& status.equals("2")){ //they have to be answered
				
				if(answer.equals("Yes")){
//					Log.d("Stress","Fire up prompt!");
					Intent start = new Intent(plugin, StressActivity.class);
					start.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					start.putExtra("Noise", false);
					plugin.startActivity(start);
				} 
			} else if(trigger.equals("False Negative Test Alarm") && status.equals("2")){
				Calendar cal = Calendar.getInstance();
				int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
//				Log.d("MOT","User has answered the false negative test alarm");
//				Log.d("MOT","User has answered with: "+answer);
				if(hourOfDay >= Plugin.NOON && hourOfDay < Plugin.EVENING){
					noonCheck++;
				} else if(hourOfDay >= Plugin.EVENING){
					eveningCheck++;
				}
			} else if(title.equals("There is noise in the environment") || trigger.equals("Ambient Noise") 
					&& esm.getString(esm.getColumnIndex(ESM_Data.ANSWER)).equals("Yes")){
//				Log.d("Stress","inside noise");
				if(answer.equals("Yes")){    
//					Log.d("Stress","Fire up prompt!");
					Intent start = new Intent(plugin, StressActivity.class);
					start.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					start.putExtra("Noise", true);
					plugin.startActivity(start);
				}	
			} 
		} if(esm != null && ! esm.isClosed() ) esm.close();
	}

}
