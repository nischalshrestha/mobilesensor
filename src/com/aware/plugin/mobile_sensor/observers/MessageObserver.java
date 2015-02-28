package com.aware.plugin.mobile_sensor.observers;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.util.Log;

import com.aware.Aware;
import com.aware.ESM;
import com.aware.plugin.mobile_sensor.MobileSensor_Provider.MobileSensor_Data;
import com.aware.plugin.mobile_sensor.Plugin;
import com.aware.providers.Communication_Provider.Messages_Data;

/**
 * A ContentObserver that will detect when the user is texting
 * @author Nischal Shrestha
 */
public class MessageObserver extends ContentObserver {
	
	private Plugin plugin;
	public static long lastMessageESM;
	
	/**
	 * Initiate MessageObserver ContentObserver with the handler, and plugin for context
	 * @param handler
	 * @param plugin
	 */
	public MessageObserver(Handler handler, Plugin plugin) {
		super(handler);
		this.plugin = plugin;
	}
	
	@Override
	public void onChange(boolean selfChange) {
		super.onChange(selfChange);
		if(Plugin.screenIsOn){
//			if( Aware.DEBUG )
//				Log.d("Messaging","User is either sending or receiving messages");
			Cursor messages = plugin.getContentResolver().query(Messages_Data.CONTENT_URI, null, null, null, Messages_Data.TIMESTAMP + " DESC LIMIT 1");
			if( messages!= null && messages.moveToFirst()) {
				//Share context
				Plugin.text_messaging = 1;
				plugin.CONTEXT_PRODUCER.onContext();
				Plugin.text_messaging = 0;
				String mtype = messages.getString(messages.getColumnIndex(Messages_Data.TYPE));
				String action = "";
				if(mtype.equals("1")){
					action = "received";
				} else if(mtype.equals("2")){
					action = "sent";
				}
				long currentTime = System.currentTimeMillis();
				Cursor textData = plugin.getContentResolver().query(MobileSensor_Data.CONTENT_URI, null, null, null, null);
				if(currentTime-lastMessageESM >= Plugin.throttle || textData.getCount() <= 1){
					Calendar morning = new GregorianCalendar();
					morning.set(Calendar.HOUR_OF_DAY, Plugin.MORNING);
					morning.set(Calendar.MINUTE, 0);
					morning.set(Calendar.SECOND, 0);
					Calendar evening = new GregorianCalendar();
					evening.set(Calendar.HOUR_OF_DAY, Plugin.EVENING);
					evening.set(Calendar.MINUTE, 0);
					evening.set(Calendar.SECOND, 0);
					if(currentTime > morning.getTimeInMillis() && currentTime < evening.getTimeInMillis()){
						lastMessageESM = System.currentTimeMillis();
						Intent esm = new Intent();
//						if( Aware.DEBUG )
//							Log.d("Messaging","Prompting the Messaging ESM");
					    esm.setAction(ESM.ACTION_AWARE_QUEUE_ESM);
						String esmStr = "[" +
				                "{'esm': {" +
				                "'esm_type': 5, " +
				                "'esm_title': 'You have just "+action+" a text message', " +
				                "'esm_instructions': 'Is this true?', " +
				                "'esm_quick_answers':  ['Yes','No'], " +
				                "'esm_expiration_threashold': 60, " +
				                "'esm_trigger': 'Messages' }}]";
						esm.putExtra(ESM.EXTRA_ESM,esmStr);
						if(Plugin.screenIsOn)
							plugin.sendBroadcast(esm);
					}
				} if(textData != null && ! textData.isClosed() ) textData.close();
			} if(messages != null && ! messages.isClosed() ) messages.close();
		}
	}
}
