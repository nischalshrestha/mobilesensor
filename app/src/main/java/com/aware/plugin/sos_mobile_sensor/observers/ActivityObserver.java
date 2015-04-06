package com.aware.plugin.sos_mobile_sensor.observers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import com.aware.ESM;
import com.aware.plugin.google.activity_recognition.Algorithm;
import com.aware.plugin.sos_mobile_sensor.MobileSensor_Provider.MobileSensor_Data;
import com.aware.plugin.sos_mobile_sensor.Plugin;
import com.aware.plugin.sos_mobile_sensor.activities.Settings;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class ActivityObserver extends BroadcastReceiver {

	public static String ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION = "ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION";
	public static String EXTRA_ACTIVITY = "activity";
	public static String EXTRA_CONFIDENCE = "confidence";
	
	private Plugin plugin;
	private String lastactivity;
	public static long lastActivityESM;

	public ActivityObserver(Plugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if(Plugin.screenIsOn && Settings.initialized){
			if(intent.getAction().equalsIgnoreCase(ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION))  {
//				Log.d("Activity","Activity: "+Algorithm.getActivityName(intent.getIntExtra(EXTRA_ACTIVITY, -1))
//						+ " Confidence: "+intent.getIntExtra(EXTRA_CONFIDENCE,-1));
				String act = Algorithm.getActivityName(intent.getIntExtra(EXTRA_ACTIVITY, -1));
				if(lastactivity == null || !lastactivity.equals(act)){
					long currentTime = System.currentTimeMillis();
					lastactivity = act;
					Plugin.movement = lastactivity;
					Plugin.movementConfidence = intent.getIntExtra(EXTRA_CONFIDENCE,-1);
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
					String mSelection = "mode_of_transportation != ?";
					String[] mSelectionArgs = new String[1];
					mSelectionArgs[0] = "";
					Cursor motData = plugin.getContentResolver().query(MobileSensor_Data.CONTENT_URI, null, mSelection, mSelectionArgs, null);
					if(currentTime-lastActivityESM >= Plugin.throttle 	//&&
//					   currentTime > morning.getTimeInMillis()   		&&
//					   currentTime < evening.getTimeInMillis()
                            ||
					   motData.getCount() < 1							//&&
//					   Calendar.DAY_OF_WEEK > 1                  		&&
//					   Calendar.DAY_OF_WEEK < 7 //don't bother on weekends
					   ) {
	                    if(!Plugin.stressInit){
	                      Plugin.stressInit = true;
	                      Plugin.initStressorTime = currentTime;
	                      Plugin.initStressor = "Mode of Transportation";
	                      Plugin.stressCount++;
	                      Intent esm = new Intent();
      //					if( Aware.DEBUG )
      //						Log.d("Mode of Transportation","Prompting the Messaging ESM");
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
	                    } else if(currentTime-Plugin.initStressorTime < 60000    		&& 
	                    		  Plugin.stressInit                                 	&& 
	                    	     !Plugin.initStressor.equals("Mode of Transportation")
	                    	      ){
	                    	Plugin.stressCount++;
	                    }
						//Share context
                        plugin.CONTEXT_PRODUCER.onContext();
						lastActivityESM = currentTime;
					} else{
                        plugin.CONTEXT_PRODUCER.onContext();
                    }
				}
			}
		}
	}

}
