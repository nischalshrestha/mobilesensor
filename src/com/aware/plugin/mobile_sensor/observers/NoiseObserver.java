package com.aware.plugin.mobile_sensor.observers;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.util.Log;

import com.aware.Aware;
import com.aware.ESM;
import com.aware.plugin.mobile_sensor.Plugin;
import com.aware.plugin.noise_level.NoiseLevel_Provider.NoiseLevel;

/**
* A ContentObserver that will detect changes in the Applications_Foreground background for multitasking
* behavior 
* i.e. Users switches btw 3 or more apps within a 5 minute interval
*/
public class NoiseObserver extends ContentObserver {
	
	/** Relevant variables */
	private Plugin plugin;	
	/** Noise variables */
	public static long lastNoisyESM;
	private static double threshold;
	public static SimpleRegression s;
	
	/**
	 * 
	 * @param handler
	 * @param plugin
	 */
	public NoiseObserver(Handler handler, Plugin plugin) {
		super(handler);
		this.plugin = plugin;
		s = null;
//		Log.d("Noise Level","Plugin started");
	}
	
	/**
	 * 
	 */
	@Override
	public void onChange(boolean selfChange) {
		super.onChange(selfChange);
//		Log.d("Noise Level","Function: "+s);
		if(s != null && Plugin.screenIsOn){
			//Figure out if the user is in a noisy environment
			Cursor noises = plugin.getContentResolver().query(NoiseLevel.CONTENT_URI, null, null, null, NoiseLevel.TIMESTAMP + " DESC LIMIT 1");
			Long currentTime = System.currentTimeMillis();
            double noise = 0;
			if( noises != null && noises.moveToFirst()) {
                double sample = noises.getDouble(noises.getColumnIndex(NoiseLevel.NOISE));
                noise = s.predict(sample);
//                Log.d("Noise Level","sample: "+sample);
//				Log.d("Noise Level","X: "+sample+" Y: "+noise);
				//If the user is in a noisy environment prompt the ESM
				if(currentTime - lastNoisyESM >= Plugin.throttle && noise >= 70){
					Plugin.noise_level = noise;
					plugin.CONTEXT_PRODUCER.onContext();
					Calendar morning = new GregorianCalendar();
					morning.set(Calendar.HOUR_OF_DAY, Plugin.MORNING);
					morning.set(Calendar.MINUTE, 0);
					morning.set(Calendar.SECOND, 0);
					Calendar evening = new GregorianCalendar();
					evening.set(Calendar.HOUR_OF_DAY, Plugin.EVENING);
					evening.set(Calendar.MINUTE, 0);
					evening.set(Calendar.SECOND, 0);
					if(currentTime > morning.getTimeInMillis() &&
						currentTime < evening.getTimeInMillis()){
						lastNoisyESM = currentTime;
						Intent esm = new Intent();
//						if( Aware.DEBUG )
//							Log.d("Noise Level","Prompting the noise level ESM");
					    esm.setAction(ESM.ACTION_AWARE_QUEUE_ESM);
						String esmStr = "[" +
				                "{'esm': {" +
				                "'esm_type': 5, " +
				                "'esm_title': 'There is noise in the environment', " +
				                "'esm_instructions': 'Is this true?', " +
				                "'esm_quick_answers':  ['Yes','No'], " +
				                "'esm_expiration_threashold': 60, " +
				                "'esm_trigger': 'Noise Level' }},"
				                + "]";
						esm.putExtra(ESM.EXTRA_ESM,esmStr);
						if(Plugin.screenIsOn)
							plugin.sendBroadcast(esm);
					} if(noises != null && ! noises.isClosed() ) noises.close();
				}
			}
		}
	}
}


