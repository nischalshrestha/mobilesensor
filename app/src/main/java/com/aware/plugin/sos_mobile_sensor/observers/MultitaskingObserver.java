package com.aware.plugin.sos_mobile_sensor.observers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.aware.Aware;
import com.aware.ESM;
import com.aware.plugin.sos_mobile_sensor.Plugin;
import com.aware.plugin.sos_mobile_sensor.MobileSensor_Provider.MobileSensor_Data;
import com.aware.providers.Applications_Provider.Applications_Foreground;

/**
 * A ContentObserver that will detect changes in the Applications_Foreground background for 
 * multitasking behavior 
 * Multitasking: If the user uses a switcher app (notifications/task switcher/launcher) and they
 * switch from the previous app to a new one, count that as a switch. If there are x or more switches
 * (MAX_SWITCHES) the user is multitasking.
 * @author Nischal Shrestha
 *
 */
public class MultitaskingObserver extends ContentObserver {
	
	/** Our plugin */
	private Plugin plugin;
	//Last instance of multitasking
	private static long lastMultitasking;
	//Last instance of prompting for false positives
	public static long lastMultiESM;
	public static long lastEmailESM;
	//Different from throttle; window for false positives
	public final static long timeWindow = 60000; 
	private String newApp;
	private int switches = 0;
    private static String appA = "";
    private static String appB = "";
    private static String appC = "";
    //Excluded apps
    private static Intent determineHome; 
    public static String home = "";
    private final static List<String> excludedApps = new ArrayList<String>();
    private final static ArrayList<String> chatApps = new ArrayList<String>();
    private final static ArrayList<String> switchers = new ArrayList<String>();
    private final List<String> emailApps = new ArrayList<String>();
    private final static String switcher = "System UI";
    private static String chatAppDetected = "";
    //Threshold
    public final static int MAX_SWITCHES = 2;
	
    /**
     * Construct for the Multitasking ContentObserver with the handler, and plugin for context
     * Adds chat apps to the list of chatApps for text messaging sensor
     * Adds switchers to the list of switchers for switcher apps
     * Determines Launcher to exclude from being sensed
     * Determines Email apps to exclude from being sensed
     * @param handler
     * @param plugin
     */
	@SuppressLint("NewApi")
	public MultitaskingObserver(Handler handler, Plugin plugin) {
		super(handler);
		this.plugin = plugin;
		//Add popular chat apps
	    chatApps.add("WhatsApp");
	    chatApps.add("Viber");
	    chatApps.add("Messenger");
		chatApps.add("Skype");
		chatApps.add("WeChat");
		//Add task switcher apps
		switchers.add("System UI");
		switchers.add("Switchr");
		switchers.add("Looper");
		switchers.add("Glovebox");
		switchers.add("Task Switcher");
		switchers.add("Fancy Switcher");
		getHomeApp();
		getEmailApps();
		//Exclude these apps (switcher here is the notifications app)
        excludedApps.add(home);
        excludedApps.add("Mobile Sensor");
        excludedApps.add("AWARE");
        excludedApps.add(switcher);
        excludedApps.add("Android System");
	}
	
	/**
	 * Grabs the default Home app(s) that the user uses
	 */
	public void getHomeApp(){
		determineHome = new Intent(); 
        determineHome.setAction(Intent.ACTION_MAIN); 
        determineHome.addCategory(Intent.CATEGORY_HOME); 
        PackageManager pm = plugin.getPackageManager(); 
        ResolveInfo ri = pm.resolveActivity(determineHome, 0);
        ApplicationInfo ai = null;
        try {
			ai = pm.getApplicationInfo(ri.activityInfo.packageName, PackageManager.GET_ACTIVITIES);
		} catch (NameNotFoundException e) {
			ai = null;
		}
        home = ( ai != null ) ? (String) pm.getApplicationLabel(ai):"";
//        Log.d("Multitasking","Launcher: "+home);
	}
	
	/**
	 * Grabs the default email app(s) that the user uses
	 */
	public void getEmailApps(){
		Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto","", null)); 
        PackageManager pm = plugin.getPackageManager();
        List<ResolveInfo> intents = pm.queryIntentActivities(emailIntent, PackageManager.MATCH_DEFAULT_ONLY);
        for(ResolveInfo r : intents){
        	String emailName = (String)pm.getApplicationLabel(r.activityInfo.applicationInfo);
//    	    Log.d("Email","Name: "+emailName);
        	if(!emailApps.contains(emailName)){
        		emailApps.add(emailName);
        	}
        }
	}
	
