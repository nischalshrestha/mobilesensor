package com.aware.plugin.sos_mobile_sensor.observers;

import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;

import com.aware.ESM;
import com.aware.plugin.sos_mobile_sensor.MobileSensor_Provider.MobileSensor_Data;
import com.aware.plugin.sos_mobile_sensor.Plugin;
import com.aware.providers.Communication_Provider.Messages_Data;

import java.util.Calendar;
import java.util.GregorianCalendar;

//import android.util.Log;

/**
 * A ContentObserver that will detect when the user is texting
 * @author Nischal Shrestha
 */
public class MessageObserver extends ContentObserver {
	
	private Plugin plugin;
	public static long lastMessageESM;
	private static String action = "";
	
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
			Cursor messages = plugin.getContentResolver().query(Messages_Data.CONTENT_URI, null, null, null, Messages_Data.TIMESTAMP + " DESC LIMIT 1");
			if( messages!= null && messages.moveToFirst()) {
				long currentTime = System.currentTimeMillis();
				Calendar morning = new GregorianCalendar();
				morning.set(Calendar.HOUR_OF_DAY, Plugin.MORNING);
				morning.set(Calendar.MINUTE, 0);
				morning.set(Calendar.SECOND, 0);
				Calendar evening = new GregorianCalendar();
				evening.set(Calendar.HOUR_OF_DAY, Plugin.EVENING);
				evening.set(Calendar.MINUTE, 0);
				evening.set(Calendar.SECOND, 0);
				//Share context
//				if( Aware.DEBUG )
//					Log.d("Messaging","Status: "+messages.getString(messages.getColumnIndex("message_type")));
				String prevSent = action;
				String mtype = messages.getString(messages.getColumnIndex(Messages_Data.TYPE));
				if(mtype.equals("1")){
					action = "received";
				} else if(mtype.equals("2")){
					action = "sent";
				}
				String mSelection = "text_messaging == ?";
				String[] mSelectionArgs = new String[1];
				mSelectionArgs[0] = "1";
				Cursor textData = plugin.getContentResolver().query(MobileSensor_Data.CONTENT_URI, null, mSelection, mSelectionArgs, null);
//				Log.d("Text", "Count: " + textData.getCount());
				if(currentTime-lastMessageESM >= Plugin.throttle    &&
				   currentTime > morning.getTimeInMillis()          &&
				   currentTime < evening.getTimeInMillis()
//                        &&
//				   Calendar.DAY_OF_WEEK > 1                  	    &&
//				   Calendar.DAY_OF_WEEK < 7
                        ||
                   textData.getCount() < 1//don't bother on weekends
				   ) {
					if(!Plugin.stressInit){
				        Plugin.stressInit = true;
				        Plugin.initStressorTime = currentTime;
				        Plugin.initStressor = "Text Messaging";
				        Plugin.stressCount++;
//				        if( Aware.DEBUG )
//	                        Log.d("Messaging","Prompting the Messaging ESM");
				        String esmStr = "[" +
	                            "{'esm': {" +
	                            "'esm_type': 5, " +
	                            "'esm_title': 'You were just text messaging', " +
	                            "'esm_instructions': 'Is this true?', " +
	                            "'esm_quick_answers':  ['Yes','No'], " +
	                            "'esm_expiration_threashold': 180, " +
	                            "'esm_trigger': 'Text Messaging' }}]";
	                    Intent esm = new Intent().setAction("ESM.ACTION_AWARE_QUEUE_ESM").putExtra(ESM.EXTRA_ESM,esmStr);
	                    if(Plugin.screenIsOn)
	                        plugin.sendBroadcast(esm);
                        lastMessageESM = System.currentTimeMillis();
	                }
	                if(Plugin.stressInit 
	                		&& currentTime-Plugin.initStressorTime < 60000 
	                		&& !Plugin.initStressor.equals("Text Messaging")){
	                    Plugin.stressCount++;
	                }
                    Plugin.text_messaging = 1;
                    plugin.CONTEXT_PRODUCER.onContext();
                    Plugin.text_messaging = 0;
                } else if(prevSent != null && !prevSent.equals(action)){
                    Plugin.text_messaging = 1;
                    plugin.CONTEXT_PRODUCER.onContext();
                    Plugin.text_messaging = 0;
                }
                if(textData != null && !textData.isClosed() ) textData.close();
			} if(messages != null && !messages.isClosed() ) messages.close();
		}
	}
}
