package com.aware.plugin.sos_mobile_sensor.observers;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.util.Log;

import com.aware.Aware;
import com.aware.ESM;
import com.aware.plugin.sos_mobile_sensor.Plugin;
import com.aware.plugin.sos_mobile_sensor.MobileSensor_Provider.MobileSensor_Data;
import com.aware.plugin.sos_mobile_sensor.activities.Settings;
import com.aware.providers.Communication_Provider.Calls_Data;

/**
* A ContentObserver that will detect when the user installs/removes/updates apps
*/
public class VoiceCallObserver extends ContentObserver {
	
	private Plugin plugin;
	public static long lastVoiceESM;
	
	public VoiceCallObserver(Handler handler, Plugin plugin) {
		super(handler);
		this.plugin = plugin;
	}
	
	/**
	 * 
	 * @param lastVoiceESM
	 */
	public static long getLastVoiceESM(){
		return lastVoiceESM;
	}
	
	/**
	 * 
	 * @param lastVoiceESM
	 */
	public void setLastVoiceESM(long lastVoiceESM){
		VoiceCallObserver.lastVoiceESM = lastVoiceESM;
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
				//Share context
				Plugin.voice_messaging = 1;
				plugin.CONTEXT_PRODUCER.onContext();
				Plugin.voice_messaging = 0;
				String selection = "voice_messaging = ? OR voice_messaging = ?";
				String[] selectionArgs = new String[2];
				selectionArgs[0] = "1";
				selectionArgs[1] = "2";
				Cursor voiceData = plugin.getContentResolver().query(MobileSensor_Data.CONTENT_URI, null, selection, selectionArgs, null);
				if(currentTime-lastVoiceESM >= Plugin.throttle || voiceData.getCount() <= 1){
					Calendar morning = new GregorianCalendar();
					morning.set(Calendar.HOUR_OF_DAY, Plugin.MORNING);
					morning.set(Calendar.MINUTE, 0);
					morning.set(Calendar.SECOND, 0);
					Calendar evening = new GregorianCalendar();
					evening.set(Calendar.HOUR_OF_DAY, Plugin.EVENING);
					evening.set(Calendar.MINUTE, 0);
					evening.set(Calendar.SECOND, 0);
					SimpleDateFormat sdf = new SimpleDateFormat("k:mm:ss");
//					Log.d("Time","Morning: "+sdf.format(new Date(morning.getTimeInMillis())));
//					Log.d("Time","Evening: "+sdf.format(new Date(evening.getTimeInMillis())));
					if(currentTime > morning.getTimeInMillis() && currentTime < evening.getTimeInMillis()){
						lastVoiceESM = System.currentTimeMillis();
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
				                "'esm_expiration_threashold': 60, " +
				                "'esm_trigger': 'Calls' }}]";
						esm.putExtra(ESM.EXTRA_ESM,esmStr);
						if(Plugin.screenIsOn)
							plugin.sendBroadcast(esm);
					}
				} if(voiceData != null && ! voiceData.isClosed() ) voiceData.close(); 
			} if(calls != null && ! calls.isClosed() ) calls.close();
		}
	}
}