	/**
	 * Sets the time for the last multitasking false positive
	 * @param lastMultiESM
	 */
	public void setLastMultiESM(long lastMultiESM){
		MultitaskingObserver.lastMultiESM = lastMultiESM;
		this.lastMultitasking = lastMultiESM;
	}
	
	/**
	 * Return last time when user was prompted for an email false positive
	 * @param lastEmailESM
	 */
	public static long getLastEmailESM(){
		return lastEmailESM;
	}
	
	/**
	 * Sets the time for the last email false positive
	 * @param lastEmailESM
	 */
	public void setLastEmailESM(long lastEmailESM){
		MultitaskingObserver.lastEmailESM = lastEmailESM;
	}
	
	/**
	 * This observes changes in the Applications sensor's database to determine if user is multitasking
	 * Definition: User is doing two or more switches btw 2 or more apps within 60 seconds: ABA, ABC
	 * @param selfChange
	 */
	@SuppressLint({ "NewApi", "ServiceCast" })
	public void onChange(boolean selfChange) {
		super.onChange(selfChange);
		if(Plugin.screenIsOn){
//			Log.d("Thread","Multitasking: Is this the main thread?"+(Looper.myLooper() == Looper.getMainLooper()));
	        //Use Applications sensor to query changes in foreground
			Cursor multitask = plugin.getContentResolver().query(Applications_Foreground.CONTENT_URI, null, null, null, Applications_Foreground.TIMESTAMP + " DESC");
			Long currentTime = System.currentTimeMillis();
			Calendar morning = new GregorianCalendar();
			morning.set(Calendar.HOUR_OF_DAY, Plugin.MORNING);
			morning.set(Calendar.MINUTE, 0);
			morning.set(Calendar.SECOND, 0);
			Calendar evening = new GregorianCalendar();
			evening.set(Calendar.HOUR_OF_DAY, Plugin.EVENING);
			evening.set(Calendar.MINUTE, 0);
			evening.set(Calendar.SECOND, 0);
			if( multitask != null && multitask.moveToFirst()) {
				newApp = multitask.getString(multitask.getColumnIndex(Applications_Foreground.APPLICATION_NAME));
//				Log.d("Multitasking","User is using an app: "+newApp);
				Long timestamp = multitask.getLong(multitask.getColumnIndex(Applications_Foreground.TIMESTAMP));
	            Long result = timestamp - lastMultitasking;
	            if(result > 0 && result < timeWindow){ //only look at apps used within 1 minute
	            	//Text messaging; popular chat apps
					if(!newApp.equals(chatAppDetected)){
						for(String s : chatApps){
							if(newApp.equals(s)){
								chatAppDetected = newApp;
								Plugin.text_messaging = 1;
								plugin.CONTEXT_PRODUCER.onContext();
								Plugin.text_messaging = 0;
								Cursor textData = plugin.getContentResolver().query(MobileSensor_Data.CONTENT_URI, null, null, null, null);
								//send an esm intent
								if(currentTime-MessageObserver.lastMessageESM >= Plugin.throttle 
										&& currentTime > morning.getTimeInMillis() 
										&& currentTime < evening.getTimeInMillis() || textData.getCount() <= 1){
									Intent esm = new Intent();
//									if( Aware.DEBUG )
//										Log.d("Messaging","Prompting the Messaging ESM");
								    esm.setAction(ESM.ACTION_AWARE_QUEUE_ESM);
									String esmStr = "[" +
							                "{'esm': {" +
							                "'esm_type': 5, " +
							                "'esm_title': 'You are using a chat app', " +
							                "'esm_instructions': 'Is this true?', " +
							                "'esm_quick_answers':  ['Yes','No'], " +
							                "'esm_expiration_threashold': 60, " +
							                "'esm_trigger': 'Messages' }}]";
									esm.putExtra(ESM.EXTRA_ESM,esmStr);
									if(Plugin.screenIsOn)
										plugin.sendBroadcast(esm);
								}
							}
						}
					}
					//Email
					if(timestamp-lastEmailESM >= timeWindow){
						for(String s : emailApps){
							if(newApp.equals(s)){
								//Share context
								Plugin.email = 1;
								plugin.CONTEXT_PRODUCER.onContext();
								Plugin.email = 0;
								//send an esm intent
								if(currentTime-lastEmailESM >= Plugin.throttle 
									 && currentTime > morning.getTimeInMillis() 
								     && currentTime < evening.getTimeInMillis()){
									lastEmailESM = System.currentTimeMillis();
									Intent esm = new Intent();
//									if( Aware.DEBUG )
//										Log.d("Email","Prompting the Email ESM");
								    esm.setAction(ESM.ACTION_AWARE_QUEUE_ESM);
									String esmStr = "[" +
							                "{'esm': {" +
							                "'esm_type': 5, " +
							                "'esm_title': 'You are using an email app', " +
							                "'esm_instructions': 'Is this true?', " +
							                "'esm_quick_answers':  ['Yes','No'], " +
							                "'esm_expiration_threashold': 60, " +
							                "'esm_trigger': 'Email' }}]";
									esm.putExtra(ESM.EXTRA_ESM,esmStr);
									if(Plugin.screenIsOn)
										plugin.sendBroadcast(esm);
									}	
							}
						}
					}
	            	//set appB, the home and app switcher apps
					//set appA and appC the two apps the user is switching btw
	            	if(!excludedApps.contains(newApp)){
	            		if(appA.equals("")){ //A
							appA = newApp;
//	            			if( Aware.DEBUG )
//	        					Log.d("Multitasking","User is using an appA: "+newApp);
						} else if(appB.equals("") && !newApp.equals(appA)){ //AB
	            			appB = newApp;
//	            			if( Aware.DEBUG )
//	        					Log.d("Multitasking","User is using an appB: "+newApp);
	            		} else if(!appB.equals("") && !newApp.equals(appB)){ //ABA or ABC
	            			appC = newApp;
//	            			if( Aware.DEBUG )
//	        					Log.d("Multitasking","User is using an appC: "+newApp);
//	            			if( Aware.DEBUG )
//	        					Log.d("Multitasking","User used these apps: "+appA+", "+appB+", "+appC);
	            			switches++;
//	            			if( Aware.DEBUG )
//	        					Log.d("Multitasking","No. of switches: "+switches);
	            			//reset to start process of counting switches
	            			appA = "";
	    					appB = "";
	    					appC = "";
	            		}
		            }
	            } 
	            //This resets the switches if switches < MAX_SWITCHES and it has a minute or longer
	            //This is to prevent a multitasking false positive question if the user has been inactive
	            //with the phone on
	            if(result >= timeWindow && switches < MAX_SWITCHES){
//	            	if( Aware.DEBUG )
//    					Log.d("Multitasking","Only "+switches+" within a minute so resetting");
	            	switches = 0;
	            	lastMultitasking = System.currentTimeMillis();
	            }
				//Fire up the false positive ESM when conditions are met 
				if((currentTime-lastMultitasking) >= timeWindow && switches >= MAX_SWITCHES){
					//Share context
					Plugin.multitasking = 1;
					plugin.CONTEXT_PRODUCER.onContext();
					Plugin.multitasking = 0;
					lastMultitasking = System.currentTimeMillis();
					String selection = "multitasking = ?";
					String[] selectionArgs = {""};
					selectionArgs[0] = "1";
					Cursor multiData = plugin.getContentResolver().query(MobileSensor_Data.CONTENT_URI, null, selection, selectionArgs, null);
					if(currentTime-lastMultiESM >= Plugin.throttle || multiData.getCount() <= 1
							 && currentTime > morning.getTimeInMillis() 
							 && currentTime < evening.getTimeInMillis()){
						lastMultiESM = System.currentTimeMillis();
						Intent esm = new Intent();
						String title = "You are multitasking";
//						if( Aware.DEBUG ){
//							Log.d("Multitasking","User is multitasking: prompting the multitasking ESM");
//						}
					    esm.setAction(ESM.ACTION_AWARE_QUEUE_ESM);
						String esmStr = "[" +
				                "{'esm': {" +
				                "'esm_type': 5, " +
				                "'esm_title': '"+title+"', " +
				                "'esm_instructions': 'Is this true?', " +
				                "'esm_quick_answers':  ['Yes','No'], " +
				                "'esm_expiration_threashold': 60, " +
				                "'esm_trigger': 'Multitasking' }}]";
						esm.putExtra(ESM.EXTRA_ESM,esmStr);
						if(Plugin.screenIsOn)
							plugin.sendBroadcast(esm);
					}
					//Reset
					appA = "";
					appB = "";
					appC = "";
					switches = 0;
				}
			} if(multitask != null && ! multitask.isClosed() ) multitask.close();
		}
	}
}
