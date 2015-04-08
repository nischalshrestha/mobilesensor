package com.aware.plugin.sos_mobile_sensor.observers;

import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;

import com.aware.ESM;
import com.aware.plugin.sos_mobile_sensor.MobileSensor_Provider.MobileSensor_Data;
import com.aware.plugin.sos_mobile_sensor.Plugin;
import com.aware.providers.Communication_Provider.Calls_Data;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
* A ContentObserver that will detect when the user makes a call
*/
public class VoiceCallObserver extends ContentObserver {
	
	private Plugin plugin;
	public static long lastVoiceESM;
	
	public VoiceCallObserver(Handler handler, Plugin plugin) {
		super(handler);
		this.plugin = plugin;
	}
	
	@Override
	public void onChange(boolean selfChange) {
		super.onChange(selfChange);
		if(Plugin.screenIsOn){
//			if( Aware.DEBUG )
//				Log.d("Phone","User is on the phone");
			Cursor calls = plugin.getContentResolver().query(Calls_Data.CONTENT_URI, null, null, null, Calls_Data.TIMESTAMP + " DESC LIMIT 1");
			if( calls!= null && calls.moveToFirst()) {
				long currentTime = System.currentTimeMillis();
				Calendar morning = new GregorianCalendar();
				morning.set(Calendar.HOUR_OF_DAY, Plugin.MORNING);
				morning.set(Calendar.MINUTE, 0);
				morning.set(Calendar.SECOND, 0);
				Calendar evening = new GregorianCalendar();
				evening.set(Calendar.HOUR_OF_DAY, Plugin.EVENING);
				evening.set(Calendar.MINUTE, 0);
				evening.set(Calendar.SECOND, 0);
				//Log firing, and/or prompt ESM
				String mSelection = "voice_messaging == ?";
				String[] mSelectionArgs = new String[1];
				mSelectionArgs[0] = "1";
				Cursor callData = plugin.getContentResolver().query(MobileSensor_Data.CONTENT_URI, null, mSelection, mSelectionArgs, null);
				if(currentTime-lastVoiceESM >= Plugin.throttle       &&
				   currentTime > morning.getTimeInMillis()           &&
				   currentTime < evening.getTimeInMillis()           &&
			       Calendar.DAY_OF_WEEK > 1                  	     &&//don't bother on weekends
			       Calendar.DAY_OF_WEEK < 7                          ||
                   callData.getCount() < 1
			       ) {
					if(!Plugin.stressInit){
	                    Plugin.stressInit = true;
	                    Plugin.initStressorTime = currentTime;
	                    Plugin.initStressor = "Calls";
	                    Plugin.stressCount++;
	                    Intent esm = new Intent();
//						if( Aware.DEBUG )
//							Log.d("Phone","Prompting the phone ESM");
	                    esm.setAction(ESM.ACTION_AWARE_QUEUE_ESM);
	                    String esmStr = "[" +
	                            "{'esm': {" +
	                            "'esm_type': 5, " +
	                            "'esm_title': 'You were just on the phone', " +
	                            "'esm_instructions': 'Is this true?', " +
	                            "'esm_quick_answers':  ['Yes','No'], " +
	                            "'esm_expiration_threashold': 180, " +
	                            "'esm_trigger': 'Calls' }}]";
	                    esm.putExtra(ESM.EXTRA_ESM, esmStr);
	                    if (Plugin.screenIsOn)
	                        plugin.sendBroadcast(esm);
                        lastVoiceESM = System.currentTimeMillis();
		            } else if(Plugin.stressInit 
		            		&& currentTime-Plugin.initStressorTime < 60000 
		            		&& !Plugin.initStressor.equals("Calls")){
		                Plugin.stressCount++;
		            }
                    //Share context
                    Plugin.voice_messaging = 1;
                    plugin.CONTEXT_PRODUCER.onContext();
                    Plugin.voice_messaging = 0;
                } else{
                    Plugin.voice_messaging = 1;
                    plugin.CONTEXT_PRODUCER.onContext();
                    Plugin.voice_messaging = 0;
                }
                if(callData != null && !callData.isClosed() ) callData.close();
			} if(calls != null && ! calls.isClosed() ) calls.close();
		}
	}
}
