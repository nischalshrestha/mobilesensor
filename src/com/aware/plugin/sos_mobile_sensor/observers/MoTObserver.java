package com.aware.plugin.sos_mobile_sensor.observers;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.util.Log;

import com.aware.Aware;
import com.aware.ESM;
import com.aware.plugin.modeoftransportation.MoT_Provider.MoT;
import com.aware.plugin.sos_mobile_sensor.Plugin;

/**
* A ContentObserver that will detect changes in the MoT 
* Ex: User goes from being still to walking.
*/
public class MoTObserver extends ContentObserver {
	
	private Plugin plugin;
	/** MoT variables */
	private static long last_MoT;
	//Last MoT ESM
	private static long lastMoTESM;
	
	/**
	 * Initiate MoTObserver ContentObserver with the handler, and plugin for context
	 * @param handler
	 * @param plugin
	 */
	public MoTObserver(Handler handler, Plugin plugin) {
		super(handler);
		this.plugin = plugin;
		lastMoTESM = Plugin.screenOnTime;
	}
	
	@Override
	public void onChange(boolean selfChange) {
		super.onChange(selfChange);
		
		if(Plugin.screenIsOn){
			//Figure out if the user has changed their mode of transportation
			Cursor mot = plugin.getContentResolver().query(MoT.CONTENT_URI, null, null, null, MoT.TIMESTAMP + " DESC LIMIT 1");
			if( mot != null && mot.moveToFirst()) {
				long currentTime = System.currentTimeMillis();
				int mode = mot.getInt(mot.getColumnIndex(MoT.MOT));
				if(last_MoT != mode){
//					if( Aware.DEBUG )
//						Log.d("MoT","User is in a different mode of transportation: "+mode);
					last_MoT = mode;
					//If the user has changed their mode of transportation, then prompt the ESM
					switch (mode) {
						case com.aware.plugin.modeoftransportation.Plugin.MOT_STILL:
							Plugin.movement = "still";
							break;
						case com.aware.plugin.modeoftransportation.Plugin.MOT_WALKING:
							Plugin.movement = "walking";
							break;
						case com.aware.plugin.modeoftransportation.Plugin.MOT_BIKING:
							Plugin.movement = "biking";
							break;
						case com.aware.plugin.modeoftransportation.Plugin.MOT_DRIVING:
							Plugin.movement = "driving"; 
							break;
						case com.aware.plugin.modeoftransportation.Plugin.MOT_RUNNING:
							Plugin.movement = "running"; 
							break;
						case com.aware.plugin.modeoftransportation.Plugin.MOT_TABLE:
							Plugin.movement = "You're phone is on the table"; 
							break;
					}
					//Share context
					plugin.CONTEXT_PRODUCER.onContext();
					if(currentTime-lastMoTESM >= Plugin.throttle){
						//Morning
						Calendar morning = new GregorianCalendar();
						morning.set(Calendar.HOUR_OF_DAY, Plugin.MORNING);
						morning.set(Calendar.MINUTE, 0);
						morning.set(Calendar.SECOND, 0);
						//Evening
						Calendar evening = new GregorianCalendar();
						evening.set(Calendar.HOUR_OF_DAY, Plugin.EVENING);
						evening.set(Calendar.MINUTE, 0);
						evening.set(Calendar.SECOND, 0);
						if(currentTime > morning.getTimeInMillis() && currentTime < evening.getTimeInMillis()){
							lastMoTESM = System.currentTimeMillis();
							Intent esm = new Intent();
//							if( Aware.DEBUG )
//								Log.d("Mode of Transportation","Prompting the Messaging ESM");
						    esm.setAction(ESM.ACTION_AWARE_QUEUE_ESM);
							String esmStr = "[" +
					                "{'esm': {" +
					                "'esm_type': 5, " +
					                "'esm_title': 'You are "+Plugin.movement+"', " +
					                "'esm_instructions': 'Is this true?', " +
					                "'esm_quick_answers':  ['Yes','No'], " +
					                "'esm_expiration_threashold': 60, " +
					                "'esm_trigger': 'Mode of Transportation' }}]";
							esm.putExtra(ESM.EXTRA_ESM,esmStr);
							if(Plugin.screenIsOn)
								plugin.sendBroadcast(esm);
						}
					}
				}	
				if(mot != null && ! mot.isClosed() ) mot.close();
			}

		}
	}
}
