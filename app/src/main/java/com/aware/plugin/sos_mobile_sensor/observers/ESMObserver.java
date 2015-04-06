package com.aware.plugin.sos_mobile_sensor.observers;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.util.Log;

import com.aware.ESM;
import com.aware.plugin.sos_mobile_sensor.Plugin;
import com.aware.providers.ESM_Provider.ESM_Data;

//import android.util.Log;

/**
 * This class follows up on false positives for the stressors
 * @author Nischal Shrestha
 */
public class ESMObserver extends ContentObserver {
	
	//Instance of our plugin
	private Plugin plugin;
	
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
//		if( Aware.DEBUG )
//        Log.d("ESM","ESM db changed");
		Cursor esm = plugin.getContentResolver().query(ESM_Data.CONTENT_URI, null, null, null, ESM_Data.TIMESTAMP + " DESC LIMIT 1");
		if( esm != null && esm.moveToFirst()) {
			String title = esm.getString(esm.getColumnIndex(ESM_Data.TITLE));
			String status = esm.getString(esm.getColumnIndex(ESM_Data.STATUS));
			String trigger = esm.getString(esm.getColumnIndex(ESM_Data.TRIGGER));
			String answer = esm.getString(esm.getColumnIndex(ESM_Data.ANSWER));
			if((title.equals("You are multitasking") //multitasking  
					|| title.equals("You were just on the phone") //voice call
					|| title.equals("You are using a chat app") //text messaging
					|| title.equals("You were just text messaging") //text messaging
					|| title.equals("You are using an email app") //email
					|| title.equals("Stress Rating")
					|| trigger.equals("Calendar Reminder") //calendar 
					|| trigger.equals("Calendar Feedback") //calendar followup
					|| trigger.equals("Mode of Transportation") //mot
					|| trigger.equals("Retroactive Question")
					|| trigger.equals("No Stress Question")) //stress q
				&& status.equals("2") 
				&& (answer.equals("Yes") 
				|| answer.equals("1.0") 
				|| answer.equals("2.0")
				|| answer.equals("3.0")
				|| answer.equals("4.0")
				|| answer.equals("5.0"))
				){ //they have to be answered
				if(trigger.equals("Retroactive Question")){
					Plugin.stressEvents = 0;
				}
				if(trigger.equals("No Stress Question")){
					ScreenObserver.noStress = false;
				}
				if(title.equals("Stress Rating")){
					Log.d("Stress", "Going to run stress rating verification...");
                    Intent rating = new Intent();
                    rating.setAction(ESM.ACTION_AWARE_QUEUE_ESM);
					String esmStr = 
                			"[{'esm': {" +
                            "'esm_type': 3, " +
                            "'esm_title': 'Which of these affected your stress rating?', " +
                            "'esm_instructions': 'Choose all that apply. If other, please specify!', " +
                            "'esm_checkboxes':['Multitasking','Mode of Transportation','Noise','Email','Text Messaging','Voice Messaging','Calendar Event','Other'], "+
                            "'esm_submit':'OK', " +
                            "'esm_expiration_threashold': 240, " +
                            "'esm_trigger': 'Stress Verification' }}]";
					rating.putExtra(ESM.EXTRA_ESM, esmStr);
                    if (Plugin.screenIsOn && Plugin.participantID != null)
                        plugin.sendBroadcast(rating);
                    Plugin.initStressorTime = System.currentTimeMillis();
                    Plugin.stressInit = false;
                    MultitaskingObserver.lock = false;
                    Plugin.stressCount = 0;
                    Plugin.initStressor = "";
				}
			} else if(trigger.equals("Ambient Noise") && 
					  status.equals("2")              && 
					  answer.equals("Yes")
					  ){
				if(Plugin.stressInit                                                   && 
				   Plugin.initStressor.equals("Ambient Noise")                         && 
				  (System.currentTimeMillis() - Plugin.initStressorTime >= 60000)
				    ) {
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
                            "'esm_expiration_threashold': 180, " +
                            "'esm_trigger': 'Ambient Noise' }}, " +
                            "{'esm': {" +
                            "'esm_type': 4, " +
                            "'esm_title': 'Stress Rating', " +
                            "'esm_instructions': 'Rate the noise level in your environment from 0-5', " +
                            "'esm_likert_max':5, "+
                            "'esm_likert_max_label':'Very Loud', "+
                            "'esm_likert_min_label':'Silent', "+
                            "'esm_likert_step':1, "+
                            "'esm_submit':'OK', "+
                            "'esm_expiration_threashold': 180, " +
                            "'esm_trigger': 'Ambient Noise' }}, " +
                            "{'esm': {" +
                            "'esm_type': 3, " +
                            "'esm_title': 'Which of these affected your stress rating?', " +
                            "'esm_instructions': 'Choose all that apply. If other, please specify!', " +
                            "'esm_checkboxes':['Multitasking','Mode of Transportation','Noise','Email','Text Messaging','Voice Messaging','Calendar Event','Other'], "+
                            "'esm_submit':'OK', " +
                            "'esm_expiration_threashold': 240, " +
                            "'esm_trigger': 'Stress Verification' }}]";
                    rating.putExtra(ESM.EXTRA_ESM, esmStr);
                    if (Plugin.screenIsOn && Plugin.participantID != null)
                        plugin.sendBroadcast(rating);
                    
				}
			} else if(status.equals("1")     || 
					  status.equals("3")     || 
					  answer.equals("No")    
					  ){
				if(trigger.equals("Retroactive Question")){
					Intent retro = new Intent();
	                retro.setAction(ESM.ACTION_AWARE_QUEUE_ESM);
	                String esmStr = "[" +
	                        "{'esm': {" +
	                        "'esm_type': 1, " +
	                        "'esm_title': 'Retroactive Question', " +
	                        "'esm_instructions': 'You rated above a 3.0 on the stress rating likert scale "+Plugin.stressEvents+" times today! "
	                        + "Please describe what caused your stress rating today.', " +
	                        "'esm_submit':'Done', " +
	                        "'esm_expiration_threashold': 240, " +
	                        "'esm_trigger': 'Retroactive Question' }}]";
	                retro.putExtra(ESM.EXTRA_ESM,esmStr);
	                if(Plugin.screenIsOn && Plugin.participantID != null)
	                    plugin.sendBroadcast(retro);
				} else if(trigger.equals("False Negative Test Alarm")){
					Intent falseNeg = new Intent();
					falseNeg.setAction(ESM.ACTION_AWARE_QUEUE_ESM);
				    String esmStr = "[{'esm':{"
				    		+ "'esm_type': 3, "
				    		+ "'esm_title': 'Mode of Transportation', "
				    		+ "'esm_instructions': 'What was your mode of transportation this "+Plugin.time+"?', "
				    		+ "'esm_checkboxes':['Walking','Running','Driving','Biking','Other'], "
				    		+ "'esm_submit': 'Done', "
				    		+ "'esm_expiration_threashold': 180, "
				    		+ "'esm_trigger':'False Negative Test Alarm'}}]";
				    falseNeg.putExtra(ESM.EXTRA_ESM,esmStr);
					if(Plugin.screenIsOn && Plugin.participantID != null)
						plugin.sendBroadcast(falseNeg);	
				} else{
                    Plugin.initStressorTime = System.currentTimeMillis();
                    Plugin.stressInit = false;
                    MultitaskingObserver.lock = false;
                    Plugin.stressCount = 0;
                    Plugin.initStressor = "";
                }
			}
		} if(esm != null && ! esm.isClosed() ) esm.close();
	}

}
