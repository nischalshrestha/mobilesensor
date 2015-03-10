package com.aware.plugin.sos_mobile_sensor.observers;
//package com.aware.plugin.sos_mobile_sensor.observers;
//
//import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.GregorianCalendar;
//
//import android.content.Intent;
//import android.database.ContentObserver;
//import android.database.Cursor;
//import android.os.Handler;
//import android.util.Log;
//
//import com.aware.Aware;
//import com.aware.ESM;
//import com.aware.plugin.ambient_noise.Provider.AmbientNoise_Data;
//import com.aware.plugin.sos_mobile_sensor.Plugin;
//import com.aware.plugin.sos_mobile_sensor.MobileSensor_Provider.MobileSensor_Data;
//import com.aware.providers.Communication_Provider.Messages_Data;
//
//public class AmbientNoiseObserver extends ContentObserver {
//
//	private Plugin plugin;
//	public long lastAmbientESM;
//	private long timeWindow = 15000; //default
//	
//	public AmbientNoiseObserver(Handler handler, Plugin plugin) {
//		super(handler);
//		this.plugin = plugin;
//		timeWindow = Plugin.throttle;
//	}
//	
//	/**
//	 * Sets the last time the ESM was prompted
//	 * @param lastAmbientESM
//	 */
//	public void setLastAmbientESM(long lastAmbientESM){
//		this.lastAmbientESM = lastAmbientESM;
//	}
//	
//	public void onChange(boolean selfChange){
//		super.onChange(selfChange);
//		if(plugin.getScreenStatus()){
//			if( Aware.DEBUG )
//				Log.d("Ambient Noise","The environment is noisy!");
//			Cursor noise = plugin.getContentResolver().query(AmbientNoise_Data.CONTENT_URI, null, null, null, AmbientNoise_Data.TIMESTAMP + " DESC LIMIT 1");
//			if( noise!= null && noise.moveToFirst()) {
//				long timestamp = noise.getLong(noise.getColumnIndex(AmbientNoise_Data.TIMESTAMP));
//				//Morning
//				Calendar newCal = new GregorianCalendar();
//				newCal.set(Calendar.HOUR_OF_DAY, Plugin.MORNING);
//				newCal.set(Calendar.MINUTE, 0);
//				newCal.set(Calendar.SECOND, 0);
//				//Evening
//				Calendar newCal2 = new GregorianCalendar();
//				newCal2.set(Calendar.HOUR_OF_DAY, Plugin.EVENING);
//				newCal2.set(Calendar.MINUTE, 0);
//				newCal2.set(Calendar.SECOND, 0);
//				String selection = "ambient_noise = ? OR ambient_noise = ?";
//				String[] selectionArgs = new String[2];
//				selectionArgs[0] = "0";
//				selectionArgs[1] = "1";
//				Cursor noiseData = plugin.getContentResolver().query(MobileSensor_Data.CONTENT_URI, null, selection, selectionArgs, null);
//				//only prompt if it has been an hour since the last one,
//				//and if it's btw 8am-8pm
//				if(timestamp - lastAmbientESM >= timeWindow || noiseData.getCount() == 0){
//					plugin.setAmbient_noise(1);
//					//Share context
//					plugin.CONTEXT_PRODUCER.onContext();
//					plugin.setAmbient_noise(0);
//					String isSilent = noise.getString(noise.getColumnIndex(AmbientNoise_Data.IS_SILENT));
//					String state = "";
//					if(isSilent.equals("0")){
//						state = "silent";
//					} else if(isSilent.equals("1")){
//						state = "loud";
//					}
//					if(state.equals("loud") && Plugin.participantID != null && timestamp > newCal.getTimeInMillis() && timestamp < newCal2.getTimeInMillis()){
//						lastAmbientESM = System.currentTimeMillis();
//						Intent esm = new Intent();
//						if( Aware.DEBUG )
//							Log.d("Ambient Noise","Prompting the Ambient Noise ESM");
//					    esm.setAction(ESM.ACTION_AWARE_QUEUE_ESM);
//						String esmStr = "[" +
//				                "{'esm': {" +
//				                "'esm_type': 5, " +
//				                "'esm_title': 'Your environment is "+state+"', " +
//				                "'esm_instructions': 'Is this true?', " +
//				                "'esm_quick_answers':  ['Yes','No'], " +
//				                "'esm_expiration_threashold': 60, " +
//				                "'esm_trigger': 'Ambient Noise' }}]";
//						esm.putExtra(ESM.EXTRA_ESM,esmStr);
//						if(plugin.getScreenStatus())
//							plugin.sendBroadcast(esm);
//					}
//				}
//			}if(noise != null && ! noise.isClosed() ) noise.close();
//		} 
//	}
//
//}
