package com.aware.plugin.sos_mobile_sensor.observers;

import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;

import com.aware.ESM;
import com.aware.plugin.ambient_noise.Provider.AmbientNoise_Data;
import com.aware.plugin.sos_mobile_sensor.MobileSensor_Provider.MobileSensor_Data;
import com.aware.plugin.sos_mobile_sensor.Plugin;
import com.aware.plugin.sos_mobile_sensor.activities.Settings;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class AmbientNoiseObserver extends ContentObserver {

	private Plugin plugin;
	public static long lastAmbientESM;
	
	public AmbientNoiseObserver(Handler handler, Plugin plugin) {
		super(handler);
		this.plugin = plugin;
	}
	
	public void onChange(boolean selfChange){
		super.onChange(selfChange);
		if(Plugin.screenIsOn && Settings.initialized){
//			if( Aware.DEBUG )
//				Log.d("Ambient Noise","The environment is noisy!");
			Cursor noise = plugin.getContentResolver().query(AmbientNoise_Data.CONTENT_URI, null, null, null, AmbientNoise_Data.TIMESTAMP + " DESC LIMIT 1");
			if( noise!= null && noise.moveToFirst()) {
				long currentTime = System.currentTimeMillis();
				double db = noise.getDouble(noise.getColumnIndex(AmbientNoise_Data.DECIBELS));
				//Morning
				Calendar newCal = new GregorianCalendar();
				newCal.set(Calendar.HOUR_OF_DAY, Plugin.MORNING);
				newCal.set(Calendar.MINUTE, 0);
				newCal.set(Calendar.SECOND, 0);
				//Evening
				Calendar newCal2 = new GregorianCalendar();
				newCal2.set(Calendar.HOUR_OF_DAY, Plugin.EVENING);
				newCal2.set(Calendar.MINUTE, 0);
				newCal2.set(Calendar.SECOND, 0);
				String isSilent = noise.getString(noise.getColumnIndex(AmbientNoise_Data.IS_SILENT));
				String state = "";
				if(isSilent.equals("0")){
					state = "noisy";
				} else if(isSilent.equals("1")){
					state = "silent";
				}
				String mSelection = "ambient_noise != ?";
				String[] mSelectionArgs = new String[1];
				mSelectionArgs[0] = "";
				Cursor noiseData = plugin.getContentResolver().query(MobileSensor_Data.CONTENT_URI, null, mSelection, mSelectionArgs, null);
				//Fire sensor and/or prompt ESM
//                Log.d("Stress", "Time remaining in Ambient Noise: " + (currentTime - lastAmbientESM));
				if(currentTime - lastAmbientESM >= Plugin.throttle &&
				   currentTime > newCal.getTimeInMillis()          &&
				   currentTime < newCal2.getTimeInMillis()         &&
				   Calendar.DAY_OF_WEEK > 1                        && //don't bother on weekends
				   Calendar.DAY_OF_WEEK < 7                        ||
                   noiseData.getCount() < 1
				   ) {
					if(!Plugin.stressInit){
	                    Plugin.stressInit = true;
	                    Plugin.initStressorTime = currentTime;
	                    Plugin.initStressor = "Ambient Noise";
	                    Plugin.stressCount++;
	                    Intent esm = new Intent();
//						if( Aware.DEBUG )
//							Log.d("Ambient Noise","Prompting the Ambient Noise ESM");
	                    esm.setAction(ESM.ACTION_AWARE_QUEUE_ESM);
	                    String esmStr = "[" +
	                            "{'esm': {" +
	                            "'esm_type': 5, " +
	                            "'esm_title': 'Your environment is noisy', " +
	                            "'esm_instructions': 'Is this true?', " +
	                            "'esm_quick_answers':  ['Yes','No'], " +
	                            "'esm_expiration_threashold': 180, " +
	                            "'esm_trigger': 'Ambient Noise' }}]";
	                    esm.putExtra(ESM.EXTRA_ESM, esmStr);
	                    if (Plugin.screenIsOn)
	                        plugin.sendBroadcast(esm);
                        lastAmbientESM = System.currentTimeMillis();
	                } else if(Plugin.stressInit 
	                		&& currentTime-Plugin.initStressorTime < 60000 
	                		&& !Plugin.initStressor.equals("Ambient Noise")){
	                    Plugin.stressCount++;;
	                }
                    //Share context
                    Plugin.noise_level = db;
                    Plugin.ambient_noise = state;
                    plugin.CONTEXT_PRODUCER.onContext();
                } else{
                    Plugin.noise_level = db;
                    Plugin.ambient_noise = state;
                    plugin.CONTEXT_PRODUCER.onContext();
                }
                if(noiseData != null && !noiseData.isClosed()){ noiseData.close();}
			}
            if(noise != null && !noise.isClosed()){ noise.close();}
		} 
	}

}
